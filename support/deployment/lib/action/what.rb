require 'rubygems'
require 'bundler/setup'

require 'rainbow'
require 'set'

require 'source/maven'

class WhatAction

  def initialize(config)
    @config = config
  end

  def requires_artefacts?
    true
  end

  def exec(options, components)
    print "\n------------------------------------------------------------------\n".bright
    print " What will be deployed, where?\n".bright
    print "------------------------------------------------------------------\n".bright

    print_components_for_hosts(components)
    print_component_descriptions(components)
  end

  private

  def print_component_descriptions(components)
    print "\n\nComponents:\n".bright

    maven_source = MavenSource.new(@config)

    components.each_pair do |component_key, component|
      begin
        description = maven_source.describe(component['maven_group'], component_key, component['version'])

        print "\n#{component_key} "
        print "@ #{component['version']} ".foreground(:green)
        print "- #{description[:name]}\n".foreground(:blue)
        if !description[:description].nil?
          print "#{description[:description]}\n".foreground(:yellow)
        end
      rescue => e
        print "\n#{component_key} query failed due to:\n".color(:red)
        print e.to_s.color(:red)
        print "\n"
      end
    end
  end

  def print_components_for_hosts(components)
    @config.hosts.each do |hostname|
      roles = @config['hosts'][hostname]['roles']
      logs = Set.new
      host_components = {}

      components.each_pair do |component_key, component|
        if !((component['deploy_to'] || []) & (roles || [])).empty? || roles.include?('all')
          host_components[component_key] = component
          logs = logs + Set.new(component['log']['files']) if component['log'] && component['log']['files']
        end
      end

      print "\nHost: #{hostname} ".bright
      print "(#{roles.join(', ')})\n".foreground(:blue)

      if !host_components.empty?
        print "Components:\n"
        host_components.each_pair do |component_key, component|
          print " * #{component_key}"
          print " (#{component['method']})\n".foreground(:green)
        end
      end

      if !logs.empty?
        print "Logs:\n"
        logs.to_a.sort.each {|log_file| print " * #{log_file}\n"}
      end
    end
  end
end
