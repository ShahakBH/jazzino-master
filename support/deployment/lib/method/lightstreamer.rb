require 'rubygems'
require 'bundler/setup'
require 'method/deployment_method'
require 'ssh'

class LightstreamerMethod < DeploymentMethod

  def initialize(config, artefact, params = {})
    super(config, artefact)

    params = params || {}

    @base_dir = "/opt/lightstreamer-adapter"
    @install_dir = "#{@base_dir}/#{@config.timestamp}"
    @current_adapters_symlink = "#{@base_dir}/current"
    @application_adapters_symlink = "/opt/lightstreamer/adapters"

    if params.has_key?('retained_uploads')
      @retained_uploads = params['retained_uploads']
    else
      @retained_uploads = DEFAULT_RETAINED_UPLOADS
    end

    print "DEBUG: LightstreamerMethod: Versioned Dir is (#{@install_dir}), for #{@artefact}, retained uploads is #{@retained_uploads}\n".foreground(:green) if @config.debug?
  end

  def maintenance_required?(options = {})
    false
  end

  def parallelisable?(step)
    case step
    when :pre_deploy
      true
    else
      false
    end
  end

  def pre_deploy(hosts, options = {})
    print "DEBUG: LightstreamerMethod: Executing pre_deploy(#{hosts}) for #{@artefact}\n".foreground(:green) if @config.debug?
    threads = []

    hosts.each do |ls_host|
      threads << Thread.new do
        stop_service_on(ls_host, "lightstreamer")
      end
    end

    wait_for_threads("Lightstreamer pre-deployment", TIMEOUT, threads)
  end

  def deploy(hosts, options = {})
    print "DEBUG: LightstreamerMethod: Executing deploy(#{hosts}) for #{@artefact}\n".foreground(:green) if @config.debug?

    threads = []

    hosts.each do |ls_host|
      threads << Thread.new do
        Yazino::SSH.start(ls_host, @config.ssh_options) do |ssh|
          print "* Deploying #{@artefact} to Lightstreamer on #{ls_host} at #{@install_dir}\n"
          ssh.exec("sudo mkdir -p #{@install_dir} && cd #{@install_dir} && sudo tar xvf #{staged_path} && sudo chown -R lightstreamer:lightstreamer #{@install_dir} && sudo chmod -R 755 #{@install_dir} && sudo rm -rf #{@application_adapters_symlink} && sudo rm -rf #{@current_adapters_symlink} && sudo ln -s #{@install_dir} #{@current_adapters_symlink} && sudo ln -s #{@current_adapters_symlink} #{@application_adapters_symlink}")
        end
      end
    end

    wait_for_threads("Lightstreamer deployment", TIMEOUT, threads)
  end

  def post_deploy(hosts, options = {})
    print "DEBUG: LightstreamerMethod: Executing post_deploy(#{hosts}) for #{@artefact}\n".foreground(:green) if @config.debug?

    threads = []

    hosts.each do |ls_host|
      threads << Thread.new do

        Yazino::SSH.start(ls_host, @config.ssh_options) do |ssh|
          directories = ssh.exec("ls -1 -d --sort=time #{@base_dir}/* | grep -v current").split("\n")

          if directories.size <= @retained_uploads
            print "DEBUG: LightstreamerMethod: No deletions required on #{ls_host}\n".foreground(:green) if @config.debug?
          else
            dirs_to_delete = directories.last(directories.size - @retained_uploads)
            print "DEBUG: LightstreamerMethod: Deleting #{dirs_to_delete} from #{ls_host}\n".foreground(:green) if @config.debug?
            ssh.exec("sudo rm -rf #{dirs_to_delete.join(" ")}")
          end

          start_service_on(ls_host, "lightstreamer")
        end
      end
    end

    wait_for_threads("Lightstreamer post-deployment", TIMEOUT, threads)
  end

  def eql?(object)
    return false if !super.eql?(object)

    return object.install_dir == @install_dir\
        && object.application_adapters_symlink == @application_adapters_symlink\
        && object.current_adapters_symlink == @current_adapters_symlink
  end

  def hash
    [super.hash, @install_dir, @application_adapters_symlink, @current_adapters_symlink].hash
  end

  private

  TIMEOUT = 60
  DEFAULT_RETAINED_UPLOADS = 4


end