require 'rubygems'
require 'bundler/setup'
require 'yaml'
require 'open-uri'

require 'rainbow'
require 'aws-sdk'

require 'ping'
require 'config'
require 'components'
require 'ssh'
require 'vpn'
require 'action/deploy'
require 'action/stage'
require 'action/dumplogs'
require 'action/maintenance'
require 'action/rollback'
require 'action/what'

module Yazino
  class Deploy

    def initialize(deployment_list, options = {})
      @deployment_list = deployment_list
      @options = options
      @action, @target = get_action_and_target(options)
    end

    def run(config)
      create_deployment_lock(config)
      config.clean_working_dir

      if @target == 'latest-release'
        @target = get_last_release_name(config)
      elsif @target == 'latest'
        print "! 'latest' is a silly release name and would fubar the symlinks. Did you mean 'current'? Bailing.\n".foreground(:red)
        delete_deployment_lock(config)
        exit(1)
      end
      components = get_components(config)

      puts "-------------------------------------------------------------------".bright
      puts " #{describe_action(@action)} to #{config.environment} @ #{config.deploy_time}".bright
      puts "-------------------------------------------------------------------".bright

      if RUBY_VERSION =~ /^1.8/
        puts "\n! ERROR: This script does not run with Ruby 1.8.x. Please update to 2.0.0 or above.".foreground(:red)
        exit 1
      end

      print "#{describe_action(@action)}:\n"
      print "! You are running a dev deployment - the versions below may be altered if you have local snapshots\n".foreground(:yellow) if @target == 'dev'
      components.each_pair do |component_name, component|
        version = component['version'] if !component['version'].nil?
        print "- #{component_name} "
        print "@ #{version}".foreground(:green) if version
        print "\n"
      end
      print "\n"

      connect_to_vpn(config) if config.vpn_required?

      begin
        prepare_config(config)

        verify_hosts_exist(config)

        actions = []
        actions << StageAction.new(config, @target, @options) if action_in(:stage, :deploy)
        actions << DeployAction.new(config) if action_in(:redeploy, :deploy)
        actions << RollbackAction.new(config) if action_in(:rollback)
        actions << DumpLogsAction.new(config, @target) if action_in(:dumplogs, :deploy, :redeploy)
        actions << MaintenanceAction.new(config, @options) if action_in(:maintenance)
        actions << WhatAction.new(config) if action_in(:what)

        actions.each { |action| raise "No artefacts specified" if action.requires_artefacts? && components.empty? }
        actions.each do |action|
          start_time = Time.now
          action.exec(@options, components)
          puts "t Action #{action} took #{Time.now - start_time}".foreground(:cyan) if @options[:timing]
        end

        config.clean_working_dir

      ensure
        disconnect_from_vpn if config.vpn_required?
        config.unlock
        delete_deployment_lock(config)
      end

      elapsed_time = Time.now - config.deploy_time
      puts "------------------------------------------------------------------".bright
      puts " #{describe_action(@action)} complete @ #{Time.now}".bright
      puts " Elapsed %02i:%02i".bright % [elapsed_time.to_i / 60, elapsed_time.to_i % 60]
      puts "------------------------------------------------------------------".bright
    end

    private

    LOCK_FILE = "/var/run/yazino-deployment.lock"

    def create_deployment_lock(config)
      config.hosts_for_roles(['staging']).each do |staging_host|
        Yazino::SSH.start(staging_host, config.ssh_options) do |ssh|
          if ssh.exec("if [ -f #{LOCK_FILE} ]; then echo LOCKED; else sudo touch #{LOCK_FILE}; fi") =~ /LOCKED/
            raise "Another deployment is running on this environment (or something has gone horribly wrong). ['#{LOCK_FILE}' present on #{staging_host}]"
          end
        end
      end
    end

    def delete_deployment_lock(config)
      config.hosts_for_roles(['staging']).each do |staging_host|
        Yazino::SSH.start(staging_host, config.ssh_options) do |ssh|
          ssh.exec("if [ -f #{LOCK_FILE} ]; then sudo rm -f #{LOCK_FILE}; fi")
        end
      end
    end

    def connect_to_vpn(config)
      @vpn = Yazino::VPN.new(config.vpn_name, config.vpn_type)
      @vpn.connect
    end

    def disconnect_from_vpn
      @vpn.disconnect if @vpn
    end

    def get_components(config)
      if @target =~ /\.release/ || @target =~ /strata-release-.*/
        Components.new(config, @deployment_list, parse_artefact_versions(@target, config))
      else
        Components.new(config, @deployment_list)
      end
    end

    def parse_artefact_versions(target, config)
      if File.exists?(target)
        puts "DEBUG: Deploy: Using local release definition #{target}".foreground(:green) if config.debug?
        release_file = target

      elsif File.exists?("../#{target}") # the current dir is not exactly where the user may expect it
        puts "DEBUG: Deploy: Using local release definition ../#{target}".foreground(:green) if config.debug?
        release_file = "../#{target}"

      elsif target =~ /^http:\/\//
        puts "DEBUG: Deploy: Fetching release definition #{target}".foreground(:green) if config.debug?
        release_file = "#{config.working_dir}/#{/([^\/]+)$/.match(target)[1]}"
        File.open(release_file, 'w') do |local_file|
          open(target) do |remote_file|
            remote_file.each_line {|line| local_file << line}
          end
        end

      else
        puts "DEBUG: Deploy: Checking release server for release definition #{target}".foreground(:green) if config.debug?
        release_file = get_release_definition(target, config)
      end

      release_definition = File.open(release_file) { |yf| YAML::load(yf) }

      puts "DEBUG: Deploy: Using release definition #{release_definition}".foreground(:green) if config.debug?
      release_definition['versions']
    end

    def get_release_definition(target, config)
      destination = config.working_dir

      s3 = AWS::S3.new(:access_key_id => config.s3['access-key'], :secret_access_key => config.s3['secret-key'])
      release_bucket = s3.buckets['yazino-releases']
      available_files = release_bucket.objects.collect {|obj| obj.key}

      if target =~ /\.release$/ && available_files.index(target)
        get_from_bucket(release_bucket, target, "#{destination}/#{target}")
        "#{destination}/#{target}"

      elsif available_files.index("#{target}.release")
        get_from_bucket(release_bucket, "#{target}.release", "#{destination}/#{target}.release")
        "#{destination}/#{target}.release"

      else
        raise "Cannot find release definition on release server: #{target}"
      end
    end

    def get_from_bucket(bucket, filename, destination)
      open(destination, 'w') do |file|
        bucket.objects[filename].read {|chunk| file.write chunk}
      end
    end

    def get_last_release_name(config)
      target = nil

      s3 = AWS::S3.new(:access_key_id => config.s3['access-key'], :secret_access_key => config.s3['secret-key'])
      release_bucket = s3.buckets['yazino-releases']
      available_files = release_bucket.objects.collect {|obj| obj.key}

      target = available_files.keep_if {|file| file =~ /^strata-release-\d+(\.\d+)?\.txt$/}.sort.last.gsub(/\.txt$/, '')
      raise "Couldn't find latest release name, available files were: #{available_files}" if target.nil?

      target
    end

    def verify_hosts_exist(config)
      hosts = config['hosts']
      hosts.each_key do |host|
        raise "Cannot ping host #{host} within 10s, aborting deployment" if !Yazino::Ping::ping(host, 10)
      end
    end

    def prepare_config(config)
      case @action
        when :deploy, :redeploy, :stage
          config.set_staged_name(@target) if (@target != 'local' && @target != 'dev' && @target != 'shared' && @target != 'current' && @target != 'latest-release')

          print "DEBUG: Deploy: Setting staging name to #{config.staged_name}\n".foreground(:green) if config.debug?
      end

      if @options.has_key?(:verbose)
        config.set_debug(true)
        config.ssh_options['debug'] = true
      end
    end

    def action_in(*actions)
      actions.include?(@action)
    end

    def describe_action(action)
      case action
        when :deploy
          "Deploy"
        when :redeploy
          "Redeploy"
        when :rollback
          "Rollback"
        when :stage
          "Stage"
        when :dumplogs
          "Log Dump"
        when :maintenance
          "Maintenance"
        when :what
          "What, Where"
        else
          "Unknown"
      end
    end

    def get_action_and_target(options)
      if options.include?(:deploy)
        [:deploy, options[:deploy]]
      elsif options.include?(:redeploy)
        [:redeploy, options[:redeploy]]
      elsif options.include?(:stage)
        [:stage, options[:stage]]
      elsif options.include?(:dumplogs)
        [:dumplogs, options[:dumplogs]]
      elsif options.include?(:maintenance)
        [:maintenance, options[:maintenance]]
      elsif options.include?(:rollback)
        [:rollback, options[:rollback]]
      elsif options.include?(:what)
        [:what, options[:what]]
      else
        raise "No action specified"
      end
    end

  end
