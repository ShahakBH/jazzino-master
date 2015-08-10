require 'rubygems'
require 'bundler/setup'

require 'rainbow'

require 'method/deployment_method'
require 'ssh'
require 'scp'
require 'fileutils'
require 'erb'

class ApacheMethod < DeploymentMethod

  def initialize(config, artefact, params = {})
    super(config, artefact)

    params = params || {}

    @config = config
    @artefact = artefact
    @never_timestamped = false

    @never_timestamped = params['never_timestamped'] if params.has_key?('never_timestamped')
    @destination_dir = params['destination_dir'] if params.has_key?('destination_dir')
    @deployment_properties = params['generated_properties'] if params.has_key?('generated_properties')

    if params.has_key?('retained_uploads')
      @retained_uploads = params['retained_uploads']
    else
      @retained_uploads = DEFAULT_RETAINED_UPLOADS
    end
  end

  def maintenance_required?(options = {})
    false
  end

  def pre_deploy(hosts, options = {})
    print "DEBUG: ApacheMethod: Executing pre_deploy(#{hosts}) for #{@artefact}\n".foreground(:green) if @config.debug?

    threads = []

    hosts.each do |apache_host|
      threads << Thread.new do
        Yazino::SSH.start(apache_host, @config.ssh_options) do |ssh|
          dest_dir = destination(timestamped?(apache_host))
          print "* Uploading #{@artefact} to Apache on #{apache_host} at #{dest_dir}\n"

          ssh.exec("if [ -d #{dest_dir} ]; then sudo rm -rf #{dest_dir}; fi; sudo mkdir -p #{dest_dir} && sudo cp -f #{staged_path} /tmp/apache-method.zip && sudo unzip -o -q -d #{dest_dir} /tmp/apache-method.zip && sudo chown -R apache:apache #{dest_dir}")
        end
      end
    end

    wait_for_threads("Apache pre-deployment", TIMEOUT, threads)

    process_generated_deployment_properties(@deployment_properties, hosts, config)
  end

  def deploy(hosts, options = {})
    print "DEBUG: ApacheMethod: Executing deploy(#{hosts}) for #{@artefact}\n".foreground(:green) if @config.debug?
  end

  def post_deploy(hosts, options = {})
    print "DEBUG: ApacheMethod: Executing post_deploy(#{hosts}) for #{@artefact}\n".foreground(:green) if @config.debug?

    threads = []

    hosts.each do |apache_host|
      threads << Thread.new do
        if timestamped?(apache_host)
          print "* Cleaning up old Apache uploads on #{apache_host}\n"

          print "DEBUG: ApacheMethod: Deleting all but #{@retained_uploads} uploads for #{artefact} from #{apache_host}\n".foreground(:green) if @config.debug?

          Yazino::SSH.start(apache_host, @config.ssh_options) do |ssh|
            directories = ssh.exec("ls -1 -d --sort=time #{@destination_dir}* | grep -v total").split("\n")

            if directories.size <= @retained_uploads
              print "DEBUG: ApacheMethod: No deletions required on #{apache_host}\n".foreground(:green) if @config.debug?
            else
              dirs_to_delete = directories.last(directories.size - @retained_uploads)
              print "DEBUG: ApacheMethod: Deleting #{dirs_to_delete} from #{apache_host}\n".foreground(:green) if @config.debug?

              ssh.exec("sudo rm -rf #{dirs_to_delete.join(" ")}")
            end
          end
        end

        restart_service_on(apache_host, "httpd")
      end
    end

    wait_for_threads("Apache post-deployment", TIMEOUT, threads)
  end

  def eql?(object)
    return false if !super.eql?(object)

    return object.destination_dir == @destination_dir\
        && object.default_timestamped == @never_timestamped\
        && object.retained_uploads == @retained_uploads
  end

  def hash
    [super.hash, @destination_dir, @never_timestamped, @retained_uploads].hash
  end

  private

  TIMEOUT = 120
  DEFAULT_RETAINED_UPLOADS = 4

  def destination(timestamped)
    destination = @destination_dir
    destination += "-#{@config.timestamp}" if timestamped
    destination
  end

  def timestamped?(host)
    if @never_timestamped
      false
    else
      @config.params_for_host(host)['apache_timestamped']
    end
  end

  def filename(path)
    /([^\/]+)$/.match(path)[1]
  end

  def base_dir(path)
    if path =~ /(.*)\/[^\/]+$/
      $1
    else
      '.'
    end
  end

end
