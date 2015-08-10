require 'ssh'
require 'set'
require 'configurer/configurer'

class LogConfigurer < Configurer

  def initialize(config)
    @config = config
  end

  def configure(components, options = {})
    if options.has_key?(:patch)
      print "* Patching previous release - log files will not be relinked\n"
    else
      relink_log_files(components, options)
    end
  end

  private

  LOG_TIMEOUT = 60
  TIMEOUT = 30

  def map_hosts_to_logs(components)
    hosts_to_logs = {}

    components.values.find_all {|component| !component['log'].nil?}.each do |component|
      component['deploy_to'].each do |role|
        @config.hosts_for_roles([role]).each do |host|
          new_log_files = (component['log']['files'] || [])
          new_log_dirs = (component['log']['directories'] || [])

          log_dirs, log_files = hosts_to_logs[host]
          hosts_to_logs[host] = [(log_dirs || Set.new) + new_log_dirs, (log_files || Set.new) + new_log_files]
        end
      end
    end

    hosts_to_logs
  end

  def relink_log_files(components, options = {})
    hosts_to_logs = map_hosts_to_logs(components)
    threads = []

    hosts_to_logs.each_key do |host|
      threads << Thread.new do
        print "* Relinking log files on #{host}\n"
        log_dirs, log_files = hosts_to_logs[host]

        log_dirs.each do |log_dir|
          if log_dir =~ /([^:]*):([^@]*)@(.*)/
            owner = $1
            group = $2
            clean_log_dir = $3
          else
            owner = 'root'
            group = 'root'
            clean_log_dir = log_dir
          end
          print "DEBUG: LogConfigurer: Processing log directory #{clean_log_dir} with owner #{owner}:#{group} on #{host}\n".foreground(:green) if @config.debug?

          if options.include?(:clean)
            Yazino::SSH.start(host, @config.ssh_options.merge('exit_on_error' => false)) do |ssh|
              print "DEBUG: LogConfigurer: Cleaning old log dirs in  #{clean_log_dir} on #{host}\n".foreground(:green) if @config.debug?
              ssh.exec("sudo find -L #{clean_log_dir} -mindepth 1 -maxdepth 1 -not -newer #{clean_log_dir}/current -not -samefile #{clean_log_dir}/current -type d | sudo xargs rm -rf")
            end
          end

          Yazino::SSH.start(host, @config.ssh_options) do |ssh|
            ssh.exec("if [ ! -d #{clean_log_dir}/#{@config.timestamp} ]; then sudo mkdir -p #{clean_log_dir}/#{@config.timestamp}; fi && sudo chown -R #{owner}:#{group} #{clean_log_dir}/#{@config.timestamp} && if [ -h '#{clean_log_dir}/current' ]; then sudo rm -f #{clean_log_dir}/current; elif [ -d '#{clean_log_dir}/current' ]; then sudo rm -rf #{clean_log_dir}/current; fi && pushd #{clean_log_dir} > /dev/null && sudo ln -sf #{@config.timestamp} current && popd > /dev/null")
          end
        end

        if options.include?(:clean) && !log_files.empty?
          unmanaged_log_files = log_files.find_all do |log_file|
            # Leave any log files that are under one of the managed directories
            managed_log_dirs = log_dirs.map {|dir| /(.@)!?(.*)/.match(dir)[2]}
            managed_log_dirs.reduce(true) {|result, dir| result & (log_file !~ /^#{dir}\//)}
          end

          print "DEBUG: LogConfigurer: Cleaning up log files #{unmanaged_log_files.join(',')} on #{host}\n".foreground(:green) if @config.debug?

          Yazino::SSH.start(host, @config.ssh_options.merge('exit_on_error' => false)) do |ssh|
            ssh.exec("sudo rm -f #{unmanaged_log_files.join(' ')}")
          end
        end
      end
    end

    wait_for_threads("Log configuration", LOG_TIMEOUT, threads)
  end

end
