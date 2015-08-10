include_recipe "haproxy::default"

template "/etc/haproxy/haproxy.cfg" do
  source "loadbalancer_haproxy.cfg.erb"
  mode 0644
  owner "root"
  group "root"
  variables(
    :lobby_listen_ipv4 => node[:haproxy][:lobby][:listen_ipv4],
    :lobby_listen_ipv6 => node[:haproxy][:lobby][:listen_ipv6],
    :lobby_servers => node[:haproxy][:lobby][:servers],
    :mobile_listen_ipv4 => node[:haproxy][:mobile][:listen_ipv4],
    :mobile_listen_ipv6 => node[:haproxy][:mobile][:listen_ipv6],
    :mobile_servers => node[:haproxy][:mobile][:servers],
    :worker_listen_ipv4 => node[:haproxy][:worker][:listen_ipv4],
    :worker_listen_ipv6 => node[:haproxy][:worker][:listen_ipv6],
    :worker_servers => node[:haproxy][:worker][:servers],
    :whitelist => node[:haproxy][:whitelist]
  )
end

execute "start or reload haproxy" do
  command "/etc/init.d/haproxy start && /etc/init.d/haproxy reload"
  action :run
end

ruby_block "haproxy-firewall-rules" do
  block do
    lobby_ports = [80, 443, 843, 8143, 8188]
    mobile_ports = [80, 443, 8090, 8091]
    worker_ports = [7900, 7943]

    ipv4_ports_to_open = {
      node[:haproxy][:lobby][:listen_ipv4] => lobby_ports,
      node[:haproxy][:mobile][:listen_ipv4] => mobile_ports,
      node[:haproxy][:worker][:listen_ipv4] => worker_ports
    }

    ipv4_ports_to_open.each_pair do |ip_address, ports|
      ports.each do |port|
        while %x[/sbin/iptables --list -n | grep ACCEPT | grep #{ip_address} | grep -e ':#{port}\s*$' | wc -l].strip.to_i > 0
          %x[/sbin/iptables -D INPUT -p tcp --dport #{port} -d #{ip_address} -j ACCEPT]
          break if $?.exitstatus != 0
        end
        %x[/sbin/iptables -I INPUT -p tcp --dport #{port} -d #{ip_address} -j ACCEPT]
      end
    end
    %x[/sbin/service iptables save]

    ipv6_ports_to_open = {
      node[:haproxy][:lobby][:listen_ipv6] => lobby_ports,
      node[:haproxy][:mobile][:listen_ipv6] => mobile_ports,
      node[:haproxy][:worker][:listen_ipv6] => worker_ports
    }

    ipv6_ports_to_open.each_pair do |ip_address, ports|
      ports.each do |port|
        while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep #{ip_address} | grep -e ':#{port}\s*$' | wc -l].strip.to_i > 0
          %x[/sbin/ip6tables -D INPUT -p tcp --dport #{port} -d #{ip_address} -j ACCEPT]
          break if $?.exitstatus != 0
        end
        %x[/sbin/ip6tables -I INPUT -p tcp --dport #{port} -d #{ip_address} -j ACCEPT]
      end
    end
    %x[/sbin/service ip6tables save]
  end
  action :create
end
