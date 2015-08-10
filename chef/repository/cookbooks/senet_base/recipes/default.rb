ruby_block "check_host_configuration" do
  block do
    %x[/bin/ping -c 1 `hostname -s`]
    if $?.exitstatus != 0
      puts "Host cannot ping its hostname, please fix before continuing"
      exit 1
    end
  end
  action :create
end

execute "install-epel-6" do
  command "rpm -Uvh http://download.fedoraproject.org/pub/epel/6/i386/epel-release-6-8.noarch.rpm"
  action :run
  only_if {%x(yum list installed | grep epel-release) !~ /epel-release/}
end

package "man" do
  action :install
end

package "sysstat" do
  action :install
end

yum_package "glibc" do
  action :install
  arch 'i686'
end

yum_package "glibc" do
  action :install
  arch 'x86_64'
end

package "bc" do
  action :install
end

package "mc" do
  action :install
end

package "pigz" do
  action :install
end

package "pv" do
  action :install
end

package "screen" do
  action :install
end

package "dialog" do
  action :install
end

package "telnet" do
  action :install
end

package "perl" do
  action :install
end

package "ipmitool" do
  action :install
end

package "git" do
  action :install
end

package "finger" do
  action :install
end

package "jwhois" do
  action :install
end

package "htop" do
  action :install
end

package "tmux" do
  action :install
end

cookbook_file "/etc/tmux.conf" do
  action :create
  source "tmux.conf"
  mode 0644
  owner "root"
  group "root"
end

package "wget" do
  action :install
end

package "denyhosts" do
  action :install
end

package "vim-enhanced" do
  action :install
end

package "e4fsprogs" do
  action :install
end

package "logwatch" do
  action :remove
end

directory "/etc/senet" do
  owner "root"
  group "root"
  mode "0755"
  action :create
end

package "nscd" do
  action :install
end

execute "enable_nscd" do
  command "/sbin/chkconfig nscd on --level 235"
  action :run
end

package "xinetd" do
  action :install
end

execute "enable_xinetd" do
  command "/sbin/chkconfig xinetd on"
  action :run
end

execute "start_xinetd" do
  command "/sbin/service xinetd start"
  action :run
end

directory "/var/log/maximiles" do
  owner "root"
  group "root"
  mode "0755"
  action :delete
  recursive true
end

template "/var/lib/denyhosts/allowed-hosts" do
  source "allowed-hosts.erb"
  owner "root"
  group "root"
  mode "0644"
  variables(
    :allowed_clients => node[:ssh][:allowed_clients]
  )
end

cookbook_file "/etc/xinetd.conf" do
  action :create
  source "xinetd.conf"
  mode 0600
  owner "root"
  group "root"
end

execute "reload_xinetd_config" do
  command "/sbin/service xinetd reload"
  action :run
end

if node[:monitoring][:suppress]
  execute "remove-email-forwarding" do
    command "rm -f ~/.forward"
    action :run
  end
else
  execute "email-forwarding" do
    command "echo #{node[:monitoring][:email]} > ~/.forward"
    action :run
  end
end

