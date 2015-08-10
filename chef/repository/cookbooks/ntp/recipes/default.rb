package "ntp" do
  action :install
end

ntp_config = data_bag_item("ntp", "server")

raise RuntimeError.new("No zoneinfo is configured in ntp:server") if ntp_config['zoneinfo'].nil?
raise RuntimeError.new("No servers are configured in ntp:server") if ntp_config['servers'].nil?

link "/etc/localtime" do
  to "/usr/share/zoneinfo/#{ntp_config['zoneinfo']}"
end

execute "boot_ntpd" do
  command "/sbin/chkconfig  ntpd  on --level 235"
  action :run
end

service "ntpd" do
  action :start
end

template "/etc/ntp.conf" do
  source "ntp.conf.erb"
  owner "root"
  group "root"
  mode 0644
  variables(:ntp_config => ntp_config)
  notifies :restart, resources(:service => "ntpd")
end
