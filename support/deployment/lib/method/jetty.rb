require 'method/deployment_method'
require 'method/web_load_balancer'
require 'ssh'

class JettyMethod < DeploymentMethod

  DEPLOYMENT_DELAY = 2

  def initialize(config, artefact, params = {})
    super(config, artefact)

    params = params || {}

    @destination_dir = '/opt/jetty/webapps'

    @config = config
    @artefact = artefact
    @destination_dir = params['destination_dir'] if params.has_key?('destination_dir')
    if params.has_key?('destination_file')
      @destination_file = params['destination_file']
    else
      @destination_file = artefact_file
    end

    @load_balancer = WebLoadBalancer.new(@config)
  end

  def maintenance_required?(options = {})
    !(options.include?(:live) || options.include?(:hot))
  end

  def parallelisable?(step)
    case step
    when :deploy
      true
    else
      false
    end
  end

  def pre_deploy(hosts, options = {})
    print "DEBUG: JettyMethod: Executing pre_deploy(#{hosts}) for #{@artefact}\n".foreground(:green) if @config.debug?

    threads = []

    hosts.each do |host|
      threads << Thread.new do
        if options.has_key?(:force_deploy) || remote_files_differ(host, staged_path, destination_path)
          @load_balancer.remove(host) if options.include?(:live)

          if !options.include?(:hot)
            print "* Cleaning up Jetty on #{host}: #{@destination_file}\n"
            ssh_command = "sudo rm -rf /opt/jetty/work/jetty-*#{@destination_file}*"
            if stop_service_on(host, "jetty")
              ssh_command = "sleep 5 && " + ssh_command
            end
            Yazino::SSH.start(host, @config.ssh_options.merge('exit_on_error' => false)) {|ssh| ssh.exec(ssh_command) }
          end
        end
      end
    end

    wait_for_threads("Jetty pre-deployment", TIMEOUT, threads)
  end

  def deploy(hosts, options = {})
    print "DEBUG: JettyMethod: Executing deploy(#{hosts}) for #{@artefact}\n".foreground(:green) if @config.debug?
    print "DEBUG: JettyMethod: Deployment is forced for #{@artefact}\n".foreground(:green) if options.has_key?(:force_deploy) && @config.debug?
    print "DEBUG: JettyMethod: Deployment is hot for #{@artefact}\n".foreground(:green) if options.include?(:hot) && @config.debug?

    threads = []

    hosts.each do |host|
      threads << Thread.new do
        if options.has_key?(:force_deploy) || remote_files_differ(host, staged_path, destination_path)
          Yazino::SSH.start(host, @config.ssh_options) do |ssh|
            if options.include?(:hot)
              print "* Hot-deploying to Jetty on #{host}: #{staged_path}\n"
              ssh.exec "sudo cp #{staged_path} #{destination_path}"

            else
              print "* Deploying to Jetty on #{host}: #{staged_path}\n"
              ssh.exec "sudo rm -rf #{destination_path} && sleep #{DEPLOYMENT_DELAY} && sudo cp #{staged_path} #{destination_path}"
            end
          end

        else
          print "* Skipping on #{host} as not changed from deployed version: #{staged_path}\n"
        end
      end
    end

    wait_for_threads("Jetty deployment", TIMEOUT, threads)
  end

  def post_deploy(hosts, options = {})
    print "DEBUG: JettyMethod: Executing post_deploy(#{hosts}) for #{@artefact}\n".foreground(:green) if @config.debug?

    threads = []

    if !options.include?(:hot)
      hosts.each do |host|
        threads << Thread.new do
          start_service_on(host, "jetty")
          @load_balancer.wait_until_added(host) if options.include?(:live)
        end
      end
    end

    wait_for_threads("Jetty post-deployment", TIMEOUT, threads)
  end

  def eql?(object)
    return false if !super.eql?(object)

    return object.destination_dir == @destination_dir
  end

  def hash
    [super.hash, @destination_dir].hash
  end

  private

  TIMEOUT = 180

  def destination_path
    "#{@destination_dir}/#{@destination_file}"
  end

end
