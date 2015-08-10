require 'digest/md5'
require 'ssh'
require 'threaded_task'

class DeploymentMethod
  include ThreadedTask

  def initialize(config, artefact)
    @config = config
    @artefact = artefact
  end

  def run(step, hosts, params)
    self.send(step, hosts, params) if self.class.method_defined?(step)
  end

  def maintenance_required?(options = {})
    true
  end

  def parallelisable?(lifecycle_step)
    false
  end

  protected

  def config
    @config
  end

  def artefact
    @artefact
  end

  def staged_path
    "#{@config.latest_staging_dir}/#{artefact_file}"
  end

  def artefact_file
    artefact_match = @artefact.match(/(.*):(.*):(.*)/)
    "#{artefact_match[2]}.#{artefact_match[3]}"
  end

  def stop_service_on(host, service_name, stop_delay = SERVICE_STOP_DELAY, retries = MAX_SERVICE_ATTEMPTS)
    Yazino::SSH.start(host, @config.ssh_options.merge('exit_on_error' => false)) do |ssh|

      ssh.exec("sudo su -c '/sbin/service #{service_name} status 2>&1 | grep -i running >/dev/null; if [ $? -eq 0 ]; then rm -rf /var/log/#{service_name}.pid; fi'; exit 1")

      attempt = 1
      while (service_status = ssh.exec("sudo su -c '/sbin/service #{service_name} status 2>&1'")) =~ /running/
        print "DEBUG: DeploymentMethod: Service status of #{service_name} on #{host} is #{service_status}\n".foreground(:green) if @config.debug?
        raise "Service #{service_name} failed to stop on #{host}" if attempt > retries

        print "* Stopping #{service_name} on #{host} (#{attempt}/#{retries})\n"
        ssh.exec("sudo su -c '/sbin/service #{service_name} stop'")
        sleep(stop_delay)
        attempt += 1
      end
      print "DEBUG: DeploymentMethod: Service status of #{service_name} on #{host} is #{service_status}\n".foreground(:green) if @config.debug?

      attempt > 1
    end
  end

  def start_service_on(host, service_name)
    Yazino::SSH.start(host, @config.ssh_options.merge('exit_on_error' => false)) do |ssh|
      service_status = ssh.exec("sudo su -c '/sbin/service #{service_name} status 2>&1'")
      print "DEBUG: DeploymentMethod: Service status of #{service_name} on #{host} is #{service_status}\n".foreground(:green) if @config.debug?
      if service_status !~ /running/
        print "* Starting #{service_name} on #{host}\n"
        ssh.exec("sudo su -c '/sbin/service #{service_name} start'")
        true
      else
        false
      end
    end
  end

  def restart_service_on(host, service_name)
    Yazino::SSH.start(host, @config.ssh_options.merge('exit_on_error' => false)) do |ssh|
      service_status = ssh.exec("sudo su -c '/sbin/service #{service_name} status 2>&1'")
      print "DEBUG: DeploymentMethod: Service status on #{host} is #{service_status}\n".foreground(:green) if @config.debug?
      if service_status !~ /running/
        print "* Starting #{service_name} on #{host}\n"
        ssh.exec("sudo su -c '/sbin/service #{service_name} start'")
      else
        if service_name == 'httpd'
          print "* Restarting #{service_name} gracefully on #{host}\n"
          ssh.exec("sudo su -c '/sbin/service #{service_name} graceful'")
        else
          print "* Restarting #{service_name} on #{host}\n"
          ssh.exec("sudo su -c '/sbin/service #{service_name} restart'")
        end
      end
    end
  end

  def remote_files_differ(host, file1, file2)
    Yazino::SSH.start(host, @config.ssh_options.merge('exit_on_error' => false)) do |ssh|
      compare_status = ssh.exec("if [ -f #{file1} -a -f #{file2} ]; then cmp #{file1} #{file2} &> /dev/null; echo \\$?; else echo 1; fi")
      case compare_status.to_i
      when 0
        false
      when 1
        true
      else
        raise "Remote diff failed with error #{compare_status} for #{file1} and #{file2} on #{host}"
      end
    end
  end

  def process_generated_deployment_properties(deployment_properties, hosts, config)
    print "DEBUG: DeploymentMethod: Processing generated deployment properties (#{deployment_properties})\n".foreground(:green) if @config.debug?

    host = hosts.first
    Yazino::SSH.start(host, config.ssh_options) do |ssh|
      next if config.params_for_host(host)['external']

      (deployment_properties || {}).each_key do |property|
        property_value = deployment_properties[property]

        if config.params_for_host(host).has_key?(property)
          property_value = config.params_for_host(host)[property]
        end

        if property_value
          property_value = property_value \
              .gsub(/\$TIMESTAMP/, config.timestamp) \
              .gsub(/\$ENVIRONMENT/, config.environment)

          print "* Updating #{GENERATED_PROPERTIES} via #{host}: #{property} to #{property_value}\n"

          properties_file = "#{config.latest_staging_dir}/#{GENERATED_PROPERTIES}"
          escaped_property = property.gsub(/\./, '\.')
          ssh.exec("if [ -f '#{properties_file}' -a -n \\\"\\$(grep #{property} #{properties_file})\\\" ]; then sudo sed -i'' -e 's%#{escaped_property}=.*$%#{property}=#{property_value}%g' '#{properties_file}'; else sudo bash -c 'echo #{property}=#{property_value} >> #{properties_file}'; fi")
        end
      end
    end
  end

  def eql?(object)
    if object.equal?(self)
     return true
    elsif !self.class.equal?(object.class)
     return false
    end

    return object.config == @config\
        && object.artefact == @artefact
  end

  def hash
    [self.class, @config, @artefact].hash
  end

  private

  GENERATED_PROPERTIES = 'environment.generated.properties'
  MAX_SERVICE_ATTEMPTS = 30
  SERVICE_STOP_DELAY = 2

end
