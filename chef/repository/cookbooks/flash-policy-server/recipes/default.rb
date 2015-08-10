package "flash-policy-server" do
  action :install
end

link "/usr/bin/java" do
  to "/opt/java/bin/java"
  action :create
end

service "flash-policy-server" do
  action [:start, :enable]
end

service "iptables" do
  action [:enable]
  only_if {node[:firewall]}
end

cookbook_file "/etc/cron.daily/clean-flash-policy-server-logs" do
  source "clean-flash-policy-server-logs"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "/etc/xinetd.d/flash-policy-server" do
  source "flash-policy-server.xinetd"
  mode 0644
  owner "root"
  group "root"
end

execute "reload_xinetd_flash-policy-server" do
  command "/sbin/service xinetd reload"
  action :run
end

ruby_block "flash-policy-server-firewall-rules" do
  block do
    while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':843\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/iptables -D INPUT -p tcp --dport 843 -j ACCEPT]
      break if $?.exitstatus != 0
    end
    while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':843\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/ip6tables -D INPUT -p tcp --dport 843 -j ACCEPT]
      break if $?.exitstatus != 0
    end
    while %x[/sbin/iptables --list -n -t nat | grep -e ':843 redir ports 8843\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/iptables -t nat -D PREROUTING -j REDIRECT -p tcp --dport 843 --to-ports 8843]
      break if $?.exitstatus != 0
    end
    while %x[/sbin/ip6tables --list -n -t nat | grep -e ':843 redir ports 8843\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/ip6tables -t nat -D PREROUTING -j REDIRECT -p tcp --dport 843 --to-ports 8843]
      break if $?.exitstatus != 0
    end

    %x[/sbin/iptables -I INPUT -p tcp --dport 843 -j ACCEPT]
    %x[/sbin/service iptables save]
    %x[/sbin/ip6tables -I INPUT -p tcp --dport 843 -j ACCEPT]
    %x[/sbin/service ip6tables save]
  end
  action :create
  only_if {node[:firewall]}
end
