require 'rexml/document'
require 'rexml/xpath'
require 'net/http'
require 'fileutils'
require 'date'

include REXML
include Net

# Thread safety patch for REXML, from https://github.com/rubiii/savon/issues/105
module REXML::Encoding
  @mutex = Mutex.new
  def self.apply(obj, enc)
    @mutex.synchronize { @encoding_methods[enc][obj] }
  end
end

class MavenSource

  def initialize(config, params = {})
    @remote_repository = DEFAULT_ARTIFACTORY_HOST
    @check_for_newer_snapshots = false
    @config = config

    @remote_repository = config.maven['repository'] if config.maven.has_key?('repository')

    @check_for_newer_snapshots = params[:check_for_newer_snapshots] if params.has_key?(:check_for_newer_snapshots)
  end

  def describe(group, artefact, version)
    source_file = get_local_artefact_path(group, artefact, 'pom', version)
    if File.exists?(source_file)
      pom_file = source_file
    else
      delete_pom_file = true
      get_latest_artefact(tmpdir, group, artefact, 'pom', version)
      pom_file = "#{tmpdir}/#{artefact}-#{version}.pom"
    end

    pom_doc = REXML::Document.new(File.new(pom_file))
    name = XPath.first(pom_doc, '/project/name').text if !XPath.first(pom_doc, '/project/name').nil?
    description = XPath.first(pom_doc, '/project/description').text if !XPath.first(pom_doc, '/project/description').nil?

    File.delete(pom_file) if delete_pom_file

    {:name => name, :description => description}
  end

  def fetch(destination, group, artefact, type, version)
    if !File.directory?(destination)
      raise "Destination is not a directory: #{destination}"
    end

    if @check_for_newer_snapshots
      get_local_snapshot_if_available(destination, group, artefact, type, version)
    else
      print "DEBUG: MavenSource: Getting latest artefact #{group}:#{artefact}:#{type}:#{version}\n".foreground(:green) if @config.debug?
      get_latest_artefact(destination, group, artefact, type, version)
    end
  end

  private

  DEFAULT_ARTIFACTORY_HOST = 'http://artifactory.london.yazino.com/artifactory/repo'

  def tmpdir
    if ENV['TMPDIR']
      ENV['TMPDIR']
    else
      '/tmp'
    end
  end

  def get_local_artefact(destination, group, artefact, type, version)
    print "DEBUG: MavenSource: Getting local artefact #{get_local_artefact_path(group, artefact, type, version)}\n".foreground(:green) if @config.debug?
    if version =~ /-SNAPSHOT/
      destination_file = "#{destination}/#{artefact}.#{type}"
    else
      destination_file = "#{destination}/#{artefact}-#{version}.#{type}"
    end

    source_file = get_local_artefact_path(group, artefact, type, version)
    if File.exists?(source_file)
      FileUtils.cp(source_file, destination_file)

    elsif version !~ /-SNAPSHOT/
      print "DEBUG: MavenSource: No local artefact found for release artefact (#{source_file}), checking remote\n".foreground(:green) if @config.debug?
      get_file_by_http(destination_file, "#{@remote_repository}/#{group.gsub('.', '/')}/#{artefact}/#{version}/#{artefact}-#{version}.#{type}")

    else
      raise "Couldn't find local artefact #{group}:#{artefact}:#{type}:#{version} - do you have a local copy?"
    end
  end

  def get_local_snapshot_if_available(destination, group, artefact, type, current_version)
    if current_version =~ /(\d+)\.(\d+)\.(\d+).*/
      version = {:major => $1.to_i, :minor => $2.to_i, :patch => $3.to_i, :version => current_version}

      get_local_artefact_snapshot_versions(group, artefact).each do |snapshot_version|
        version = snapshot_version if snapshot_version[:major] > version[:major] \
          || (snapshot_version[:major] == version[:major] && snapshot_version[:minor] > version[:minor]) \
          || (snapshot_version[:major] == version[:major] && snapshot_version[:minor] == version[:minor] && snapshot_version[:patch] > version[:patch])
      end

      if version[:version] != current_version
        print "* Development version #{version[:version]} of #{group}:#{artefact} will be used\n".foreground(:yellow)
        get_local_artefact(destination, group, artefact, type, version[:version])
      else
        get_latest_artefact(destination, group, artefact, type, current_version)
      end

    else
      print "! Cannot parse #{current_version} as n.n.n; using current version"
    end
  end

  def get_local_artefact_snapshot_versions(group, artefact)
    base_dir = "#{get_repo_path}/#{group.gsub(/\./, '/')}/#{artefact}"
    if File.exists?(base_dir)
      Dir.entries(base_dir) \
        .find_all {|entry| File.directory?("#{base_dir}/#{entry}") && entry.strip =~ /\d+\.\d+\.\d+.*-SNAPSHOT$/} \
        .map {|snapshot| if snapshot =~ /(\d+)\.(\d+)\.(\d+).*-SNAPSHOT/ then {:major => $1.to_i, :minor => $2.to_i, :patch => $3.to_i, :version => snapshot} else {} end}
    else
      []
    end
  end

  def get_local_artefact_path(group, artefact, type, version)
    "#{get_repo_path}/#{group.gsub(/\./, '/')}/#{artefact}/#{version}/#{artefact}-#{version}.#{type}"
  end

  def get_latest_artefact(destination, group, artefact, type, version)
    raise "No remote repository is configured" if @remote_repository.nil?

    remote_base_url = "#{@remote_repository}/#{group}/#{artefact}/#{version}"

    if version =~ /.*-SNAPSHOT/
      get_artefact_by_http("#{remote_base_url}/maven-metadata.xml", destination, group, artefact, type, version)
    else
      get_local_artefact(destination, group, artefact, type, version)
    end
  end

  def get_artefact_by_http(url, destination, group, artefact, type, version, limit = 10)
    raise 'HTTP redirection limit exceeded' if limit == 0

    metadata_resp = HTTP.get_response(URI.parse(url))
    case metadata_resp
    when HTTPRedirection then
      get_artefact_by_http(metadata_resp['location'], destination, group, artefact, type, version, limit - 1)

    when HTTPSuccess then
      metadata_xml = metadata_resp.body
      metadata_doc = REXML::Document.new(metadata_xml)

      artefact_timestamp = XPath.first(metadata_doc, "/metadata/versioning/snapshot/timestamp")
      artefact_buildnumber = XPath.first(metadata_doc, "/metadata/versioning/snapshot/buildNumber")

      local_artefact = get_local_artefact_path(group, artefact, type, version)
      if File.exists?(local_artefact)
        local_timestamp = File.new(local_artefact).mtime
        remote_timestamp = if artefact_timestamp.nil? then nil else DateTime.strptime(artefact_timestamp.text, '%Y%m%d.%H%M%S').to_time end

        if remote_timestamp.nil? || local_timestamp > remote_timestamp
          return get_local_artefact(destination, group, artefact, type, version)
        end
      end

      get_remote_artefact(destination, group, artefact, type, version, artefact_timestamp, artefact_buildnumber)

    when HTTPNotFound then
      get_local_artefact(destination, group, artefact, type, version)

    else
      raise "Couldn't fetch artefact from: #{remote_base_url}; response was #{metadata_resp}"
    end
  end

  def get_remote_artefact(destination, group, artefact, type, version, artefact_timestamp, artefact_buildnumber)
    artefact_file = "#{artefact}-#{version}.#{type}"
    if !artefact_timestamp.nil?
      timestamp_version = /(.*)-SNAPSHOT/.match(version)[1]
      artefact_file = "#{artefact}-#{timestamp_version}-#{artefact_timestamp.text}-#{artefact_buildnumber.text}.#{type}"
    end

    print "DEBUG: MavenSource: Getting remote artefact from #{@remote_repository}/#{group.gsub('.', '/')}/#{artefact}/#{version}/#{artefact_file}\n".foreground(:green) if @config.debug?
    if version =~ /-SNAPSHOT/
      get_file_by_http("#{destination}/#{artefact}.#{type}", "#{@remote_repository}/#{group.gsub('.', '/')}/#{artefact}/#{version}/#{artefact_file}")
    else
      get_file_by_http("#{destination}/#{artefact}-#{version}.#{type}", "#{@remote_repository}/#{group.gsub('.', '/')}/#{artefact}/#{version}/#{artefact_file}")
    end
  end

  def get_file_by_http(destination, uri_to_fetch)
    uri = URI(uri_to_fetch)

    HTTP.start(uri.host, uri.port) do |http|
      request = HTTP::Get.new(uri.request_uri)

      begin
        http.request request do |response|
          case response
          when HTTPSuccess then
            open destination, 'w' do |io|
              response.read_body do |chunk|
                io.write chunk
              end
            end
          else
            raise "Couldn't fetch file #{uri_to_fetch} to #{destination}: code #{response.code}"
          end
        end
      rescue Exception => e
        raise "Failed to fetch #{uri_to_fetch} to #{destination}: #{e}"
      end
    end
  end

  def get_repo_path
    home = ENV['HOME']
    if home.nil? || !File.directory?(home)
      raise "HOME is not set or is not a directory: #{home}"
    end

    if File.exists?("#{home}/.m2/settings.xml")
      settings_document = Document.new(File.new("#{home}/.m2/settings.xml"))
      local_repo = XPath.first(settings_document, '/settings/localRepository')
      if !local_repo.nil? && local_repo.length > 0
        return local_repo
      end
    end

    "#{home}/.m2/repository"
  end

end
