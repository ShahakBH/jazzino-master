yum_package "rpcbind" do
  action :install
end

yum_package "nfs-utils" do
  action :install
end

service "rpcbind" do
  supports :status => true, :restart => true
  service_name "rpcbind"
  action :start
end

service "nfs" do
  service_name "nfs"
  supports :status => true, :restart => true
  action :start
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

existing_mounts = {}
%x{mount -t nfs,nfs4}.each_line do |mount|
  existing_mounts[$1] = $2 if mount =~ /(.*) on (.*) type nfs/
end

if existing_mounts != node[:nfs][:mount_points].to_hash
  execute "umount-nfs-mounts" do
    command "umount -t nfs,nfs4 -a"
    timeout 60
    action :run
  end

  execute "cleanup-nfs-mounts" do
    command "cp /etc/fstab /etc/fstab.backup.#{Time.now.to_i} && cat /etc/fstab | grep -v ' nfs ' > /tmp/fstab && mv /tmp/fstab /etc/fstab"
    action :run
    only_if {%x[grep -c ' nfs ' /etc/fstab].to_i > 0}
  end

  node[:nfs][:mount_points].each_pair do |host_path, target|
    directory target do
      owner "root"
      group "contentuser"
      mode "0775"
      action :create
    end
    mount target do
      device "#{host_path}"
      fstype "nfs"
      options "hard,intr"
      action [:mount, :enable]
    end
  end
end
