require 'maintenance/web'
require 'maintenance/pingdom'

class MaintenanceAction

  def initialize(config, options)
    @config = config
    @options = options
  end

  def requires_artefacts?
    false
  end

  def exec(options, components)
    @config.check_for_lock

    pingdom = Pingdom.new(@config) if @config['pingdom']
    maintenance = WebMaintenance.new(@config)

    if options[:maintenance]
      if @options[:maintenance] == "on"
        pingdom.suspend if pingdom
        maintenance.on
      elsif @options[:maintenance] == "off"
        maintenance.off
        pingdom.resume if pingdom
      end
    end
  end

end
