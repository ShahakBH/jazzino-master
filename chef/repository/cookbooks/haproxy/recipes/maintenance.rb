include_recipe "haproxy::default"

template "/etc/haproxy/haproxy.cfg" do
  source "maintenance_haproxy.cfg.erb"
  mode 0644
  owner "root"
  group "root"
  variables(
    :http_source_port => node[:haproxy][:maintenance][:http][:port],
    :http_redirect_port => node[:haproxy][:maintenance][:http][:redirect_port],
    :http_whitelist_port => node[:haproxy][:maintenance][:http][:whitelist_port],
    :https_source_port => node[:haproxy][:maintenance][:https][:port],
    :https_redirect_port => node[:haproxy][:maintenance][:https][:redirect_port],
    :https_whitelist_port => node[:haproxy][:maintenance][:https][:whitelist_port],
    :whitelist => node[:haproxy][:whitelist]
  )
end

execute "start or reload haproxy" do
  command "/etc/init.d/haproxy start && /etc/init.d/haproxy reload"
  action :run
end

ruby_block "haproxy-maintenance-firewall-rules" do
  block do
    ports = [node[:haproxy][:maintenance][:http][:port], [:haproxy][:maintenance][:https][:port]]

    ports.each do |port|
      while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':#{port}\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/iptables -D INPUT -p tcp --dport #{port} -j ACCEPT]
        break if $?.exitstatus != 0
      end
      %x[/sbin/iptables -I INPUT -p tcp --dport #{port} -j ACCEPT]
    end
    %x[/sbin/service iptables save]

    ports.each do |port|
      while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':#{port}\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/ip6tables -D INPUT -p tcp --dport #{port} -j ACCEPT]
        break if $?.exitstatus != 0
      end
      %x[/sbin/ip6tables -I INPUT -p tcp --dport #{port} -j ACCEPT]
    end
    %x[/sbin/service ip6tables save]
  end

  action :create
  only_if {node[:firewall]}
end
