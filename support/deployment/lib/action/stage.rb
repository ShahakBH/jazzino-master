require 'source/maven'
require 'stage/scp'
require 'thread'
require 'threaded_task'

require 'fileutils'

class StageAction
  include ThreadedTask

  def initialize(config, target, options)
    @config = config
    @options = options
    @target = target
  end

  def requires_artefacts?
    true
  end

  def exec(options, components)
    @config.check_for_lock

    stager = SCPStage.new(@config)

    gather_artefacts(@config, components)

    @config.hosts_for_roles(['staging']).each do |host|
      stager.stage(host, components, options)
    end
  end

  private

  TIMEOUT = 300
  FETCH_THREADS = 4

  def gather_artefacts(config, components)
    destination = "#{config.working_dir}"
    FileUtils.rm_rf("#{destination}/artifact.txt") if File.exists?("#{destination}/artifact.txt")

    source = get_source(config, @target)

    print "* Fetching artefacts for source '#{@target}'\n"
    File.open("#{destination}/artifact.txt", 'w') do |file|
      file.write("Artefact: #{@target}\n")
      file.write("From: #{Etc.getlogin}@#{Socket.gethostname}\n")
      file.write("Date: #{Time.now.strftime('%Y/%m/%d %H:%M:%S %Z')}\n")
    end

    component_queue = Queue.new
    components.each_pair {|component_key, component| component_queue << [component_key, component]}

    threads = []

    FETCH_THREADS.times do
      threads << Thread.new do
        while (component_key, component = component_queue.pop(true) rescue nil)
          source.fetch(destination, component['maven_group'], component_key, component['type'], component['version']) if component['type'] != 'noartefact'
        end
      end
    end

    wait_for_threads("Artefact retrieval", TIMEOUT, threads)
  end

  def get_source(config, target)
    if target =~ /\.release$/
      MavenSource.new(config, @options)
    else
      case target
      when 'local'
        print "! deploy=local is deprecated - use deploy=dev to deploy local snapshots if newer than current release versions\n".foreground(:red)
        MavenSource.new(config, @options.merge(:check_for_newer_snapshots => true))
      when 'dev'
        MavenSource.new(config, @options.merge(:check_for_newer_snapshots => true))
      else
        MavenSource.new(config, @options)
      end
    end
  end

end
