require 'rubygems'
require 'bundler/setup'

require 'json'
require 'rest_client'
require 'open-uri'

class Pingdom

  def initialize(config)
    raise "No Pingdom configuration present" if !config['pingdom']

    @app_key = config['pingdom']['app-key']
    @username = config['pingdom']['username']
    @password = config['pingdom']['password']
  end

  def suspend
    puts "* Suspending Pingdom checks"
    list_checks.each {|check| suspend_check(check['id'])}
  end

  def resume
    puts "* Resuming Pingdom checks"
    list_checks.each {|check| resume_check(check['id'])}
  end

  private

  def list_checks
    JSON.parse(api_resource('/checks').get('App-Key' => @app_key))['checks']
  end

  def suspend_check(check_id)
    api_resource("/checks/#{check_id}").put('paused=true', 'App-Key' => @app_key)
  end

  def resume_check(check_id)
    api_resource("/checks/#{check_id}").put('paused=false', 'App-Key' => @app_key)
  end

  def api_resource(service)
    RestClient::Resource.new("https://api.pingdom.com/api/2.0/#{service}", @username, @password)
  end

end
