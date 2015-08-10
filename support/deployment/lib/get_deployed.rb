#!/usr/bin/env ruby

require 'rubygems'

require 'yaml'

require 'ssh'

module Yazino

  class GetDeployedArtefact

    def initialize(environment)
      @config = YAML.load_file(config_file).merge(YAML.load_file(environment_config_file(environment)))
    end

    def deployed_artefact
      artifact_text = Yazino::SSH.start(staging_host_from(@config['hosts']), @config['ssh_options']) do |ssh|
        ssh.exec('cat /etc/senet/artifact.txt')
      end

      if artifact_text =~ /Artefact: (.*)/
        $1
      else
        raise "Cannot determine deployed artefact for #{environment}"
      end
    end

    private

    def staging_host_from(hosts)
      staging_hosts = hosts.select do |hostname, host_definition|
        roles = (host_definition['roles'] || [])
        roles.include?('all') || roles.include?('staging')
      end
      raise "No staging hosts defined" if staging_hosts.empty?
      staging_hosts.keys.first
    end

    def base_dir
      File.expand_path("#{File.dirname(__FILE__)}/..")
    end

    def config_dir
      File.expand_path("#{base_dir}/config")
    end

    def config_file
      "#{config_dir}/deployment.yaml"
    end

    def environment_config_file(name)
      "#{config_dir}/environments/#{name}/environment.yaml"
    end

  end

end

if __FILE__ == $0
  begin
    raise "Usage: #{__FILE__} <environment name>" if ARGV.empty?

    puts Yazino::GetDeployedArtefact.new(ARGV[0]).deployed_artefact
    exit 0

  rescue => e
    puts "#{e}"
    exit 1
  end
end
