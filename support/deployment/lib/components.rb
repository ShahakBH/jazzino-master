require 'method/apache'
require 'method/jetty'
require 'method/database'
require 'method/gigaspaces'
require 'method/lightstreamer'
require 'method/copy'
require 'method/no_op'
require 'set'

module Yazino
  class Components

    DEFAULT_MAVEN_GROUP = 'com.yazino'

    def initialize(config, deployment_list, versions = {})
      @config = config
      @artefact_versions = versions

      @components = find_components(deployment_list)
    end

    def all_filenames
      @components.keys.map do |component_key|
        component = @components[component_key]
        "#{component_key}.#{component['type']}"
      end
    end

    def map_methods_to_hosts
      ordered_components.reduce({}) do |methods_to_hosts, component_key|
        component = @components[component_key]
        component_artefact = "#{component['maven_group']}:#{component_key}:#{component['type']}"

        method = get_method(component['method'], component_artefact, component['params'])
        hosts = @config.hosts_for_roles(component['deploy_to'])

        methods_to_hosts[method] = hosts
        methods_to_hosts
      end
    end

    def ordered_components
      # We don't use sort as equality is not transitive
      sorted_components = @components.keys.reduce([]) do |sorted, this|
        pos = sorted.size
        sorted.each_with_index {|other, index| pos = [pos, index].min if dependencies_of(other).include?(this)}
        sorted.insert(pos, this)
      end

      print "Deployment order is: #{sorted_components}\n".foreground(:green) if @config.debug?
      sorted_components
    end

    def dependencies_of(component)
      if @components.has_key?(component) && @components[component]['depends_on']
        @components[component]['depends_on'].reduce(Set.new) do |all_deps, dependency|
          raise "Circular dependency on #{component}" if component == dependency

          next if all_deps.include?(dependency)
          all_deps.add(dependency).merge(dependencies_of(dependency))
        end.to_a
      else
        []
      end
    end

    def get_method(method_name, artefact, params = {})
      case method_name
        when "apache"
          ApacheMethod.new(@config, artefact, params)
        when "jetty"
          JettyMethod.new(@config, artefact, params)
        when "database"
          DatabaseMethod.new(@config, artefact, params)
        when "gigaspaces"
          GigaspacesMethod.new(@config, artefact, params)
        when "lightstreamer"
          LightstreamerMethod.new(@config, artefact, params)
        when "copy"
          CopyMethod.new(@config, artefact, params)
        when "noop"
          NoOpMethod.new(@config, artefact)
        else
          RuntimeError.raise("Unknown deployment method: #{method_name}")
      end
    end

    def get_log_directories
      @components.each_value.reduce(Set.new) { |log_dirs, component| log_dirs << component['log_directory'] if component.has_key?('log_directory') }
    end

    def [](key)
      @components[key]
    end

    def each_key(&block)
      @components.each_key(&block)
    end

    def each_pair(&block)
      @components.each_pair(&block)
    end

    def keys
      @components.keys
    end

    def values
      @components.values
    end

    def empty?
      @components.empty?
    end

    private

    def find_components(deployment_list)
      components_to_deploy = find_components_to_deploy(component_config, deployment_list.find_all {|item| item !~ /^~/})

      items_to_exclude = deployment_list.find_all {|item| item =~ /^~/}.map {|item| item[1..item.size]}
      filter_excluded_items(components_to_deploy, items_to_exclude)
    end

    def filter_excluded_items(components_to_deploy, items_to_exclude)
      components_to_deploy.delete_if do |component_key, component|
        items_to_exclude.include?(component_key) \
            || !(items_to_exclude & component['short_name'].split(',')).empty? \
            || items_to_exclude.include?(component['group'])
      end
    end

    def find_components_to_deploy(components, deployment_list)
      components_to_deploy = {}

      deployment_list.each do |deployment_item|
        if deployment_item == 'all'
          components.each_pair do |component_key, component|
            components_to_deploy[component_key] = populate_component(component_key, component)
          end
          found = true

        else
          found = false
          if !components[deployment_item].nil?
            components_to_deploy[deployment_item] = populate_component(deployment_item, components[deployment_item])
            found = true
          else
            components.each_pair do |component_key, component|
              if component['group'] == deployment_item || component['short_name'].split(',').include?(deployment_item)
                components_to_deploy[component_key] = populate_component(component_key, component)
                found = true
              end
            end
          end
        end

        raise "#{deployment_item} is not a valid component or group" if !found
      end

      components_to_deploy
    end

    def populate_component(component_key, component)
      component['version'] = version_for(component_key, component) if component['type'] != 'noartefact' && component['type']  != 'noartifact'
      component
    end

    def version_for(artefact, component)
      # TODO allow snapshot override param

      if @artefact_versions && @artefact_versions.has_key?(artefact)
        @artefact_versions[artefact]
      elsif component.has_key?('version')
        component['version']
      else
        raise "Couldn't find version for artefact #{artefact}"
      end
    end

    def component_config
      config_file = "#{@config.config_dir}/components.yaml"
      raise "Cannot find components configuration" if !File.exists?(config_file)

      component_config = File.open(config_file) { |yf| YAML::load(yf) }

      if File.exists?("#{@config.environment_dir}/components.yaml")
        component_config = component_config.update("#{@config.environment_dir}/components.yaml")
      end

      populate_defaults(component_config)

      raise "No components loaded" if component_config.nil?

      component_config
    end
    def populate_defaults(components)
      components.each_pair do |key, component|
        component['maven_group'] = DEFAULT_MAVEN_GROUP if !component.has_key?('maven_group')
      end
    end

  end
end
