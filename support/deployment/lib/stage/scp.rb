require 'scp'
require 'components'

require 'set'
require 'thread'
require 'threaded_task'

class SCPStage
  include ThreadedTask

  def initialize(config)
    @config = config
  end

  def stage(host, components, options)
    component_config = config_for(components)
    filenames = filenames_for(components)

    print "DEBUG: SCPStage: Staging #{filenames} to #{host}:#{@config.staging_dir}\n".foreground(:green) if @config.debug?

    Yazino::SSH.start(host, @config.ssh_options) do |ssh|
      force = options.has_key?(:force_stage)
      patch = options.has_key?(:patch)

      start_time = Time.now
      staging_dir = @config.staging_dir
      artefacts_dir = "#{@config.base_staging_dir}/artefacts"

      latest_link = @config.latest_staging_dir

      if patch
        print "DEBUG: Patching latest release\n".foreground(:green) if @config.debug?
        staging_dir = latest_link
      end

      if force
        print "* Forcing stage of deployment to #{host} as #{@config.staged_name}\n"
      elsif patch
        print "* Patching latest deployment on #{host}\n"
      else
        print "* Staging deployment to #{host} as #{@config.staged_name}\n"
      end

      config_dirs = Set.new(component_config.keys).map {|dir| "#{staging_dir}/#{dir}"}.join(" ")

      if options.include?(:clean)
        print "DEBUG: SCPStage: Cleaning old staged files in  #{@config.base_staging_dir} on #{host}\n".foreground(:green) if @config.debug?
        ssh.exec("sudo find -L #{@config.base_staging_dir} -mindepth 1 -maxdepth 1 -not -newer #{latest_link} -not -samefile #{latest_link} -not -name artefacts -type d | sudo xargs rm -rf")
      end

      if patch
        ssh.exec("sudo mkdir -p #{artefacts_dir} #{config_dirs} && sudo chown -R spanner:spanner #{config_dirs} #{artefacts_dir}")
      else
        ssh.exec("sudo mkdir -p #{staging_dir} #{artefacts_dir} #{config_dirs} && sudo chown -R spanner:spanner #{staging_dir} #{artefacts_dir}")
      end

      Yazino::SCP.start(host, @config.ssh_options) do |scp|
        scp.upload_to(staging_dir, ["#{@config.working_dir}/artifact.txt"]) if File.exists?("#{@config.working_dir}/artifact.txt")

        filename_queue = Queue.new
        filenames.each {|filename| filename_queue << filename}

        config_files_queue = Queue.new
        component_config.each_pair {|role, filenames_for_role| config_files_queue << [role, filenames_for_role]}

        threads = []

        UPLOAD_THREADS.times do
          threads << Thread.new do
            while filename = filename_queue.pop(true) rescue nil
              if filename =~ /(.*?)-[-\d.]+(\.[A-Za-z.]+)$/
                unversioned_filename = "#{$1}#{$2}"
                upload_if_required(scp, ssh, force || patch, filename, artefacts_dir)
                ssh.exec("sudo ln -sf #{artefacts_dir}/#{filename_of(filename)} #{staging_dir}/#{filename_of(unversioned_filename)}")
              else
                upload_if_required(scp, ssh, force || patch, filename, staging_dir)
              end
            end

            while (role, filenames_for_role = config_files_queue.pop(true) rescue nil)
              checked_files = resolve_files(filenames_for_role)
              scp.upload_to("#{staging_dir}/#{role}", checked_files)
            end
          end
        end

        wait_for_threads("Staging", UPLOAD_TIMEOUT, threads)
      end

      print "t Staging took #{start_time - Time.now}\n" if options[:timing]
    end
  end

  private

  UPLOAD_THREADS = 2
  UPLOAD_TIMEOUT = 900

  def upload_if_required(scp, ssh, always_upload, filename, destination_dir)
    if always_upload || ssh.exec("if [ -f #{destination_dir}/#{filename_of(filename)} ]; then echo filealreadyexists; fi") !~ /filealreadyexists/
      print "DEBUG: SCPStage: #{destination_dir}/#{filename_of(filename)} does not exist; uploading\n".foreground(:green) if @config.debug?
      scp.upload_to(destination_dir, [filename])
    else
      print "DEBUG: SCPStage: #{destination_dir}/#{filename_of(filename)} already exists; not uploading\n".foreground(:green) if @config.debug?
    end
  end

  def resolve_files(filenames)
    filenames.map do |filelist|
      print "DEBUG: SCPStage: Resolving config files #{filelist}\n".foreground(:green) if @config.debug?
      found = nil
      filelist.split(',').each do |file|
        if File.exists?("#{@config.environment_dir}/#{file}")
          print "DEBUG: SCPStage: Using environment override config for file #{file}\n".foreground(:green) if @config.debug?
          found = "#{@config.environment_dir}/#{file}"
          break
        end
      end

      if !found
        filelist.split(',').each do |file|
          if File.exists?("#{@config.environment_dir}/../shared/#{file}")
            print "DEBUG: SCPStage: Using shared config for file #{file}\n".foreground(:green) if @config.debug?
            found = "#{@config.environment_dir}/../shared/#{file}"
            break
          end
        end
      end

      raise "Couldn't resolve config files #{filelist}" if !found
      found
    end
  end

  def filename_of(path)
    if path =~ /.*\/([^\/]+)$/
      $1
    else
      path
    end
  end

  def filenames_for(components)
    components.keys.find_all {|component_key| components[component_key]['type'] != 'noartefact'}.map do |component_key|
      component = components[component_key]
      if component.has_key?('version') \
          && component['version'] !~ /-SNAPSHOT/ \
          && File.exists?("#{@config.working_dir}/#{component_key}-#{component['version']}.#{component['type']}")
        "#{@config.working_dir}/#{component_key}-#{component['version']}.#{component['type']}"
      elsif File.exists?("#{@config.working_dir}/#{component_key}.#{component['type']}")
        "#{@config.working_dir}/#{component_key}.#{component['type']}"
      else
        raise "Could not find a source file for #{component_key}"
      end
    end
  end

  def config_for(components)
    config = {}
    components.each_pair do |component_key, component|
      raise "deploy_to missing for #{component_key}" if component['deploy_to'].nil?

      component['deploy_to'].each do |role|
        config_files = component['config_files']

        if !config_files.nil? && !config_files.empty?
          config[role] = config[role] || []
          config[role] += config_files.keys
        end
      end
    end
    config
  end

end
