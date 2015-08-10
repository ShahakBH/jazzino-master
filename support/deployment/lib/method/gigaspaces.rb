require 'method/deployment_method'
require 'ssh'
require 'set'

class GigaspacesMethod < DeploymentMethod

  def initialize(config, artefact, params = {})
    super(config, artefact)

    params = params || {}

    @kill_attempts = 5
    @wait_time = 3
    @gs_base = '/opt/gigaspaces'
    @osgi_caches = ["#{@gs_base}/osgi-cache", "#{@gs_base}/bin/felix-cache"]
    @sla = nil
    @application = nil

    @config = config
    @artefact = artefact
    @kill_attempts = params['kill_attempts'] if params.has_key?('kill_attempts')
    @wait_time = params['wait_time'] if params.has_key?('wait_time')
    @gs_base = params['gs_base'] if params.has_key?('gs_base')
    @sla = params['sla'] if params.has_key?('sla')
    @application = params['application'] if params.has_key?('application')

    # Kill redeployment, as it doesn't offer any speedups at present
    @force_all_deploys = true

    raise "GigaSpaces artefact must have either a SLA or Application XML specified" if @sla.nil? && @application.nil?
  end

  def maintenance_required?(options = {})
    if @force_all_deploys || options.include?(:force_deploy)
      true
    else
      gsm_host = @config.hosts_for_roles(['gigaspaces']).first
      Yazino::SSH.start(gsm_host, @config.ssh_options) do |ssh|
        is_deployed, is_changed = artefact_status(ssh)
      end
      !is_deployed || is_changed
    end
  end

  def pre_deploy(hosts, options = {})
    print "DEBUG: GigaspacesMethod: Executing pre_deploy(#{@config.hosts_for_roles(['gigaspaces'])}) of #{@artefact}\n".foreground(:green) if @config.debug?

    shutdown_grid if @force_all_deploys || options.include?(:force_deploy)
  end

  def deploy(hosts, options = {})
    print "DEBUG: GigaspacesMethod: Executing deploy(#{@config.hosts_for_roles(['gigaspaces'])}) of #{@artefact}\n".foreground(:green) if @config.debug?

    ensure_grid_is_running(options)
    options = options.merge({:force_deploy => true}) if @force_all_deploys
    deploy_processor(options)
  end

  private

  TIMEOUT=120
  STARTUP_WAIT_TIME = 5
  POST_DEPLOY_SLEEP = 2

  def deploy_processor(options)
    gsm_host = @config.hosts_for_roles(['gigaspaces']).first

    print "DEBUG: GigaspacesMethod: Deploying #{@artefact} to #{gsm_host}\n".foreground(:green) if @config.debug?

    Yazino::SSH.start(gsm_host, @config.ssh_options) do |ssh|
      deploy_app = options.include?(:force_deploy)
      if !deploy_app
        is_deployed, is_changed = artefact_status(ssh)
        deploy_app = !is_deployed || is_changed
      end

      is_application = !@application.nil?

      if deploy_app
        if is_application
          # If you're deploying an application then you can't specify an override to the application.xml, hence you must replace it.
          ssh.exec("if [ -d #{application_dir} ]; then sudo rm -rf #{application_dir}; fi && sudo unzip -d #{application_dir} #{staged_path} && sudo cp #{@application} #{application_dir}/")
        end

        # If you try to deploy a deployed processing unit, it appears to be ignored (i.e. NOT redeployed)
        if !options.include?(:force_deploy) && is_deployed
          print "* Undeploying #{@artefact}\n"
          if is_application
            output = ssh.exec("sudo su - gsrun -c '#{@gs_base}/bin/gs.sh undeploy-application -initialization-timeout 30000 #{artefact_name}' 2>&1 && if [ -f #{hash_file} ]; then sudo rm #{hash_file}; fi")
          else
            output = ssh.exec("sudo su - gsrun -c '#{@gs_base}/bin/gs.sh undeploy -initialization-timeout 30000 #{artefact_name}' 2>&1 && if [ -f #{hash_file} ]; then sudo rm #{hash_file}; fi")
          end
          print "gs.sh undeployment output: #{output}\n".foreground(:green) if @config.debug?
          raise "Undeployment failed, check GS logs: output was: #{output}" if output =~ /[Ff]ailed/ && output !~ /no active deployment/

          # TODO should poll until undeploy completed?
        end

        clean_osgi_cache

        print "* Deploying #{@artefact}\n"
        if is_application
          # We provide a way to package shared libs, since GS doesn't
          ssh.exec("if [ -d #{application_dir}/lib ]; then sudo cp #{application_dir}/lib/* #{@gs_base}/lib/optional/pu-common/; fi")
          output = ssh.exec("sudo su - gsrun -c '#{@gs_base}/bin/gs.sh deploy-application -initialization-timeout 60000 #{application_dir}' 2>&1 && sudo bash -c 'sha512sum #{staged_path} | cut -f 1 -d \\\" \\\" > #{hash_file}'")
        else
          output = ssh.exec("sudo su - gsrun -c '#{@gs_base}/bin/gs.sh deploy -initialization-timeout 60000 -sla #{@sla} #{staged_path}' 2>&1 && sudo bash -c 'sha512sum #{staged_path} | cut -f 1 -d \\\" \\\" > #{hash_file}'")
        end
        print "gs.sh deployment output: #{output}\n".foreground(:green) if @config.debug?
        raise "Deployment failed, check GS logs: output was: #{output}" if output !~ /deployed successfully/ || output =~ /[Ff]ailed/

        sleep(POST_DEPLOY_SLEEP)
      else
        print "* Skipping as not changed from deployed version: #{@artefact}\n"
      end
    end

    sleep(@wait_time)
  end

  def clean_osgi_cache
    threads = []

    @config.hosts_for_roles(['gigaspaces']).each do |host|
      threads << Thread.new do
        Yazino::SSH.start(host, @config.ssh_options) do |ssh|
          ssh_commands = []
          @osgi_caches.each do |cache_dir|
            ssh_commands << "if [ -d #{cache_dir} ]; then for dir in \\$(ls #{cache_dir}); do sudo rm -rf #{cache_dir}/\\$dir; done; fi"
          end
          ssh.exec(ssh_commands.join(" && "))
        end
      end
    end

    wait_for_threads("OSGi cache cleanup", TIMEOUT, threads)
  end

  def application_dir
    "#{@config.latest_staging_dir}/#{artefact_name}"
  end

  def artefact_name
    @artefact.split(':')[1]
  end

  def hash_file
    "/etc/senet/gs-#{artefact_name}.hash"
  end

  def artefact_status(ssh)
    if @application.nil?
      # TODO a faster way to determine the deployment status would be wizard (list is slow as shite)
      is_deployed = ssh.exec("sudo su - gsrun -c '#{@gs_base}/bin/gs.sh list gsc' 2>&1") =~ /#{artefact_name}/
    else
      # gs.sh list doesn't work with applications
      processing_units = ssh.exec("cat /etc/senet/application.xml  | grep 'processing-unit' | gawk 'match(\\$0, /processing-unit=\\\\\\\"([^\\\\\\\"]*).jar\\\\\\\"/, a) {print a[1]}'")
      print "DEBUG: GigaspacesMethod: Checking deployment status of #{@artefact} using processing units #{processing_units}\n".foreground(:green) if @config.debug?
      is_deployed = ssh.exec("sudo su - gsrun -c '#{@gs_base}/bin/gs.sh list gsc' 2>&1") =~ /#{processing_units.split(/$/)[0].strip}/
    end

    is_changed = true
    if is_deployed
      is_changed = ssh.exec("sudo su - gsrun -c 'if [ -e #{hash_file} -a \\\$(sha512sum #{staged_path} | cut -f 1 -d \\\" \\\") == \\\$(cat #{hash_file}) ]; then echo \\\"same-artefact\\\"; fi'") !~ /same-artefact/
      print "DEBUG: GigaspacesMethod: Artefact  #{@artefact} is deployed. Changed? #{is_changed}\n".foreground(:green) if @config.debug?
    else
      print "DEBUG: GigaspacesMethod: Artefact  #{@artefact} is not deployed.\n".foreground(:green) if @config.debug?
    end
    [is_deployed, is_changed]
  end

  def shutdown_grid
    threads = []

    @config.hosts_for_roles(['gigaspaces']).each do |host|
      threads << Thread.new do
        Yazino::SSH.start(host, @config.ssh_options) do |ssh|
          print "DEBUG: GigaspacesMethod: ensuring GigaSpaces is running on #{host}\n".foreground(:green) if @config.debug?
          stop_service_on(host, "gs-agent", 10, 12)
        end
      end
    end

    wait_for_threads("GigaSpaces shutdown", TIMEOUT, threads)
  end

  def ensure_grid_is_running(options)
    threads = []

    @config.hosts_for_roles(['gigaspaces']).each do |host|
      threads << Thread.new do
        Yazino::SSH.start(host, @config.ssh_options) do |ssh|
          print "DEBUG: GigaspacesMethod: ensuring GigaSpaces is running on #{host}\n".foreground(:green) if @config.debug?
          start_service_on(host, "gs-agent")
        end
      end
    end

    wait_for_threads("GigaSpaces startup", TIMEOUT, threads)

    sleep(STARTUP_WAIT_TIME)
  end

end
