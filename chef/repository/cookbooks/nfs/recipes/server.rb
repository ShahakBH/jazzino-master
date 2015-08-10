yum_package "rpcbind" do
  action :install
end

yum_package "nfs-utils" do
  action :install
end

service "portmap" do
  supports :status => true, :restart => true
  service_name "rpcbind"
  action :nothing
end

service "nfs" do
  service_name "nfs"
  supports :status => true, :restart => true
  action :nothing
end

# The Chef resource version of this won't handle sudoing
execute "add-content-group" do
  command "/usr/sbin/groupadd -g '1001' contentuser"
  action :run
  only_if {%x[cat /etc/group | grep '^contentuser:'].strip.length == 0}
end

ruby_block "check-content-group-configuration" do
  block do
    if %x[cat /etc/group | grep '^contentuser:' | grep 1001].strip.length == 0
      puts "Group contentuser exists but does not have GID 1001; please correct manually"
      exit 1
    end
  end
  action :create
end

node[:nfs][:exports].each do |path|
  directory path do
      owner "root"
      group "root"
      mode "0755"
      action :create
    end
end

export_paths = ["/var/marketing", "/var/avatars", "/var/staging", "/var/yazino-games"]
lobby_nodes = []

search(:node, 'role:*') do |lobby_node|
  if lobby_node[:environment] == node[:environment]
    lobby_nodes << lobby_node
  end
end

template "/etc/exports" do
  source "exports.erb"
  owner "root"
  group "root"
  mode "0644"
  variables(:lobby_nodes => lobby_nodes, :export_paths => export_paths)
  notifies :restart, resources(:service => "portmap"), :immediately
  notifies :restart, resources(:service => "nfs"), :immediately
end

directory "/var/staging" do
  owner "root"
  group "root"
  mode "0755"
  action :create
end

directory "/var/marketing" do
  owner "root"
  group "contentuser"
  mode "0775"
  action :create
end

directory "/var/avatars" do
  owner "root"
  group "contentuser"
  mode "0775"
  action :create
end

directory "/var/yazino-games" do
  owner "root"
  group "contentuser"
  mode "0775"
  action :create
end

directory "/var/staging/content" do
  owner "root"
  group "root"
  mode "0755"
  action :create
end

execute "boot_portmap" do
  command "/sbin/chkconfig portmap on --level 235"
  action :run
  only_if {%x(cat /etc/redhat-release) =~ /CentOS release 5/}
end

execute "boot_portmap" do
  command "/sbin/chkconfig rpcbind on --level 235"
  action :run
  only_if {%x(cat /etc/redhat-release) =~ /CentOS release 6/}
end

execute "boot_nfs" do
  command "/sbin/chkconfig nfs on --level 235"
  action :run
end
