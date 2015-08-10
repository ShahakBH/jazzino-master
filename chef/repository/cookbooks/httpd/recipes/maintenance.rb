include_recipe "httpd::default"

directory "/var/www/html/maintenance" do
  owner "root"
  group "root"
  mode 0755
  action :create
end

directory "/var/www/html/maintenance/images" do
  owner "root"
  group "root"
  mode 0755
  action :create
end

directory "/var/www/html/command" do
  owner "root"
  group "root"
  mode 0755
  action :create
end

directory "/var/www/html/jetty-monitor" do
  owner "root"
  group "root"
  mode 0755
  action :create
end

template "/etc/httpd/conf.d/maintenance.conf" do
  source "maintenance.conf.erb"
  mode 0644
  owner "root"
  group "root"
  variables(
    :httpd_port => node[:httpd][:port],
    :httpd_ssl_port => node[:httpd][:ssl_port],
    :jetty_port => node[:jetty][:port],
    :jetty_ssl_port => node[:jetty][:ssl_port],
    :ssl_certificate_directory => node[:httpd][:ssl_cert_directory]
  )
end

template "/var/www/html/maintenance/maintenance.html" do
  source "maintenance.html.erb"
  mode 0644
  owner "root"
  group "root"
end

template "/var/www/html/maintenance/oops.html" do
  source "oops.html.erb"
  mode 0644
  owner "root"
  group "root"
end

template "/var/www/html/maintenance/style.css" do
  source "style.css.erb"
  mode 0644
  owner "root"
  group "root"
end

template "/var/www/html/crossdomain.xml" do
  source "crossdomain.xml.erb"
  mode 0644
  owner "root"
  group "root"
end

cookbook_file "/var/www/html/command/loadBalancerStatus" do
  source "loadBalancerStatus"
  mode 0644
  owner "root"
  group "root"
end

cookbook_file "/var/www/html/jetty-monitor/load-balancer" do
  source "loadBalancerStatus"
  mode 0644
  owner "root"
  group "root"
end

cookbook_file "/var/www/html/maintenance/images/body-back.png" do
  source "body-back.png"
  mode 0644
  owner "root"
  group "root"
end

cookbook_file "/var/www/html/maintenance/images/maintenance-medal.png" do
  source "maintenance-medal.png"
  mode 0644
  owner "root"
  group "root"
end

cookbook_file "/var/www/html/maintenance/images/maintenance-umbrella.png" do
  source "maintenance-umbrella.png"
  mode 0644
  owner "root"
  group "root"
end

cookbook_file "/var/www/html/maintenance/maintenance.json" do
  source "maintenance.json"
  mode 0644
  owner "root"
  group "root"
end

execute "selinux_configuration_conf" do
  command "chcon -t httpd_config_t /etc/httpd/conf.d/maintenance.conf"
  action :run
  only_if {system("/usr/sbin/selinuxenabled")}
end

execute "selinux_configuration_content" do
  command "chcon -R -t httpd_sys_content_t /var/www/html/maintenance"
  action :run
  only_if {system("/usr/sbin/selinuxenabled")}
end

ruby_block "http_firewall_rules" do
  block do
    while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':#{node[:httpd][:port]}\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/iptables -D INPUT -p tcp --dport #{node[:httpd][:port]} -j ACCEPT]
      break if $?.exitstatus != 0
    end
    while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':#{node[:httpd][:port]}\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/ip6tables -D INPUT -p tcp --dport #{node[:httpd][:port]} -j ACCEPT]
      break if $?.exitstatus != 0
    end
    %x[/sbin/iptables -I INPUT -p tcp --dport #{node[:httpd][:port]} -j ACCEPT]
    %x[/sbin/service iptables save]
    %x[/sbin/ip6tables -I INPUT -p tcp --dport #{node[:httpd][:port]} -j ACCEPT]
    %x[/sbin/service ip6tables save]
  end
  action :create
  only_if {node[:firewall]}
end

execute "set_httpd_port" do
  command "sed -i -e \"s/Listen 80/Listen #{node[:httpd][:port]}/g\" /etc/httpd/conf/httpd.conf"
  action :run
end

service "httpd" do
  action [:start, :enable, :restart]
end
