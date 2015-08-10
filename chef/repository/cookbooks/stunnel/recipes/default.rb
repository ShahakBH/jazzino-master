yum_package "stunnel" do
  version "4.56-1"
  action :install
end

execute "add stunnel user/group" do
  command "groupadd -g 51 stunnel && useradd -c stunnel -d /var/lib/stunnel -g stunnel -s /bin/false -u 51 stunnel"
  action :run
  not_if "id -u stunnel"
end

directory "/etc/stunnel" do
  owner "root"
  group "root"
  mode 0775
  action :create
end

cookbook_file "/etc/stunnel/cert.key" do
  action :create_if_missing
  source "cert.key"
  mode 0600
  owner "root"
  group "root"
end

cookbook_file "/etc/stunnel/cert-chain.pem" do
  action :create_if_missing
  source "cert-chain.pem"
  mode 0600
  owner "root"
  group "root"
end

cookbook_file "/etc/init.d/stunnel" do
  action :create
  source "stunnel"
  mode 0755
  owner "root"
  group "root"
end

template "/etc/stunnel/stunnel.conf" do
  source "stunnel.conf.erb"
  mode 0644
  owner "root"
  group "root"
  variables(
    :listen_ipv4 => node[:stunnel][:listen_ipv4],
    :listen_ipv6 => node[:stunnel][:listen_ipv6],
    :listen_port => node[:stunnel][:listen_port],
    :redirect_port => node[:stunnel][:redirect_port]
  )
end

execute "start or reload stunnel" do
  command "/etc/init.d/stunnel start && /etc/init.d/stunnel reload"
  action :run
end