ruby_block "set-disk-readahead-buffers" do
  block do
    node[:disk][:buffer][:ra].each_pair do |device, buffer_size|
      %x[blockdev --setra #{buffer_size} #{device}]
    end
  end
  action :create
  only_if {!node[:disk][:buffer][:ra].empty?}
end

ruby_block "set-mount-options" do
  block do
    node[:disk][:mount_options].each_pair do |mount, options|
      if %x[egrep '[[:space:]]+#{mount}[::space::]+' /etc/fstab | awk '{print $4}'].strip != options
        %x[sed -i'' -E 's%([[:space:]]+#{mount}[[:space:]]+[^[:space:]]+[[:space:]]+)[^[:space:]]+%\\1#{options}%g' /etc/fstab]
      end
    end
  end
  action :create
end

template "/usr/bin/set-io-schedulers" do
  source "set-io-schedulers.erb"
  owner "root"
  group "root"
  mode "0755"
  variables(
    :schedulers => node[:disk][:io_scheduler]
  )
end

template "/etc/cron.d/yazino-reboot" do
  source "cron-reboot.erb"
  owner "root"
  group "root"
  mode "0755"
  variables(
    :jobs => ["/usr/bin/set-io-schedulers"]
  )
end

execute "ensure-iptables-has-default-reject-rules" do
  command "iptables -A INPUT -j REJECT"
  action :run
  only_if {node[:firewall] && %x[service iptables status | grep -c REJECT].strip == '0'}
end

execute "ensure-iptables-allows-pings" do
  command "iptables -I INPUT -p icmp -j ACCEPT"
  action :run
  only_if {node[:firewall] && %x[service iptables status | grep ACCEPT | grep -c icmp].strip == '0'}
end

execute "ensure-iptables-allows-established" do
  command "iptables -I INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT"
  action :run
  only_if {node[:firewall] && %x[service iptables status | grep ACCEPT | grep ESTABLISHED | grep -c RELATED].strip == '0'}
end

execute "ensure-iptables-accepts-all-on-loopback" do
  command "iptables -I INPUT -i lo -j ACCEPT"
  action :run
  only_if {node[:firewall] && %x[iptables --list -v | grep ACCEPT | grep -c lo].strip == '0'}
end

execute "ensure-iptables-accepts-all-local-traffic" do
  command "iptables -I INPUT -i #{node[:internal_interface]} -j ACCEPT"
  action :run
  only_if {node[:firewall] && node[:internal_interface].strip != "" && %x[iptables --list -v | grep ACCEPT | grep -c #{node[:internal_interface]}].strip == '0'}
end

execute "save-iptables" do
  command "service iptables save"
  action :run
  only_if {node[:firewall]}
end

execute "ensure-ip6tables-has-default-reject-rules" do
  command "ip6tables -A INPUT -j REJECT"
  action :run
  only_if {node[:firewall] && %x[service ip6tables status | grep -c REJECT].strip == '0'}
end

execute "ensure-ip6tables-allows-pings" do
  command "ip6tables -I INPUT -p ipv6-icmp -j ACCEPT"
  action :run
  only_if {node[:firewall] && %x[service ip6tables status | grep ACCEPT | grep -c icmp].strip == '0'}
end

execute "ensure-ip6tables-allows-established" do
  command "ip6tables -I INPUT -m state --state RELATED,ESTABLISHED -j ACCEPT"
  action :run
  only_if {node[:firewall] && %x[service iptables status | grep ACCEPT | grep ESTABLISHED | grep -c RELATED].strip == '0'}
end

execute "ensure-ip6tables-accepts-all-on-loopback" do
  command "ip6tables -I INPUT -i lo -j ACCEPT"
  action :run
  only_if {node[:firewall] && %x[ip6tables --list -v | grep ACCEPT | grep -c lo].strip == '0'}
end

execute "ensure-ip6tables-accepts-all-local-traffic" do
  command "ip6tables -I INPUT -i #{node[:internal_interface]} -j ACCEPT"
  action :run
  only_if {node[:firewall] && node[:internal_interface].strip != "" && %x[ip6tables --list -v | grep ACCEPT | grep -c #{node[:internal_interface]}].strip == '0'}
end

execute "save-ip6tables" do
  command "service ip6tables save"
  action :run
  only_if {node[:firewall]}
end

if node[:security][:denyhosts]
  service "denyhosts" do
    service_name "denyhosts"
    supports :status => true, :restart => true
    action :restart
  end

  execute "boot_denyhosts" do
    command "/sbin/chkconfig denyhosts on --level 235"
    action :run
  end
elsif File.exists?("/etc/init.d/denyhosts")
  service "denyhosts" do
    service_name "denyhosts"
    supports :status => true, :stop => true
    action :stop
  end
  execute "boot_denyhosts" do
    command "/sbin/chkconfig denyhosts off"
    action :run
  end
end
