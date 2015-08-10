require 'ssh'

require 'maintenance/rackspace_api'
require 'threaded_task'

class WebMaintenance
  include ThreadedTask

  def initialize(config)
    @config = config

    @haproxy_config_file = '/etc/haproxy/haproxy.cfg'
    @enable_match = /^(\s*)#(.* unless maintenance_whitelist$)/
    @disable_match = /^(\s*)([^\s#].* unless maintenance_whitelist)$/
    @roles = ['loadbalancer']
    @error = false

    if config.has?('maintenance')
      maintenance = config['maintenance']
      @haproxy_config_file = maintenance['haproxy_config_file'] if maintenance.has_key?('haproxy_config_file')
      @roles = maintenance['roles'] if maintenance.has_key?('roles')
    end
  end

  def on
    loadbalancer_type = @config['loadbalancer_type'] || 'haproxy'
    case loadbalancer_type
    when 'haproxy'
      on_for_haproxy
    when 'rackspace_cloud'
      on_for_rackspace_cloud
    else
      raise "Unknown loadbalancer type: #{loadbalancer_type}"
    end
  end

  def off
    loadbalancer_type = @config['loadbalancer_type'] || 'haproxy'
    case loadbalancer_type
    when 'haproxy'
      off_for_haproxy
    when 'rackspace_cloud'
      off_for_rackspace_cloud
    else
      raise "Unknown loadbalancer type: #{loadbalancer_type}"
    end
  end

  private

  TIMEOUT = 60
  JETTY_HTTP_PORT = 7900
  JETTY_HTTPS_PORT = 7943
  MAINTENANCE_HTTP_PORT = 9180
  MAINTENANCE_HTTPS_PORT = 9190
  MAINTENANCE_HTML = '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"><html xmlns="http://www.w3.org/1999/xhtml"><head><meta http-equiv="content-type" content="text/html; charset=utf-8" /><title>Yazino</title><link rel="stylesheet" href="https://ne1.wac.edgecastcdn.net/002F91/maintenance/style.css" type="text/css" /></head><body id="maintenance-medal"><div class="body"><span class="sp1">Our site is down for</span><span class="sp2">maintenance</span><span class="sp3">while we make some updates to our games</span><span class="sp4">Please come back &amp; play soon</span><span class="sp5">Questions? Drop us a line at <a href="mailto:contact@yazino.com">contact@yazino.com</a></span><span class="sp6">&nbsp;</span></div></body></html>'
  LOADBALANCER_HTML = '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd"><html xmlns="http://www.w3.org/1999/xhtml"><head><meta http-equiv="content-type" content="text/html; charset=utf-8" /><title>Yazino</title><link rel="stylesheet" href="https://ne1.wac.edgecastcdn.net/002F91/maintenance/style.css" type="text/css" /></head><body id="maintenance-medal"><div class="body"><span class="sp1">We&rsquo;re having a</span><span class="sp2">kerfuffle</span><span class="sp3">and can&rsquo;t talk to our web servers</span><span class="sp4">We&rsquo;re working to fix it!</span><span class="sp5">Questions? Drop us a line at <a href="mailto:contact@yazino.com">contact@yazino.com</a></span><span class="sp6">&nbsp;</span></div></body></html>'

  def on_for_rackspace_cloud
    @rackspace_api = Yazino::RackSpaceApi.new(@config['rackspace']['username'], @config['rackspace']['api_key'], @config['rackspace']['loadbalancer_region'])
    @rackspace_api.authenticate

    nodes_to_delete = {}
    nodes_to_update = {}

    @rackspace_api.loadbalancers.keep_if {|balancer| balancer[:public]}.each do |balancer|
      # This is all a big hack to get around the error page for access list violations
      # being unmodifiable. Thus we can't use access lists on web loadbalancers without
      # displayed a very unfriendly message to customers.

      if balancer[:name] =~ /\[M\]/
        existing_nodes = @rackspace_api.nodes(balancer[:id])
        if existing_nodes.count {|node| node[:port] == MAINTENANCE_HTTP_PORT || node[:port] == MAINTENANCE_HTTPS_PORT} > 0
          puts "* Maintenance mode already active for balancer #{balancer[:name]}"
        else
          puts "* Activating maintenance page for balancer #{balancer[:name]}"

          nodes_to_update[balancer] = update_nodes_with_ports(balancer, existing_nodes, MAINTENANCE_HTTP_PORT, MAINTENANCE_HTTPS_PORT)
          nodes_to_delete[balancer] = existing_nodes.collect {|node| node[:id]}

          @rackspace_api.set_error_page(balancer[:id], MAINTENANCE_HTML)
        end

      elsif balancer[:name] !~ /\[I\]/
        existing_access_lists = @rackspace_api.access_lists(balancer[:id])
        unless existing_access_lists.empty?
          puts "* Maintenance mode already active for balancer #{balancer[:name]}"
        else
          puts "* Turning on maintenance mode for balancer #{balancer[:name]}"

          access_lists = [{:address => '0.0.0.0/0', :type => 'DENY'}]
          @config['rackspace']['maintenance_whitelist'].each do |whitelist_netmask|
            access_lists << {:address => whitelist_netmask, :type => 'ALLOW'}
          end
          @rackspace_api.set_access_lists(balancer[:id], access_lists)
        end
      end
    end

    nodes_to_delete.each_pair do |balancer, node_ids|
      puts "* Removing default nodes for balancer #{balancer[:name]}"
      @rackspace_api.delete_nodes(balancer[:id], node_ids)
    end

    nodes_to_update.each_pair do |balancer, nodes|
      puts "* Add maintenance nodes for balancer #{balancer[:name]}"
      @rackspace_api.add_nodes(balancer[:id], nodes)
    end
  end

  def off_for_rackspace_cloud
    unless @rackspace_api
      @rackspace_api = Yazino::RackSpaceApi.new(@config['rackspace']['username'], @config['rackspace']['api_key'], @config['rackspace']['loadbalancer_region'])
      @rackspace_api.authenticate
    end

    add_error_pages = []
    nodes_to_update = {}

    @rackspace_api.loadbalancers.keep_if {|balancer| balancer[:public]}.each do |balancer|
      if balancer[:name] =~ /\[M\]/
        existing_nodes = @rackspace_api.nodes(balancer[:id])
        if existing_nodes.count {|node| node[:port] == MAINTENANCE_HTTP_PORT || node[:port] == MAINTENANCE_HTTPS_PORT} > 0
          puts "* Deleting maintenance nodes for balancer #{balancer[:name]}"

          nodes_to_update[balancer] = update_nodes_with_ports(balancer, existing_nodes, JETTY_HTTP_PORT, JETTY_HTTPS_PORT)
          @rackspace_api.delete_nodes(balancer[:id], existing_nodes.collect {|node| node[:id]})

          add_error_pages << balancer
        else
          puts "* Maintenance mode already inactive for balancer #{balancer[:name]}"
        end

      elsif balancer[:name] !~ /\[I\]/
        existing_access_lists = @rackspace_api.access_lists(balancer[:id])
        if existing_access_lists.empty?
          puts "* Maintenance mode already inactive for balancer #{balancer[:name]}"
        else
          puts "* Turning off maintenance mode for balancer #{balancer[:name]}"

          @rackspace_api.delete_access_lists(balancer[:id])
        end
      end
    end

    nodes_to_update.each_pair do |balancer, nodes|
      puts "* Restoring default nodes for balancer #{balancer[:name]}"
      @rackspace_api.add_nodes(balancer[:id], nodes)
    end

    add_error_pages.each do |balancer|
      puts "* Restoring default error page for balancer #{balancer[:name]}"
      @rackspace_api.set_error_page(balancer[:id], LOADBALANCER_HTML)
    end
  end

  def update_nodes_with_ports(balancer, nodes, http_port, https_port)
    nodes.collect do |node|
      {
        :address => node[:address],
        :port => if balancer[:port] == 80 then http_port else https_port end,
        :condition => node[:condition],
        :weight => node[:weight],
        :type => node[:type]
      }
    end
  end

  def on_for_haproxy
    print "DEBUG: WebMaintenance: Applying maintenance to roles #{@roles}: #{@config.hosts_for_roles(@roles)}\n".foreground(:green) if @config.debug?

    threads = []

    @config.hosts_for_roles(@roles).each do |host|
      threads << Thread.new do
        Yazino::SSH.start(host, @config.ssh_options) do |ssh|
          if !active?(ssh)
            if @error
              print "* Turning error mode on for #{host}\n"
              set_page_to('oops', ssh)
            else
              print "* Turning maintenance mode on for #{host}\n"
              set_page_to('maintenance', ssh)
            end

            change_maintenance_mode(ssh, true)

          else
            print "* Maintenance mode is already on for #{host}\n"
          end
        end
      end
    end

    wait_for_threads("Maintenance activation", TIMEOUT, threads)
  end

  def off_for_haproxy
    print "DEBUG: WebMaintenance: Removing maintenance from roles #{@roles}: #{@config.hosts_for_roles(@roles)}\n".foreground(:green) if @config.debug?

    threads = []

    @config.hosts_for_roles(@roles).each do |host|
      threads << Thread.new do
        Yazino::SSH.start(host, @config.ssh_options) do |ssh|
          if active?(ssh)
            print "* Turning maintenance mode off for #{host}\n"

            change_maintenance_mode(ssh, false)

          else
            print "* Maintenance mode is already off for #{host}\n"
          end
        end
      end
    end

    wait_for_threads("Maintenance deactivation", TIMEOUT, threads)
  end

  def change_maintenance_mode(ssh, enable)
    service_restart = "sudo /sbin/service haproxy reload"
    if enable
      ssh.exec("if [ -f #{@haproxy_config_file} ]; then sudo sed -i'' -e 's%^\\([ \\t]*\\)#\\(.* maintenance_whitelist\\)$%\\1\\2%g\' #{@haproxy_config_file} && #{service_restart}; fi")
    else
      ssh.exec("if [ -f #{@haproxy_config_file} ]; then sudo sed -i'' -e 's%^\\([ \\t]*\\)\\([^ \\t#].* maintenance_whitelist\\)$%\\1#\\2%g\' #{@haproxy_config_file} && #{service_restart}; fi")
    end
  end

  def set_page_to(page, ssh)
    ssh.exec("if [ -f '#{@base}/index.html' ]; then sudo rm #{@base}/index.html; fi && sudo ln -sf #{@base}/#{page}.html #{@base}/index.html")
  end

  def active?(ssh)
    ssh.exec("sudo grep -c '^[\t ]*#.* maintenance_whitelist$' #{@haproxy_config_file}; true").strip == '0'
  end

end
