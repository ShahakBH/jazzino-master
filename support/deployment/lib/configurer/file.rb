require 'ssh'
require 'configurer/configurer'
require 'set'

class FileConfigurer < Configurer

  def initialize(config)
    @config = config
  end

  def configure(components, options = {})
    threads = []

    map_hosts_to_files(components).each_pair do |host, files|
      if !files.empty?
        threads << Thread.new do
          print "* Copying configuration files to #{host}\n"

          Yazino::SSH.start(host, @config.ssh_options) do |ssh|
            deployment_properties_file = "#{@config.latest_staging_dir}/environment.generated.properties"
            ssh.exec("if [ ! -d /etc/senet ]; then sudo mkdir -p /etc/senet; fi; if [ -f /etc/senet/artifact.txt ]; then sudo rm -f /etc/senet/artifact.txt; fi; if [ -f #{@config.latest_staging_dir}/artifact.txt ]; then sudo cp #{@config.latest_staging_dir}/artifact.txt /etc/senet/; fi; if [ -f #{deployment_properties_file} ]; then sudo cp #{deployment_properties_file} /etc/senet/; fi")
          end

          files.each do |source, dest, role|
            if dest =~ /([^:]*):([^@]*)@(.*)/
              owner = $1
              group = $2
              clean_dest = $3
            else
              owner = 'root'
              group = 'root'
              clean_dest = dest
            end

            Yazino::SSH.start(host, @config.ssh_options) do |ssh|
              source_file = "#{@config.latest_staging_dir}/#{role}/#{file_for(source)}"
              ssh.exec("if [ ! -d '#{parent_of(clean_dest)}' ]; then sudo mkdir -p #{parent_of(clean_dest)}; fi && if [ -f #{source_file} ]; then sudo cp #{source_file} #{clean_dest}; if [ -d #{clean_dest} ]; then sudo chown #{owner}:#{group} #{clean_dest}/#{file_for(source)}; else sudo chown #{owner}:#{group} #{clean_dest}; fi; fi")
            end
          end
        end
      end
    end

    wait_for_threads("File configuration", DEPLOY_TIMEOUT, threads)
  end

  private

  DEPLOY_TIMEOUT = 60
  TIMEOUT = 30

  def map_hosts_to_files(components)
    hosts_to_files = {}

    components.each_pair do |component_key, component|
      component['deploy_to'].each do |role|
        @config.hosts_for_roles([role]).each do |host|
          next if @config.params_for_host(host)['external']

          hosts_to_files[host] = hosts_to_files[host] || Set.new

          (component['config_files'] || {}).each_pair {|source, dest| hosts_to_files[host] << [source, dest, role]}
        end
      end
    end

    hosts_to_files
  end

  def file_for(filelist)
    filename = filelist.split(',')[0]
    if filename.match(/.*\/([^\/]+)$/)
      $1
    else
      filename
    end
  end

  def parent_of(filename)
    filename.match(/(.*)\/[^\/]+$/)[1]
  end

end
