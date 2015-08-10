require 'rubygems'
require 'bundler/setup'

require 'rainbow'

require 'method/deployment_method'
require 'ssh'
require 'erb'

class CopyMethod < DeploymentMethod

  def initialize(config, artefact, params = {})
    super(config, artefact)

    params = params || {}

    @config = config
    @artefact = artefact

    @destination_dir = params['destination_dir'] if params.has_key?('destination_dir')
    @destination_file = params['destination_file'] if params.has_key?('destination_file')
    @extract = params['extract'] || false
  end

  def maintenance_required?(options = {})
    true
  end

  def parallelisable?(step)
    case step
    when :deploy
      true
    else
      false
    end
  end

  def deploy(hosts, options = {})
    print "DEBUG: CopyMethod: Executing deploy(#{hosts}) for #{@artefact}\n".foreground(:green) if @config.debug?

    threads = []

    hosts.each do |destination_host|
      if @destination_file
        dest_file = "#{@destination_dir}/#{@destination_file}"
      else
        dest_file = "#{@destination_dir}/#{artefact_file}"
      end

      if @extract
        if artefact_file =~ /\.[jw]ar$/ || artefact_file =~ /\.zip/
          copy_cmd = "sudo unzip -o -q -d #{@destination_dir} #{staged_path}"
          dest_file = @destination_dir
        else
          raise "Unknown archive type: #{artefact_file}"
        end
      else
        copy_cmd = "if [ -f #{dest_file} ]; then cmp #{staged_path} #{dest_file} 1>/dev/null; if [ \\$? -ne 0 ]; then sudo cp #{staged_path} #{dest_file}; fi; else sudo cp #{staged_path} #{dest_file}; fi"
      end

      print "* Deploying #{@artefact} to #{destination_host} at #{dest_file}\n"

      threads << Thread.new do
        Yazino::SSH.start(destination_host, @config.ssh_options) do |ssh|
          ssh.exec("if [ ! -d #{@destination_dir} ]; then sudo mkdir #{@destination_dir} && sudo chown jetty:contentuser #{@destination_dir} && sudo chmod 775 #{@destination_dir}; fi && #{copy_cmd} && if [ -d #{dest_file} ]; then sudo chmod 775 #{dest_file}; else sudo chmod 664 #{dest_file}; fi && sudo chgrp contentuser #{dest_file}")
        end
      end
    end

    wait_for_threads("deploy", TIMEOUT, threads)
  end

  def eql?(object)
    return false if !super.eql?(object)

    return object.destination_dir == @destination_dir
  end

  def hash
    [super.hash, @destination_dir].hash
  end

  private

  TIMEOUT = 60

end
