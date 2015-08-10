require 'rubygems'
require 'bundler/setup'

require 'rbconfig'
require 'rainbow'

module Yazino

  class VPN

    def initialize(vpn_name, vpn_type)
      @name = vpn_name
      @type = vpn_type
    end

    def connect
      case @type
      when 'cisco-ipsec'
        connect_cisco
      else
        throw "Unknown VPN type: #{@type}"
      end
    end

    def disconnect
      case @type
      when 'cisco_ipsec'
        disconnect_cisco
      else
        throw "Unknown VPN type: #{@type}"
      end
    end

    private

    def check_vpnc_exists
      %x[which vpnc 1>/dev/null 2>&1]
      raise "Cannot find vpnc executable on path" if $?.exitstatus != 0
    end

    def connect_cisco
      check_vpnc_exists

      vpn_out = %x[sudo vpnc #{@name}]
      raise "VPN connection #{@name} failed: #{vpn_out}" if $?.exitstatus != 0
    end

    def disconnect_cisco
      check_vpnc_exists

      vpn_out = %x[sudo vpnc-disconnect]
      raise "VPN disconnection failed: #{vpn_out}" if $?.exitstatus != 0
    end

  end

end
