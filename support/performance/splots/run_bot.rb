#!/usr/bin/env jruby

require 'java'
require 'lib/ls-client.jar'

require 'rubygems'
require 'bundler/setup'

require "httpclient"
require "json"
require "base64"
require "zlib"
require "thread"
require 'eventmachine'
require 'optparse'

require 'sinatra/base'
require 'puma/server'


require 'lib/bots/splots.rb'
require 'lib/yazino/config.rb'

java_import com.lightstreamer.ls_client.ConnectionInfo
java_import com.lightstreamer.ls_client.ExtendedConnectionListener
java_import com.lightstreamer.ls_client.ExtendedTableInfo
java_import com.lightstreamer.ls_client.HandyTableListener
java_import com.lightstreamer.ls_client.LSClient

java_import java.net.CookieHandler
java_import java.net.CookieManager
java_import java.net.CookiePolicy

require 'lib/yazino/bot_connection_listener.rb'
require 'lib/yazino/web.rb'
require 'lib/yazino/lightstreamer_listener.rb'

options = {}
OptionParser.new do |opts|
  options[:environment] = "breakmycasino"
  options[:bot_type] = "normal_play"
  opts.banner = "Usage: run_bot.rb [options]"
  opts.on('-e', '--name bot name', 'dseeto-centos') { |v| options[:environment] = v }
  opts.on('-b', '--bot bot type', 'SPLOTS BLACKJACK') { |v| options[:bot_type] = v }
end.parse!

CONFIG = YazinoConfig.new(options[:environment])
ENVIRONMENT = CONFIG.get_server
USER = CONFIG.get_user
PASSWORD = CONFIG.get_password
BOT_TYPE = options[:bot_type]

$http = HTTPClient.new
$connection = nil
$user_context = {:logged_in => false}
$stats = {}
$semaphore = Mutex.new

$http.ssl_config.verify_mode = OpenSSL::SSL::VERIFY_NONE

def now
  Time.new.to_f
end

def puts(message)
  puts_with_timestamp "#{message}\n"
end

def puts_with_timestamp(message)
  Kernel.puts "#{now} #{message}"
end

def update_context(new_context)
  $user_context.update(new_context)
  puts "context = #{$user_context}"
end

def login
  puts "Attempting to login as #{USER}"
  login_url = "https://#{ENVIRONMENT}/public/login/#{CONFIG.get_platform}/#{CONFIG.get_game_type}/YAZINO"
  puts "with login url #{login_url}"
  response = $http.post login_url, {:email => USER, :password => PASSWORD}
  puts "Failed to login" && return if response.status_code != 200
  puts "Login OK"
  JSON.parse(response.body)
  puts "Retrieving config"
  conf_response = $http.get "https://#{ENVIRONMENT}/api/1.0/configuration/#{CONFIG.get_platform}/#{CONFIG.get_game_type}"
  puts "Failed to retrieve server configuration" && return if conf_response.status_code != 200
  server_config = JSON.parse(conf_response.body)
  update_context({
     :logged_in => true,
     :playerId => server_config["player-id"],
     :command_url => server_config["command-url"],
     :lightstreamer_server => "#{server_config['lightstreamer-protocol']}://#{server_config['lightstreamer-server']}:#{server_config['lightstreamer-port']}",
     :table_id => nil,
     :game_id => nil,
     :playing => false,
     :allowed_actions => [],
     :betting => false,
     :game_phase => nil,
     :last_bet_ts => now - 10,
     :last_collect_ts => now - 10,
     :can_bet => false,
     :has_gambled => false,
     :has_collected => false,
     :line_options => [1],
     :bet_options => [1],
 })
end

def find_table
  puts "Finding table"
  response = $http.get "https://#{ENVIRONMENT}/api/1.0/private_table", {:gameType => "#{CONFIG.get_game_type}", :clientId => "#{CONFIG.get_client_id}", :variationName => "#{CONFIG.get_variation_name}"}
  puts "Failed to find table" && return if response.status_code != 200
  update_context({:table_id => JSON.parse(response.body)["tableId"]})
end

def decode(payload, content_encoding)
  if content_encoding != 'DEF'
    return payload
  end
  compressed_message = Base64.decode64(payload)
  zstream = Zlib::Inflate.new
  message = zstream.inflate(compressed_message)
  zstream.finish
  zstream.close
  message
