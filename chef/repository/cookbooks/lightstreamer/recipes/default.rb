# in order to downgrade you need to specify package as yum_package because allow_downgrade is a yum atttribute and not found on package

# yum_package do
# allow_downgrade true
# ...
# end

package "lightstreamer" do
  version "#{node[:lightstreamer][:version]}-1"
  action :install
end

include_recipe "lightstreamer::server-#{node[:lightstreamer][:short_version]}"

mac_addresses = [
]

mac_addresses.each do |mac_address|
  cookbook_file "/opt/lightstreamer/conf/Yazino_#{mac_address}_Allegro.lic" do
    source "#{node[:lightstreamer][:short_version]}/Yazino_#{mac_address}_Allegro.lic"
    mode 0755
    owner node[:lightstreamer][:user]
    group node[:lightstreamer][:group]
  end
end

cookbook_file "/opt/lightstreamer/pages/crossdomain.xml" do
  source "#{node[:lightstreamer][:short_version]}/crossdomain.xml"
  mode 0644
  owner node[:lightstreamer][:user]
  group node[:lightstreamer][:group]
end

cookbook_file "/opt/lightstreamer/pages/index.html" do
  source "#{node[:lightstreamer][:short_version]}/index.html"
  mode 0644
  owner node[:lightstreamer][:user]
  group node[:lightstreamer][:group]
end

directory "/opt/lightstreamer/adapters" do
  recursive true
  action :delete
end

directory "/opt/lightstreamer/pages/demos" do
  recursive true
  action :delete
end

link "/opt/lightstreamer/adapters" do
  to "/opt/lightstreamer-adapter/current"
  action :create
  only_if "test -L /opt/lightstreamer-adapter/current"
end

execute "clean-ls-lock" do
  command "rm -f /var/run/lightstreamer.pid"
  only_if {File.exists?('/etc/init.d/lightstreamer') && %x(service lightstreamer status) =~ /lock file found but no process running/}
end

execute "stop-lightstreamer" do
  command "service lightstreamer stop || true"
  only_if {%x(service lightstreamer status) !~ /is stopped/}
end

execute "start-lightstreamer" do
  command "service lightstreamer start"
end

service "lightstreamer" do
  action [:enable]
end

ruby_block "lightstreamer-firewall-rules" do
  block do
    while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':#{node[:lightstreamer][:http_port]}\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/iptables -D INPUT -p tcp --dport #{node[:lightstreamer][:http_port]} -j ACCEPT]
      break if $?.exitstatus != 0
    end
    while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':#{node[:lightstreamer][:http_port]}\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/ip6tables -D INPUT -p tcp --dport #{node[:lightstreamer][:http_port]} -j ACCEPT]
      break if $?.exitstatus != 0
    end
    while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':#{node[:lightstreamer][:https_port]}\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/iptables -D INPUT -p tcp --dport #{node[:lightstreamer][:https_port]} -j ACCEPT]
      break if $?.exitstatus != 0
    end
    while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':#{node[:lightstreamer][:https_port]}\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/ip6tables -D INPUT -p tcp --dport #{node[:lightstreamer][:https_port]} -j ACCEPT]
      break if $?.exitstatus != 0
    end
    %x[/sbin/iptables -I INPUT -p tcp --dport #{node[:lightstreamer][:http_port]} -j ACCEPT]
    %x[/sbin/iptables -I INPUT -p tcp --dport #{node[:lightstreamer][:https_port]} -j ACCEPT]
    %x[/sbin/service iptables save]
    %x[/sbin/ip6tables -I INPUT -p tcp --dport #{node[:lightstreamer][:http_port]} -j ACCEPT]
    %x[/sbin/ip6tables -I INPUT -p tcp --dport #{node[:lightstreamer][:https_port]} -j ACCEPT]
    %x[/sbin/service ip6tables save]
  end
  action :create
  only_if {node[:firewall]}
end

