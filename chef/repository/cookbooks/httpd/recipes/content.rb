include_recipe "httpd::default"

cookbook_file "#{node[:httpd][:content][:directory]}/crossdomain.xml" do
  source "crossdomain.xml"
  mode 0644
  owner "apache"
  group "apache"
end

cookbook_file "#{node[:httpd][:content][:directory]}/available" do
  source "available"
  mode 0644
  owner "apache"
  group "apache"
end

template "/etc/httpd/conf.d/content.conf" do
  source "content.conf.erb"
  mode 0644
  owner "root"
  group "root"
  variables(
    :content_port => node[:httpd][:content][:port],
    :content_ssl_port => node[:httpd][:content][:ssl_port],
    :content_directory => node[:httpd][:content][:directory],
    :ssl_certificate_directory => node[:httpd][:content][:ssl_cert_directory]
  )
end

directory "/var/avatars" do
  owner "root"
  group "contentuser"
  mode 0775
  action :create
  not_if {File.exists?('/var/avatars')}
end

link "#{node[:httpd][:content][:directory]}/avatars" do
  to "/var/avatars"
end

directory "/var/marketing" do
  owner "root"
  group "contentuser"
  mode 0775
  action :create
  not_if {File.exists?('/var/marketing')}
end

directory "/var/yazino-games" do
  owner "root"
  group "contentuser"
  mode 0775
  action :create
  not_if {File.exists?('/var/yazino-games')}
end

execute "set_httpd_port" do
  command "sed -i -e \"s/Listen 80/Listen #{node[:httpd][:port]}/g\" /etc/httpd/conf/httpd.conf"
  action :run
  only_if {node[:httpd][:port]}
end

execute "selinux_configuration_conf" do
  command "chcon -t httpd_config_t /etc/httpd/conf.d/content.conf"
  action :run
  only_if {system("/usr/sbin/selinuxenabled")}
end

execute "selinux_configuration_content" do
  command "chcon -R -t httpd_sys_content_t /home/content"
  action :run
  only_if {system("/usr/sbin/selinuxenabled")}
end

ruby_block "http-firewall-rules" do
  block do
    [node[:httpd][:content][:port], node[:httpd][:content][:ssl_port]].each do |port|
      while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':#{port}\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/iptables -D INPUT -p tcp --dport #{port} -j ACCEPT]
        break if $?.exitstatus != 0
      end
      while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':#{port}\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/ip6tables -D INPUT -p tcp --dport #{port} -j ACCEPT]
        break if $?.exitstatus != 0
      end
      %x[/sbin/iptables -I INPUT -p tcp --dport #{port} -j ACCEPT]
      %x[/sbin/ip6tables -I INPUT -p tcp --dport #{port} -j ACCEPT]
    end
    %x[/sbin/service iptables save]
    %x[/sbin/service ip6tables save]
  end
  action :create
  only_if {node[:firewall]}
end

service "httpd" do
  action [:start, :enable, :restart]
end
