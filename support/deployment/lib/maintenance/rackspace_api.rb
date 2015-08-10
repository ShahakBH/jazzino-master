require 'rubygems'
require 'bundler/setup'

require 'json'
require 'rest_client'

module Yazino

  class RackSpaceApi

    def initialize(username, api_key, region = nil)

      @username = username
      @api_key = api_key
      @region = region || DEFAULT_REGION
      @endpoints = {}
    end

    def authenticate
      parsed_response = JSON.parse(RestClient.post "#{RACKSPACE_API}/tokens", {:auth => { 'RAX-KSKEY:apiKeyCredentials' => {:username => @username, :apiKey => @api_key}}}.to_json, :content_type => :json, :accept => :json)

      @endpoints = {
        :loadbalancer => parsed_response['access']['serviceCatalog'].keep_if{|entry| entry['name'] == 'cloudLoadBalancers'}[0]['endpoints'].keep_if {|entry| entry['region'] == @region}[0]['publicURL']
      }

      @token = parsed_response['access']['token']['id']
    end

    def loadbalancers
      raise "Not authenticated" unless @token

      JSON.parse(RestClient.get "#{@endpoints[:loadbalancer]}/loadbalancers", :accept => :json, 'X-Auth-Token' => @token)["loadBalancers"].collect do |balancer|
        {
          :id => balancer['id'],
          :name => balancer['name'],
          :port => balancer['port'],
          :protocol => balancer['protocol'],
          :status => balancer['status'],
          :public => !balancer['virtualIps'].keep_if {|ip| ip['type'] == 'PUBLIC'}.empty?
        }
      end
    end

    def nodes(loadbalancer_id)
      raise "Not authenticated" unless @token

      JSON.parse(RestClient.get "#{@endpoints[:loadbalancer]}/loadbalancers/#{loadbalancer_id}/nodes", :accept => :json, 'X-Auth-Token' => @token)["nodes"].collect do |node|
        {
          :id => node['id'],
          :status => node['status'],
          :address => node['address'],
          :port => node['port'],
          :condition => node['condition'],
          :weight => node['weight'],
          :type => node['type']
        }
      end
    end

    def add_nodes(loadbalancer_id, nodes)
      raise "Not authenticated" unless @token

      send { RestClient.post "#{@endpoints[:loadbalancer]}/loadbalancers/#{loadbalancer_id}/nodes", {:nodes => nodes}.to_json, :content_type => :json, :accept => :json, 'X-Auth-Token' => @token }
    end

    def delete_nodes(loadbalancer_id, node_ids = [])
      raise "Not authenticated" unless @token

      unless node_ids.empty?
        send { RestClient.delete "#{@endpoints[:loadbalancer]}/loadbalancers/#{loadbalancer_id}/nodes?id=#{node_ids.join("&id=")}", :content_type => :json, :accept => :json, 'X-Auth-Token' => @token }
      end
    end

    def delete_access_lists(loadbalancer_id)
      raise "Not authenticated" unless @token

      send { RestClient.delete "#{@endpoints[:loadbalancer]}/loadbalancers/#{loadbalancer_id}/accesslist", :content_type => :json, :accept => :json, 'X-Auth-Token' => @token }
    end

    def access_lists(loadbalancer_id)
      raise "Not authenticated" unless @token

      response = send { RestClient.get "#{@endpoints[:loadbalancer]}/loadbalancers/#{loadbalancer_id}/accesslist", :content_type => :json, :accept => :json, 'X-Auth-Token' => @token }
      JSON.parse(response)['accessList'].collect do |access_list|
        {
          :id => access_list['id'],
          :address => access_list['address'],
          :type => access_list['type']
        }
      end
    end

    def set_access_lists(loadbalancer_id, access_lists)
      raise "Not authenticated" unless @token

      send { RestClient.post "#{@endpoints[:loadbalancer]}/loadbalancers/#{loadbalancer_id}/accesslist", {:accessList => access_lists}.to_json, :content_type => :json, :accept => :json, 'X-Auth-Token' => @token }
    end

    def set_error_page(loadbalancer_id, page_content)
      raise "Not authenticated" unless @token

      send { RestClient.put "#{@endpoints[:loadbalancer]}/loadbalancers/#{loadbalancer_id}/errorpage", {'errorpage' => {'content' => "\n" + page_content}}.to_json, :content_type => :json, :accept => :json, 'X-Auth-Token' => @token }
    end

    private

    RACKSPACE_API = 'https://identity.api.rackspacecloud.com/v2.0'
    DEFAULT_REGION = 'IAD'
    MAX_TRIES = 10

    def send
      raise "send requires a block" if !block_given?

      response = nil
      tries = 0

      while !response && tries < MAX_TRIES
        tries += 1
        begin
          response = yield
        rescue => e
          if tries < MAX_TRIES && e.response.code == 422
            response = nil
            sleep(3)
          else
            raise e
          end
        end
      end

      response
    end

  end


end
