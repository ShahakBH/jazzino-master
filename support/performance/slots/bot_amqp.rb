#!/usr/bin/env ruby

require 'rubygems'
require 'bundler/setup'

require "httpclient"
require "json"
require "amqp"
require "base64"
require "zlib"
require "thread"

require "sinatra/base"
require "puma/server"

@environment="www.yazino.com"
@user = "rubybot1@yazino.com"
@password = "rubybot"

@http = HTTPClient.new
@connection = nil
@user_context = {:logged_in => false}
$stats = {}
@semaphore = Mutex.new

# @http.ssl_config.verify_mode = OpenSSL::SSL::VERIFY_NONE

def now
  Time.new.to_f
end

def puts(message)
  print "#{message}\n"
end

def puts_with_timestamp(message)
  puts "#{now} #{message}"
end

def update_context(new_context)
  @user_context.update(new_context)
  puts "context = #{@user_context}"
end

def login
  puts "Attempting to login as #{@user}"
  response = @http.post "https://#{@environment}/public/login/ANDROID/SLOTS/YAZINO", {:email => @user, :password => @password}
  puts "Failed to login" && return if response.status_code != 200
  puts "Login OK"
  login_result = JSON.parse(response.body)
  puts "Retrieving config"
  conf_response = @http.get "https://#{@environment}/game-server/command/launchConfiguration"
  puts "Failed to retrieve server configuration" && return if conf_response.status_code != 200
  server_config = JSON.parse(conf_response.body)
  update_context({
                     :logged_in => true,
                     :playerId => login_result["playerId"],
                     :command_url => server_config["commandUrl"],
                     :amqp_host => server_config["amqpHost"],
                     :amqp_virtual_host => server_config["amqpVirtualHost"],
                     :table_id => nil,
                     :game_id => nil,
                     :playing => false,
                     :allowed_actions => [],
                     :betting => false,
                     :game_phase => nil,
                     :last_bet_ts => now - 10,
                     :can_bet => false
                 })
end

def find_table
  puts "Finding table"
  response = @http.post "https://#{@environment}/lobbyCommand/mobile/tableLocator", {:gameType => "SLOTS", :clientId => "Default Slots", :variationName => "Slots Low"}
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
  puts_with_timestamp "Sending command: #{command}"
  args_flat = args.join("|")
  response = @http.post "#{@user_context[:command_url]}/giga", "#{@user_context[:table_id]}|-1|#{command}|#{args_flat}"
  puts "Failed to send command" && return if response.status_code != 200 || response.body != "OK"
end

def parse_game_status(message)
  puts_with_timestamp message
  changes = message["changes"]
  lines = changes.split("\n")
  new_changes = []
  lines[(5..lines.length)].each do |line|
    details = line.split("\t")
    new_changes << details[1] if details[0].to_i > @user_context[:increment]
  end
  new_context = {
      :game_id => message["gameId"],
      :playing => message["isAPlayer"] == "true",
      :increment => lines[2].to_i,
      :allowed_actions => lines[3].split("\t")
  }
  if !@user_context[:command_result] || new_changes.include?("BetPlaced")
    latency = now - @user_context[:last_bet_ts]
    puts_with_timestamp "Latency = #{latency}"
    $stats[:last_command_latency] = (latency * 1000).round(3)
    new_context[:command_result] = true,
        new_context[:can_bet] = true
  end
  update_context(new_context)
end

def print_error(message)
  error = message["message"]["message"]
  puts_with_timestamp "Error = #{error}"
end

def attempt_bet
  if @user_context[:can_bet] && @user_context[:allowed_actions].include?("Bet") && (now - @user_context[:last_bet_ts] > 5)
    update_context({
                       :last_bet_ts => now,
                       :can_bet => false,
                       :command_result => false
                   })
    send_command "Bet", ["1", "10"]
  end
end

def handle_next_message(message_type, message)
  # puts "Received a message[#{message_type}]: #{message}."
  print_error(message) && return if message_type == "ERROR"
  puts_with_timestamp "Unrecognised message #{message_type}" && return if !["INITIAL_GAME_STATUS", "GAME_STATUS"].include? message_type
  parse_game_status message
end

def join_table
  puts "Connecting to #{@user_context[:amqp_host]}"
  @connection.close if !@connection.nil?
  @connection = AMQP.connect(:host => @user_context[:amqp_host], :vhost => @user_context[:amqp_virtual_host], :user => "flash", :pass => "readonly")
  channel = AMQP::Channel.new(@connection)
  queue = channel.queue("", :auto_delete => true, :exclusive => true)
  queue.bind("player-direct", {:routing_key => "PLAYERTABLE.#{@user_context[:playerId]}.#{@user_context[:table_id]}"})
  queue.bind("player-direct", {:routing_key => "PLAYER.#{@user_context[:playerId]}"})
  puts "Waiting for game messages"
  queue.subscribe do |metadata, payload|
    raw_message = decode payload, metadata.attributes[:content_encoding]
    json_message = JSON.parse(raw_message)
    @semaphore.synchronize do
      handle_next_message metadata.content_type, json_message
    end
  end
  update_context({:increment => -1, :can_bet => true})
  EventMachine.add_timer(2) do
    @semaphore.synchronize do
      send_command "InitialGetStatus"
    end
  end
end

def play
  begin
    puts_with_timestamp "<play>"
    if !@user_context[:logged_in]
      login
      return
    end
    if !@user_context[:table_id]
      find_table
      join_table
      return
    end
    if !@user_context[:playing] && @user_context[:allowed_actions].include?("Join")
      send_command "Join"
      return
    end
    if now - @user_context[:last_bet_ts] > 60
      puts "TIMEOUT"
      update_context({
                         :logged_in => false,
                         :can_bet => false
                     })
    end
    attempt_bet
  rescue => e
    puts_with_timestamp "Play error! Resetting... #{e}"
    update_context({ :logged_in => false, :can_bet => false });
  end
end

def ping
  begin
    res = %x(ping -c 1 -t 60 #{@environment})
    match = res.match(/time=(.*)ms/)
    puts_with_timestamp "Ping Unknown: #{res}" && return if match.nil?
    ping = match.captures.first.to_f
    puts_with_timestamp "Ping = #{ping}"
    $stats[:last_ping] = ping
  rescue => e
    puts_with_timestamp "Ping error"
  end
end

class Web < Sinatra::Base
  configure {
    set :server, :puma
    set :bind, '0.0.0.0'
    set :port, 3000
  }
  get '/' do
    $stats.to_json
  end
end

Thread.new do
  Web.run!
end

EventMachine.run do
  EventMachine.add_periodic_timer(1) do
    @semaphore.synchronize do
      play
    end
  end
  EventMachine.add_periodic_timer(1) do
    ping
  end
  # Thin::Server.start Web, '0.0.0.0', 3000
  # cli = Puma::CLI.new ["-p", "3000"]
  # cli.run
# server = Puma::Server.new Web, Puma::Events.stdio, {:Port => 3000}
# server.run
end

