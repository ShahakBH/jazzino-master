#!/usr/bin/env ruby

require 'rubygems'
require 'bundler/setup'

require 'fileutils'
require 'etc'
require 'socket'
require 'pty'
require 'uri'
require 'yaml'

require 'rexml/document'
require 'rexml/xpath'
require 'aws-sdk'
require 'net/http'

require 'notifier'
require 'git'
require 'config'

include REXML
include Net

class Releaser

  PRODUCT_PREFIX = 'strata'

  def initialize(branch, build_number, override_config)
    @build_number = build_number
    @override_config = override_config
    @config = Yazino::Config.new(override_config)
    @branch = branch

    begin
      @previous_branch, @previous_hash, @previous_build_number = get_latest_artefact(@branch)
      puts "Last release was #{@previous_build_number} on #{@previous_branch} @ #{@previous_hash}"
    rescue => e
      puts "ERROR: #{e}" if @config.debug?
      puts e.backtrace.join("\n") if @config.debug?
      puts "No previous release found"
      @previous_branch = @previous_hash = nil
    end
  end

  def release
    puts "Releasing #{@branch} for build #{@build_number}"

    create_branch = @branch == 'master'

    work_dir = work_dir("release.git", :create => false, :reuse => true)
    git = Git.new(@config[Yazino::Config::GIT_REPO], work_dir)

    build_branch, @build_number = prepare_branch(git, @branch, @build_number, create_branch)

    puts "Release number will be #{@build_number}"

    files_to_upload, release_info = package_build(git, build_branch, @build_number)
    upload_files(@config[Yazino::Config::RELEASE_SERVER], @config[Yazino::Config::RELEASE_USER], \
      @config[Yazino::Config::RELEASE_PASSWORD], @config[Yazino::Config::RELEASE_LOCATION], files_to_upload)

    notifier = Notifier.new(@override_config)
    notifier.send(@config[Yazino::Config::NOTIFICATION_TO], @config[Yazino::Config::NOTIFICATION_FROM], release_info)

    puts "Release complete"
  end

  private

  def branch_name_for(build_number)
    "#{PRODUCT_PREFIX}-#{build_number}"
  end

  def get_latest_artefact(branch_name)
    s3 = AWS::S3.new(:access_key_id => @config.s3['access-key'], :secret_access_key => @config.s3['secret-key'])
    release_bucket = s3.buckets['yazino-releases']
    available_files = release_bucket.objects.collect {|obj| obj.key}

    if branch_name =~ /strata-release-\d+(\.\d+)?/
      artefact = available_files.keep_if {|file| file =~ /^#{branch_name}(\.\d+)?\.txt$/}.sort {|a, b| b <=> a}.first.gsub(/\.txt$/, '')
    else
      artefact = available_files.keep_if {|file| file =~ /^strata-release-\d+\.txt$/}.sort {|a, b| b <=> a}.first.gsub(/\.txt$/, '')
    end
    raise "Error: Cannot find definition for artefact #{artefact}" if !available_files.include?("#{artefact}.txt")

    artefact_details =  release_bucket.objects["#{artefact}.txt"].read.strip
    raise "Error: Cannot read definition for artefact #{artefact}" if artefact_details.nil?

    branch = nil
    git_hash = nil
    if artefact_details =~ /^VCS: [^ ]+ ([^ ]+) ([^ ]+)$/
      branch = $1
      git_hash = $2
    else
      raise "Error: malformed artefact information: #{artefact_details}"
    end

    [branch, git_hash, artefact]
  end

  def upload_files(host, username, password, location, files)
    puts "Uploading release to repository"

    s3 = AWS::S3.new(:access_key_id => @config.s3['access-key'], :secret_access_key => @config.s3['secret-key'])
    upload(s3.buckets['yazino-releases'], files)
  end

  def upload(release_bucket, files, prefix = nil)
    files.each do |file|
      filename = File.basename(file)
      if File.directory?(file)
        upload(release_bucket, Dir.entries(file).collect {|entry| "#{file}/#{entry}"}, "#{prefix}#{filename}/")
      else
        release_bucket.objects["#{prefix}#{filename}"].write(Pathname.new(file))
      end
    end
  end

  def package_build(git, branch, build_number)
    work_dir = work_dir("#{PRODUCT_PREFIX}-#{build_number}", :create => true)
    artefacts = get_artefacts

    puts "Building release atom for #{branch}"

    artefacts.each do |artefact|
      puts "* Checking validity of #{artefact[:group]}:#{artefact[:artefact]}:#{artefact[:version]}"
      next if artefact[:type] == 'noartefact'

      raise "Artefact #{artefact[:artefact]} has an invalid or snapshot version: #{artefact[:version]}, exiting." if artefact[:version].nil? || artefact[:version] =~ /-SNAPSHOT$/

      artefact_url = "#{@config[Yazino::Config::MAVEN_REPO]}/#{artefact[:group].gsub('.', '/')}/#{artefact[:artefact]}/#{artefact[:version]}/#{artefact[:artefact]}-#{artefact[:version]}.#{artefact[:type]}"
      raise "Artefact #{artefact[:artefact]}:#{artefact[:version]} can't be retrieved from the Maven repo: #{artefact_url}" if !http_file_exists(artefact_url)
    end

    puts "Created artefact #{File.basename(work_dir)}"

    release_info = gather_release_info(git, branch, build_number, artefacts)
    files_to_upload = [create_release_file(work_dir, release_info), \
        create_release_definition(work_dir, release_info)]
    [files_to_upload, release_info]
  end

  def get_artefacts
    release_components = File.expand_path("#{@config.base_dir}/#{@config[Yazino::Config::RELEASE_COMPONENTS]}")
    raise "Invalid components.definition: #{release_components}" if release_components.nil? || !File.exists?(release_components)

    components = File.open(release_components) { |yf| YAML::load(yf) }

    artefacts = []
    components.each_key do |artefact_id|
      group = components[artefact_id]['maven_group'] || 'com.yazino'
      artefacts << {:group => group, \
        :artefact => artefact_id, \
        :version => components[artefact_id]['version'], \
        :type => components[artefact_id]['type'], \
        :jira_group => components[artefact_id]['jira_group']}
    end
    artefacts
  end

  def create_release_definition(work_dir, release_info)
    release_def_file = "#{work_dir}/strata-#{release_info[:build_number]}.release"

    release_def = {}
    release_def['build_number'] = release_info[:build_number]
    release_def['timestamp'] = release_info[:timestamp]
    release_def['versions'] = {}
    release_info[:artefacts].each do |artefact|
      release_def['versions'][artefact[:artefact]] = artefact[:version] if artefact[:version]
    end

    File.open(release_def_file, 'w') {|file| YAML.dump(release_def, file)}
    release_def_file
  end

  def create_release_file(work_dir, release_info)
    release_file = "#{work_dir}/#{PRODUCT_PREFIX}-#{release_info[:build_number]}.txt"
    File.open(release_file, 'w') do |file|
      file.write("Build: #{release_info[:build_number]}\n")
      file.write("VCS: #{release_info[:git_url]} #{release_info[:git_branch]} #{release_info[:git_last_commit]}\n")
      file.write("Date: #{release_info[:timestamp]}\n")
      file.write("Host: #{release_info[:hostname]}\n")
      file.write("User: #{release_info[:username]}\n")

      if release_info[:artefacts] && !release_info[:artefacts].empty?
        file.write("\nThis release includes:\n")
        release_info[:artefacts].each do |artefact|
          file.write("* #{artefact[:group]}:#{artefact[:artefact]}")
          file.write(" @ #{artefact[:version]}") if artefact[:version] && !artefact[:version].empty?
          file.write("\n")
        end
      end

      if release_info[:git_history] && !release_info[:git_history].empty?
        file.write("\nChanges to support since last release:\n")
        if !@previous_hash.nil?
          file.write("\n> strata.git (last was #{release_info[:git_last_branch]} #{release_info[:git_last_hash]}):\n")
          file.write(release_info[:git_history])
        end
      end
    end
    release_file
  end

  def gather_release_info(git, branch, build_number, included_releases = [])
    git_remote_url, git_last_commit = git.info
    if !@previous_hash.nil?
      git_history = git.log(@previous_hash, git_last_commit)
      last_branch = @previous_branch
      last_hash = @previous_hash
    else
      git_history = last_branch = last_hash = nil
    end

    {:build_number => build_number, \
      :previous_build_number => @previous_build_number, \
      :git_url => git_remote_url, \
      :git_branch => branch, \
      :git_last_commit => git_last_commit, \
      :git_history => git_history, \
      :git_last_branch => last_branch, \
      :git_last_hash => last_hash, \
      :timestamp => Time.now.strftime('%Y/%m/%d %H:%M:%S %Z'), \
      :hostname => Socket.gethostname, \
      :username => Etc.getlogin, \
      :artefacts => included_releases}
  end

  def prepare_branch(git, branch, build_number, create_branch)
    if git.exists?
      git.remove_working_copy
    end

    git.clone

    git.checkout(branch)
    git.pull(branch)

    if create_branch
      build_branch = branch_name_for(build_number)
      puts "Creating branch #{build_branch}"

      git.branch(build_branch)
      git.push(build_branch) unless @config.debug?

      [build_branch, build_number]
    else
      build_number = update_build_number_for_branch(git, branch, build_number)

      [branch, build_number]
    end
  end

  def update_build_number_for_branch(git, branch, build_number)
    suffix_file = "#{git.repository}/build-suffix"
    puts suffix_file

    build_suffix = 1
    if File.exists?(suffix_file)
      File.open(suffix_file, 'r') {|f| build_suffix = f.gets.strip.to_i}
      build_suffix += 1
    end

    File.open(suffix_file, 'w') {|f| f.puts(build_suffix)}

    previous_build_number_match = /#{PRODUCT_PREFIX}-(.*)/.match(branch)
    if (previous_build_number_match.nil?)
      build_number = "#{build_number}.#{build_suffix}"
    else
      build_number = "#{previous_build_number_match[1]}.#{build_suffix}"
    end

    git.add("build-suffix")
    git.commit("Incremented build-suffix for #{build_number}")
    git.push(branch) unless @config.debug?

    build_number
  end

  def work_dir(task, params = [])
    work_dir = "#{tmpdir()}/#{task.downcase}"

    if File.exists?(work_dir) && !params[:reuse]
      puts "Removing stale work directory"
      FileUtils.rm_rf(work_dir)
    end

    FileUtils.mkdir_p work_dir if params[:create]

    puts "Work directory for #{task} is #{work_dir}"

    work_dir
  end

  def tmpdir()
    if !ENV['RELEASE_TMPDIR'].nil?
      filter_dir ENV['RELEASE_TMPDIR']
    elsif !ENV['TMPDIR'].nil?
      filter_dir ENV['TMPDIR']
    else
      "/tmp"
    end
  end

  def filter_dir(dirname)
    dirname =~ /\/$/ ? dirname[0..dirname.length - 2] : dirname
  end

  def http_file_exists(uri_to_check)
    uri = URI(uri_to_check)

    HTTP.start(uri.host, uri.port) do |http|
      request = HTTP::Head.new(uri.request_uri)

      begin
        http.request request do |response|
          case response
            when HTTPSuccess then
              true
            else
              false
          end
        end
      rescue Exception => e
        raise "Failed to fetch #{uri_to_check}: #{e}"
      end
    end
  end

end

if __FILE__ == $0
  def parse_arguments(args)
    raise "Usage: #{__FILE__} <branch name> <build number> [--config=file.yaml]" if args.length < 2

    source_branch = args[0]
    build_number = args[1]

    source_branch = source_branch.gsub(/refs\/heads\//, '')

    override_config = nil
    (2..args.length - 1).each do |i|
      override_config = $1 if args[i] =~ /--config=(.*)/
    end

    [source_branch, build_number, override_config]
  end

  begin
    source_branch, build_number, override_config = parse_arguments(ARGV)

    Releaser.new(source_branch, build_number, override_config).release

  rescue RuntimeError => e
    puts "Error: #{e}"
    exit 1
  end
end
