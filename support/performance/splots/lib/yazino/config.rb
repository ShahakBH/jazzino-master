require 'YAML'
class YazinoConfig
  def initialize(environment = "breakmycasino")
    @environment = environment
    @bot_type = "Splots"
    @yazino_config = get_config
  end

  def get_server
    property = 'server'
    if !@yazino_config.has_key?(property)
      puts "property #{property} is undefined in config #{@environment}"
      exit 1
    end
    @yazino_config[property]
  end

  def get_user
    property = 'user'
    if !@yazino_config.has_key?(property)
      puts "property #{property} is undefined in config #{@environment}"
      exit 1
    end
    @yazino_config[property]
  end

  def get_password
    property = 'password'
    if !@yazino_config.has_key?(property)
      puts "property #{property} is undefined in config #{@environment}"
      exit 1
    end
    @yazino_config[property]
  end

  def get_play_type
    property = 'playType'
    if !@yazino_config.has_key?(property)
      puts "property #{property} is undefined in config #{@environment}"
      exit 1
    end
    @yazino_config[property]
  end

  def get_platform
    property = 'platform'
    if !@yazino_config.has_key?(property)
      puts "property #{property} is undefined in config #{@environment}"
      exit 1
    end
    @yazino_config[property]
    end

  def get_client_id
    property = 'clientId'
    if !@yazino_config.has_key?(property)
      puts "property #{property} is undefined in config #{@environment}"
      exit 1
    end
    @yazino_config[property]
    end

  def get_variation_name
    property = 'variationName'
    if !@yazino_config.has_key?(property)
      puts "property #{property} is undefined in config #{@environment}"
      exit 1
    end
    @yazino_config[property]
    end

  def get_game_type
    property = 'gameType'
    if !@yazino_config.has_key?(property)
      puts "property #{property} is undefined in config #{@environment}"
      exit 1
    end
    @yazino_config[property]
    end

  def get_bot_type
    @bot_type
  end



  private
  def config_dir
    File.expand_path("#{base_dir}/config")
  end

  def config_file
    "#{config_dir}/config.yaml"
  end

  def base_dir
    File.expand_path("#{File.dirname(__FILE__)}/../..")
  end

  def get_config
    config = YAML.load_file(config_file)
    if !config.has_key?(@bot_type)
      puts "invalid bot_type : #{@bot_type}"
      puts "valid bot_types are : #{config.keys.to_s}"
      exit
    end

    if !config[@bot_type].has_key?(@environment)
      puts "invalid environment : #{@bot_type}"
      puts "valid environments are : #{config[@bot_type].keys.to_s}"
      exit 1
    end
      config[@bot_type][@environment]
  end

end