end

def send_command(command, args = [])
  puts_with_timestamp "Sending command: #{command} #{args}"
  args_flat = args.join("|")
  response = $http.post "https://#{ENVIRONMENT}/game-server/command/giga", "#{$user_context[:table_id]}|-1|#{command}|#{args_flat}\n#{$user_context[:table_id]}|-1|ACK|#{$user_context[:increment]}"
  puts "Failed to send command" && return if response.status_code != 200 || response.body != "OK"
end

def parse_game_status(message)
  module_name = CONFIG.get_bot_type

  if Object.const_defined?(module_name)
    required_module = Kernel.const_get(module_name)
    if required_module.respond_to?("parse_game_status") then
      update_context(required_module.parse_game_status(message))
    else
      puts "module #{module_name} does not have method parse_game_status implemented'"
    end
  else
    puts "Invalid module '#{module_name}'"
  end
end

def print_error(message)
  error = message["message"]["message"]
  puts_with_timestamp "Error = #{error}"
end

def play_game
  module_name = CONFIG.get_bot_type
  method_name = CONFIG.get_play_type

  if Object.const_defined?(module_name)
    required_module = Kernel.const_get(module_name)
    if required_module.respond_to?(method_name) then
      required_method = required_module.method(method_name)
      update_context(required_method.call($user_context))
    else
      puts "Invalid method '#{method_name}' for module '#{module_name}'"
    end
  else
    puts "Invalid module '#{module_name}'"
  end
  rescue => e
    puts_with_timestamp "Play Game! ... #{e}"

end

def handle_next_message(message_type, message)
  print_error(message) && return if message_type == "ERROR"
  puts_with_timestamp "Unrecognised message #{message_type}" && return if !["INITIAL_GAME_STATUS", "GAME_STATUS"].include? message_type
  parse_game_status message
end

def join_table
  puts "Connecting to #{$user_context[:lightstreamer_server]}"
  $connection.closeConnection if !$connection.nil?

  CookieHandler::setDefault(CookieManager.new(nil, CookiePolicy::ACCEPT_ALL)) # require for LS load balancing

  connection_info = ConnectionInfo.new
  connection_info.pushServerUrl = $user_context[:lightstreamer_server]
  connection_info.adapter = 'STRATA'

  $connection = LSClient.new
  $connection.openConnection(connection_info, BotConnectionListener.new)

  puts "Waiting for game messages"

  subscriptions = ["PLAYERTABLE.#{$user_context[:playerId]}.#{$user_context[:table_id]}", "PLAYER.#{$user_context[:playerId]}"]
  puts_with_timestamp "Subscribing to #{subscriptions}"
  table_info = ExtendedTableInfo.new subscriptions, ExtendedTableInfo::RAW, ["contentType", "body"], false
  table_info.setDataAdapter "TABLE"
  $connection.subscribeItems table_info, LightstreamerListener.new

  update_context({:increment => -1, :can_bet => true, :can_collect => true})
  EventMachine.add_timer(2) do
    $semaphore.synchronize do
      send_command "InitialGetStatus"
    end
  end
end

def play
  begin
    puts_with_timestamp "<play>"
    if !$user_context[:logged_in]
      puts_with_timestamp "<login>"
      login
      return
    end
    if !$user_context[:table_id]
      puts_with_timestamp "<find table>"
      find_table
      puts_with_timestamp "<Join Table>"
      join_table
      return
    end
    if !$user_context[:playing] && $user_context[:allowed_actions].include?("Join")
      puts_with_timestamp "<Join>"
      send_command "Join"
      return
    end
    if now - $user_context[:last_bet_ts] > 60
      puts "TIMEOUT"
      update_context({
                         :logged_in => false,
                         :can_bet => false
                     })
    end
    puts_with_timestamp "<attempt Bet>"
    play_game
  rescue => e
    puts_with_timestamp "Play error! Resetting... #{e}"
    update_context({ :logged_in => false, :can_bet => false });
  end
end


Thread.new do
  Web.run!
end

EventMachine.run do
  EventMachine.add_periodic_timer(1) do
    $semaphore.synchronize do
      play
    end
  end
  EventMachine.add_periodic_timer(1) do
  end
end

