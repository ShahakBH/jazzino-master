require 'yaml'
require 'fileutils'

module Yazino

  class Config

    def self.load(environment)
      Config.new("#{default_config_dir}/deployment.yaml").merge(environment)
    end

    def initialize(config_file)
      @config = load_from_file(config_file)
      @deploy_time = Time.now.utc
      @staged_name = timestamp
      @debug = false
    end

    def merge(environment)
      print "DEBUG: Config: Merging config for environment #{environment}\n" if debug?

      @environment = environment

      config_file = environment_config
      if (!config_file.nil? && File.exists?(config_file))
        @config.update(load_from_file(config_file))
      end
      self
    end

    def [](key)
      @config[key]
    end

    def config_dir
      Config.default_config_dir
    end

    def environment
      @environment
    end

    def environment_dir
      "#{config_dir}/environments/#{@environment}"
    end

    def maven
      @config['maven'] || []
    end

    def s3
      @config['s3'] || []
    end

    def timestamp
      @deploy_time.strftime('%Y%m%dT%H%M%S')
    end

    def deploy_time
      @deploy_time
    end

    def ssh_options
      @config['ssh_options']
    end

    def base_staging_dir
      "#{@config['staging_dir']}"
    end

    def latest_staging_dir
      "#{base_staging_dir}/latest"
    end

    def set_staged_name(new_staged_name)
      @staged_name = new_staged_name
    end

    def staged_name
      @staged_name
    end

    def staging_dir
      "#{@config['staging_dir']}/#{staged_name}"
    end

    def working_dir
      working_dir = @config['working_dir']
      if !File.exists?(working_dir)
        FileUtils.mkdir_p(working_dir)
      end

      raise "Couldn't create: #{working_dir}" if !File.exists?(working_dir) || !File.directory?(working_dir)
      working_dir
    end

    def clean_working_dir
      FileUtils.rm_rf(working_dir) if File.exists?(working_dir)
    end

    def hosts
      @config['hosts'].keys
    end

    def hosts_for_roles(roles)
      hosts = []
      @config['hosts'].each_key do |hostname|
        host = @config['hosts'][hostname]
        if host && host['roles']
          hosts << hostname if host['roles'].include?('all') || roles.reduce(false) { |has_role, role| has_role || host['roles'].include?(role) }
        end
      end
      raise "No hosts were found for roles: #{roles}" if hosts.empty?
      hosts
    end

    def params_for_host(host)
      raise "No host exists with name: #{host}" if !@config['hosts'].has_key?(host)

      @config['hosts'][host]['params'] || {}
    end

    def has?(key)
      @config.has_key?(key)
    end

    def set_debug(new_debug)
      @debug = new_debug
    end

    def debug?
      @config['debug'] || @debug
    end

    def log_dir
      log_dir = "#{File.dirname(__FILE__)}/../logs"
      FileUtils.mkdir_p(log_dir) if !File.exists?(log_dir)
      log_dir
    end

    def check_for_lock
      if lock_file && !File.exists?(lock_file)
        raise "Environment is locked; please create #{lock_file} to proceed"
      end
    end

    def unlock
      File.delete(lock_file) if lock_file && File.exists?(lock_file)


    end

    def auto_resume?
      if @config.has_key?('auto_resume')
        @config['auto_resume']
      else
        true
      end
    end

    def vpn_required?
      @config.has_key?('vpn_name')
    end

    def vpn_name
      @config['vpn_name']
    end

    def vpn_type
      if @config['vpn_type']
        @config['vpn_type']
      else
        'cisco-ipsec'
      end
    end

    private

    def lock_file
      if @config['lock_file']
        "#{File.dirname(__FILE__)}/../#{@config['lock_file']}"
      else
        nil
      end
    end

    def self.default_config_dir
      "#{File.dirname(__FILE__)}/../config"
    end

    def environment_config
      raise "No environment is set" if @environment.nil?

      env_config = "#{environment_dir}/environment.yaml"
      raise "Invalid environment: #{@environment}" if !File.exists?(env_config)
      env_config
    end

    def load_from_file(filename)
      File.open(filename) { |yf| YAML::load(yf) }
    end

  end
end
