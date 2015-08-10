require 'rubygems'
require 'bundler/setup'

require 'rainbow'

require 'configurer/chef'
require 'configurer/file'
require 'configurer/log'
require 'maintenance/web'
require 'thread'
require 'threaded_task'

class DeployAction
  include ThreadedTask

  def initialize(config)
    @config = config
  end

  def requires_artefacts?
    true
  end

  def exec(options, components)
    @config.check_for_lock

    configurers = [LogConfigurer.new(@config), FileConfigurer.new(@config)]

    configurers.unshift(ChefConfigurer.new(@config)) unless options.has_key?(:skip_chef)

    methods_to_hosts = components.map_methods_to_hosts
    options[:maintenance_active] = maintenance_required?(options, methods_to_hosts)
    if options[:maintenance_active]
      print "* Maintenance will be required for this release\n"
    else
      print "* Maintenance will not be required for this release\n"
    end

    if options.include?(:live) && options[:maintenance_active]
      raise "Component list contains components requiring maintenance; live deployment cancelled"
    end

    create_latest_deployment_link(options)

    if options.include?(:live) && !options[:maintenance_active]
      perform_live_deployment(methods_to_hosts, components, configurers, options)
    else
      perform_standard_deployment(methods_to_hosts, components, configurers, options, options[:maintenance_active])
    end
  end

  private

  DEPLOY_THREADS = 5
  TIMEOUT = 300

  def perform_live_deployment(methods_to_hosts, components, configurers, options)
    print "* Live deployment commencing\n"

    hosts_to_methods = sort_by_host(methods_to_hosts)
    print "* WARNING: Only one host is present, which makes a live deployment kind of pointless\n".foreground(:red) if hosts_to_methods.size == 1

    hosts_to_methods.each_pair do |host, methods|
      configurers.each { |configurer| configurer.run(:pre_configure, components, options) }
      methods.each { |method| method.run(:pre_deploy, [host], options) }

      configurers.each { |configurer| configurer.run(:configure, components, options) }
      methods.each { |method| method.run(:deploy, [host], options) }

      configurers.each { |configurer| configurer.run(:post_configure, components, options) }
      methods.each { |method| method.run(:post_deploy, [host], options) }
    end
  end

  def perform_standard_deployment(methods_to_hosts, components, configurers, options, maintenance_required)
    print "* Standard deployment commencing\n"

    maintenance = WebMaintenance.new(@config)
    maintenance.on if maintenance_required
    merged_options = options.merge(:maintenance => maintenance_required)

    configurers.each { |configurer| configurer.run(:pre_configure, components, merged_options) }
    execute_method_step(:pre_deploy, methods_to_hosts, merged_options)

    configurers.each { |configurer| configurer.run(:configure, components, merged_options) }
    execute_method_step(:deploy, methods_to_hosts, merged_options)

    configurers.each { |configurer| configurer.run(:post_configure, components, merged_options) }
    execute_method_step(:post_deploy, methods_to_hosts, merged_options)

    if maintenance_required
      if @config.auto_resume?
        maintenance.off
      else
        print "\n! WARNING - maintenance is still active for this environment.\n".foreground(:red)
        print "! You can deactivate it using the --mantenance=off option.\n\n".foreground(:red)
      end
    end
  end

  def sort_by_host(methods_to_hosts)
    hosts_to_methods = {}

    methods_to_hosts.each_key do |method|
      methods_to_hosts[method].each do |host|
        methods_for_host = hosts_to_methods[host] || []
        methods_for_host << method
        hosts_to_methods[host] = methods_for_host
      end
    end

    hosts_to_methods
  end

  def create_latest_deployment_link(options)
    return if options.has_key?(:patch)

    @config.hosts_for_roles(['staging']).each do |host|
      latest_link = @config.latest_staging_dir
      Yazino::SSH.start(host, @config.ssh_options) do |ssh|
        ssh.exec("if [ -h '#{latest_link}' ]; then sudo rm -f #{latest_link}; fi && sudo ln -s #{@config.staging_dir} #{latest_link}")
      end
    end
  end

  def execute_method_step(step, methods_to_hosts, params)
    print "DEBUG: DeployAction: step execution started (#{[step]})\n".foreground(:green) if @config.debug?
    methods = Queue.new

    methods_to_hosts.each_pair do |method, hosts|
      if !method.parallelisable?(step)
        execute_queue(step, methods, params)
        print "DEBUG: DeployAction: Executing non-parallelisable step(#{[method, hosts]})\n".foreground(:green) if @config.debug?
        methods << [method, hosts]
        execute_queue(step, methods, params)
      else
        print "DEBUG: DeployAction: Adding parallelisable step to queue(#{[method, hosts]})\n".foreground(:green) if @config.debug?
        methods << [method, hosts]
      end
    end
    execute_queue(step, methods, params)
    print "DEBUG: DeployAction: step execution complete (#{[step]})\n".foreground(:green) if @config.debug?
  end

  def execute_queue(step, methods_queue, params)
    print "DEBUG: DeployAction: Executing queue(#{methods_queue})\n".foreground(:green) if @config.debug?

    if methods_queue.length == 1
      method, hosts = methods_queue.pop(true)
      print "DEBUG: DeployAction: Directly executing method (#{method}, #{step}, #{hosts}, #{params})\n".foreground(:green) if @config.debug?
      method.run(step, hosts, params)

    elsif !methods_queue.empty?
      threads = []

      [DEPLOY_THREADS, methods_queue.length].min.times do
        threads << Thread.new do
          while (item_method, item_hosts = methods_queue.pop(true) rescue nil)
            print "DEBUG: DeployAction: Thread executing method (#{item_method}, #{step}, #{item_hosts}, #{params})\n".foreground(:green) if @config.debug?
            item_method.run(step, item_hosts, params)
          end
        end
      end

      wait_for_threads("Execution of #{step}", TIMEOUT, threads)

      print "DEBUG: DeployAction: queue purge complete\n".foreground(:green) if @config.debug?
    end
  end

  def maintenance_required?(options, methods_to_hosts)
    return false if options.include?(:skip_maintenance)

    methods_to_hosts.each_key do |method|
      return true if method.maintenance_required?(options)
    end
    false
  end

end