end

def notify(message, title = "Deployment")
  if RUBY_PLATFORM.include?('darwin')
    terminal_notifier = %x(bash -c 'which terminal-notifier 2>/dev/null').strip
    if !terminal_notifier.nil? && terminal_notifier.length > 0
      %x(#{terminal_notifier} -title '#{title}' -message '#{message.gsub(/'/, '"')}')
    end
  end
end

if __FILE__ == $0
  OPTIONS_MAP = {
      :skip_chef => ['--skip-chef'],
      :clean => ['-c', '--clean'],
      :deploy => ['-d', '--deploy'],
      :dumplogs => ['-p', '--print-logs'],
      :force_deploy => ['-f', '--force-deploy'],
      :force_stage => ['--force-stage'],
      :hot => ['--x-hot'],
      :live => ['-l', '--live'],
      :skip_maintenance => ['-k', '--skip-maintenance'],
      :maintenance => ['--maintenance'],
      :patch => ['--patch'],
      :redeploy => ['-r', '--redeploy'],
      :rollback => ['--rollback'],
      :stage => ['-s', '--stage'],
      :timing => ['-t', '--timing'],
      :what => ['-w', '--what'],
      :verbose => ['-v', '--verbose']
  }

  def parse_arguments(args)
    raise "Usage: #{__FILE__} <ACTIONS> [OPTIONS] <--environment=env|-e=env> <artefact-id|artefact-name|artefact-group|all>
Where ACTIONS are one of:
  --deploy=type | -d=type                Deploy to the given environment, where type is one of:
                                         - dev - get local snapshots if newer than the current version.
                                         - current - get the current component versions from components.yaml.
                                         - latest-release - get the most recent (highest versioned) release.
                                         - <release-name> - get the named release.
  --maintenance=on|off                   Turn maintenance mode on/off.
  --print-logs=level | -p=level          Print remote logs, searching for the given level (Log4J style levels).
  --redeploy=type | -r=type              Redeploy a staged release, where type is one of the options for 'deploy'.
  --rollback                             Rollback to the previous release.
  --stage=type | -s=type                 Upload a release to the staging area, where type is one of the options for 'deploy'.
  --what | -w                            Describe what will be deployed and to where.
Where OPTIONS are zero or more of:
  --skip-chef                            Skip Chef during deployment.
  --clean | -c                           Remove old log files/staged artefacts.
  --force-deploy | -f                    Force deployment of all services.
  --force-stage                          Force staging of all files.
  --skip-maintenance | -k                Do not activate maintenance mode.
  --live | -l                            Use load balancer for no downtime (when applicable).
  --patch                                Patch the latest release with the new uploads (no new staging dir will be created).
  --timing | -t                          Activate timing information.
  --verbose | -v                         Activate verbose logging.
Experimental options:
** These are potentially dangerous. Never, ever use these on prod or even BMC. **
  --x-hot                                Hot deploy components where possible. This *will* cause memory leaks.
Where artefact-id|artefact-name|artefact-group may be negated using a prefix of ~.
  e.g. all ~test                         Deploy all items other than the test group.
       lobby ~web                        Deploy the lobby group other than the web artefact.

    " if args.length < 2

    artefacts = []
    options = {}
    environment = nil

    (0..args.length - 1).each do |i|
      if args[i] =~ /-e=(.*)/ || args[i] =~ /--environment=(.*)/
        environment = $1
      elsif args[i] =~ /^(--?[^=]+)(=(.*))?/
        OPTIONS_MAP.each_pair { |option, arguments| options[option] = $3 if arguments.include?($1) }
      else
        artefacts << args[i]
      end
    end

    raise "No environment specified" if environment.nil?
    raise "No options specified" if options.empty?

    [environment, artefacts, options]
  end

  config = nil
  begin
    environment, artefacts, options = parse_arguments(ARGV)

    config = Yazino::Config.load(environment)

    deploy = Yazino::Deploy.new(artefacts, options)
    deploy.run(config)

    notify("Deployment completed at #{Time.now}")

  rescue RuntimeError => e
    puts "ERROR: #{e}".foreground(:red)

    puts e.backtrace.join("\n") if !config || config.debug?

    notify("Deployment failed at #{Time.now}: #{e}")

    exit 1
  end
end
