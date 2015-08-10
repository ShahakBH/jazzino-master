require 'ssh'
require 'thread'
require 'fileutils'
require 'configurer/configurer'
require 'maintenance/web'

class ChefConfigurer < Configurer

  TIMEOUT = 600

  def initialize(config)
    @config = config
  end

  def pre_configure(components, options = {})
    threads = []
    @config.hosts.each do |host|
      if @config.params_for_host(host)['external']
        print "* Chef skipped for external host #{host}\n"

      else
        print "* Running Chef on #{host} (logging to #{@config.log_dir})\n"

        threads << Thread.new do
          Thread.current[:name] = host

          Yazino::SSH.start(host, @config.ssh_options) do |ssh|
            if ssh.exec("sudo gem list | grep chef 2>&1") !~ /chef/
              raise "Please run the Chef bootstrap to install Chef on #{host}"
            end

            FileUtils.rm_f(["#{@config.log_dir}/#{host}-chef-update.log", "#{@config.log_dir}/#{host}-chef-client.log"])

            ssh.exec("sudo gem update chef --no-ri --no-rdoc 2>&1", "#{@config.log_dir}/#{host}-chef-update.log")
            ssh.exec("sudo chef-client -N \\$(hostname -s) 2>&1", "#{@config.log_dir}/#{host}-chef-client.log")

            print "* Chef completed on #{host}\n"
          end
        end
      end
    end

    wait_for_threads("Chef update", TIMEOUT, threads) do |thread|
      host = thread[:name]

      if File.exists?("#{@config.log_dir}/#{host}-chef-update.log")
        print "! Update log file for Chef on host #{host}:\n".foreground(:red)
        File.open("#{@config.log_dir}/#{host}-chef-update.log", 'r') {|f| while line = f.gets; print "#{line}\n" end }
      else
        print ("! Update log file for Chef on host #{host} does not exist\n").foreground(:red)
      end

      if File.exists?("#{@config.log_dir}/#{host}-chef-client.log")
        print "! Client log file for Chef on host #{host}:\n".foreground(:red)
        File.open("#{@config.log_dir}/#{host}-chef-client.log", 'r') {|f| while line = f.gets; print "#{line}\n" end }
      else
        print ("! Client log file for Chef on host #{host} does not exist\n").foreground(:red)
      end
    end

    # Cheffing the haproxy files will reset maintenance
    WebMaintenance.new(@config).on if options[:maintenance]
  end

end
