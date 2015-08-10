service "sshd" do
  supports :status => true, :restart => true, :reload => true
  action :enable
end

node[:ssh][:users].each_key do |username|
  #if `finger #{username} 2>&1` =~ /no such user/
  userstr = `getent passwd #{username} 2>&1`
  user_stat = $?
   if user_stat != 0
    # Chef user action expects to be root, won't work with sudo
    execute "add_user" do
      command "/usr/sbin/useradd -c 'SSH authorised user' #{username}"
      action :run
    end

    password = node[:ssh][:users][username]
    unless password.nil?
      execute "set_passwords" do
        command "echo #{password} | passwd --stdin #{username}"
        action :run
      end
    end
  end
end

execute "remove_ssh_tty_requirement" do
  command "sed -i -e 's%^Defaults.*requiretty%# Defaults    requiretty%g' /etc/sudoers"
  action :run
end

node[:ssh][:sudoers].each do |username|
  if (`grep -e '^#{username}.*ALL' /etc/sudoers` =~ /^#{username}/).nil?
    execute "add_sudo_access" do
      command "echo '#{username}  ALL=(ALL)  NOPASSWD: ALL' >> /etc/sudoers"
      action :run
    end
  end
end

ruby_block "ssh-firewall-rules" do
  block do
    if node[:ssh][:port] != 22
      while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':22\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/iptables -D INPUT -p tcp --dport 22 -j ACCEPT]
        break if $?.exitstatus != 0
      end
      while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':22\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/ip6tables -D INPUT -p tcp --dport 22 -j ACCEPT]
        %x[/sbin/ip6tables -D INPUT -p tcp -m state --state NEW -m tcp --dport 22 -j ACCEPT]
        break if $?.exitstatus != 0
      end
    end

    while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':#{node[:ssh][:port]}\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/iptables -D INPUT -p tcp --dport #{node[:ssh][:port]} -j ACCEPT]
      break if $?.exitstatus != 0
    end
    while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':#{node[:ssh][:port]}\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/ip6tables -D INPUT -p tcp --dport #{node[:ssh][:port]} -j ACCEPT]
      break if $?.exitstatus != 0
    end

    %x[/sbin/iptables -I INPUT -p tcp --dport #{node[:ssh][:port]} -j ACCEPT]
    %x[/sbin/service iptables save]
    %x[/sbin/ip6tables -I INPUT -p tcp --dport #{node[:ssh][:port]} -j ACCEPT]
    %x[/sbin/service ip6tables save]
  end
  action :create
  only_if {node[:firewall]}
end

directory "/tmp/keys" do
  owner "root"
  group "root"
  mode 0755
  action :create
end

cookbook_file "/tmp/keys/deployment-key-new.pub" do
  source "deployment-key-new.pub"
  owner "root"
  group "root"
  mode 0644
  action :create
end

template "/tmp/add_deployment_key.sh" do
  source "add_deployment_key.sh.erb"
  owner "root"
  group "root"
  mode 0755
  action :create
end

template "/tmp/add_deployment_key_for_users.sh" do
  source "add_deployment_key_for_users.sh.erb"
  owner "root"
  group "root"
  mode 0755
  variables(
    :ssh_users => node[:ssh][:users].keys
  )
  action :create
end

template "/etc/ssh/ssh_banner" do
  source "ssh_banner.erb"
  owner "root"
  group "root"
  mode 0600
  action :create
end

template "/etc/ssh/sshd_config" do
  source "sshd_config.erb"
  owner "root"
  group "root"
  mode 0600
  variables(
    :ssh_users => node[:ssh][:users].keys,
    :ssh_harden => node[:ssh][:harden],
    :ssh_port => node[:ssh][:port]
  )
  action :create
  notifies :restart, resources(:service => "sshd")
end

execute "add_authorized_key" do
  command "/tmp/add_deployment_key_for_users.sh"
  action :run
end

# Chef doesn't always restart SSHd, so we force the issue
execute "restart_ssh" do
  command "/sbin/service sshd restart"
  action :run
end
