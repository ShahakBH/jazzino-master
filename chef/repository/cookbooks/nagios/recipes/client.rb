execute "check_for_incompatible_nagios_plugins_rpm" do
  command "yum remove -y -q nagios-plugins"
  action :run
  only_if {%x(yum list installed | grep nagios-plugins | awk '{print $2}') =~ /.rf$/}
end

package "nagios-plugins"
package "nrpe"
package "nagios-plugins-load"
package "nagios-plugins-users"
package "nagios-plugins-apt"
package "nagios-plugins-breeze"
package "nagios-plugins-by_ssh"
package "nagios-plugins-cluster"
package "nagios-plugins-dhcp"
package "nagios-plugins-dig"
package "nagios-plugins-disk"
package "nagios-plugins-disk_smb"
package "nagios-plugins-dns"
package "nagios-plugins-file_age"
package "nagios-plugins-flexlm"
package "nagios-plugins-fping"
package "nagios-plugins-game"
package "nagios-plugins-hpjd"
package "nagios-plugins-http"
package "nagios-plugins-icmp"
package "nagios-plugins-ide_smart"
package "nagios-plugins-ifoperstatus"
package "nagios-plugins-ifstatus"
package "nagios-plugins-ircd"
package "nagios-plugins-ldap"
package "nagios-plugins-linux_raid"
package "nagios-plugins-log"
package "nagios-plugins-mailq"
package "nagios-plugins-mrtg"
package "nagios-plugins-mrtgtraf"
package "nagios-plugins-nagios"
package "nagios-plugins-nrpe"
package "nagios-plugins-nt"
package "nagios-plugins-ntp"
package "nagios-plugins-nwstat"
package "nagios-plugins-oracle"
package "nagios-plugins-overcr"
package "nagios-plugins-perl"
package "nagios-plugins-pgsql"
package "nagios-plugins-ping"
package "nagios-plugins-procs"
package "nagios-plugins-radius"
package "nagios-plugins-real"
package "nagios-plugins-rpc"
package "nagios-plugins-sensors"
package "nagios-plugins-smtp"
package "nagios-plugins-snmp"
package "nagios-plugins-ssh"
package "nagios-plugins-swap"
package "nagios-plugins-tcp"
package "nagios-plugins-time"
package "nagios-plugins-udp"
package "nagios-plugins-ups"
package "nagios-plugins-wave"

package "vnstat"
package "hddtemp"

plugin_dir = %x(uname -p).strip == 'x86_64' ? '/usr/lib64/nagios/plugins' : '/usr/lib/nagios/plugins'

directory "/etc/nagios" do
  owner "root"
  group "root"
  mode 0775
  action :create
end

directory "#{plugin_dir}/yazino" do
  owner "root"
  group "root"
  mode 0775
  recursive true
  action :create
end

cookbook_file "#{plugin_dir}/yazino/check_available_games.sh" do
  source "check_available_games.sh"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_bad_bo_queries" do
  source "check_bad_bo_queries"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_gc" do
  source "check_gc"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_jetty_threads" do
  source "check_jetty_threads"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_response_time" do
  source "check_response_time"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_jmx.jar" do
  source "check_jmx.jar"
  mode 0644
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_network_stats.sh" do
  source "check_network_stats.sh"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_network_bandwidth.pl" do
  source "check_network_bandwidth.pl"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_rabbit_memory.sh" do
  source "check_rabbit_memory.sh"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_rabbit_queues.sh" do
  source "check_rabbit_queues.sh"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_rabbit_tcp.sh" do
  source "check_rabbit_tcp.sh"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_rabbitmq_messages.sh" do
  source "check_rabbitmq_messages.sh"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_mysql_health" do
  source "check_mysql_health"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_init_service" do
  source "check_init_service"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_services" do
  source "check_services"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/custom_check_mem" do
  source "custom_check_mem"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/custom_check_procs" do
  source "custom_check_procs"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_open_files.pl" do
  source "check_open_files.pl"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_yum" do
  source "check_yum"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_cpu_stats.sh" do
  source "check_cpu_stats.sh"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_iostat" do
  source "check_iostat"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_gs_objects.rb" do
  source "check_gs_objects.rb"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_java_logs" do
  source "check_java_logs"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_raid" do
  source "check_raid"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "#{plugin_dir}/yazino/check_lightstreamer.rb" do
  source "check_lightstreamer.rb"
  mode 0755
  owner "root"
  group "root"
end

template "/etc/nagios/nrpe.cfg" do
  source "nrpe.cfg.erb"
  mode 0644
  owner "root"
  group "root"
  variables({
      :hosts => data_bag_item('nagios', 'server')['hosts'],
      :plugin_dir => plugin_dir
  })
end

execute "add_nrpe_service" do
  command "echo -e 'nrpe\t\t5666/tcp' >> /etc/services"
  action :run
  not_if "grep -E nrpe /etc/services"
end

execute "clean_up_nrpe_over_xinetd" do
  command "rm -f /etc/xinetd.d/nrpe"
  action :run
  only_if {File.exists?("/etc/xinetd.d/nrpe")}
end

execute "restart_xinetd" do
  command "service xinetd restart"
  action :run
  only_if {File.exists?("/etc/init.d/xinetd")}
end

execute "fix_check_icmp_permissions" do
  command "chmod +x #{plugin_dir}/check_icmp"
  action :run
end

execute "add_rabbitmqctl_permissions_for_nrpe" do
  command "echo -e '\nnrpe ALL=NOPASSWD: /usr/sbin/rabbitmqctl\n' >> /etc/sudoers"
  action :run
  not_if "grep -e 'nrpe\s*ALL=NOPASSWD:\s*/usr/sbin/rabbitmqctl' /etc/sudoers"
end

execute "add_memset_megacli_permissions_for_nrpe" do
  command "echo -e '\nnrpe ALL=NOPASSWD: /usr/local/bin/MegaCli\n' >> /etc/sudoers"
  action :run
  not_if "grep -e 'nrpe\s*ALL=NOPASSWD:\s*/usr/local/bin/MegaCli' /etc/sudoers"
end

execute "add_check_init_service_permissions_for_nrpe" do
  command "echo -e '\nnrpe ALL=NOPASSWD: #{plugin_dir}/yazino/check_init_service\n' >> /etc/sudoers"
  action :run
  not_if "grep -e 'nrpe\s*ALL=NOPASSWD:\s*#{plugin_dir}/yazino/check_init_service' /etc/sudoers"
end

execute "add_check_network_bandwidth_permissions_for_nrpe" do
  command "echo -e '\nnrpe ALL=NOPASSWD: #{plugin_dir}/yazino/check_network_bandwidth.pl\n' >> /etc/sudoers"
  action :run
  not_if "grep -e 'nrpe\s*ALL=NOPASSWD:\s*#{plugin_dir}/yazino/check_network_bandwidth.pl' /etc/sudoers"
end

execute "add_check_load_for_nrpe" do
  command "echo -e '\nnrpe ALL=NOPASSWD: #{plugin_dir}/check_load\n' >> /etc/sudoers"
  action :run
  not_if "grep -e 'nrpe\s*ALL=NOPASSWD:\s*#{plugin_dir}/check_load' /etc/sudoers"
end

ruby_block "flash-policy-server-firewall-rules" do
  block do
    [5666, 7900].each do |nrpe_port|
      while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':#{nrpe_port}\s*$' | wc -l].strip.to_i > 0
        node[:nagios][:servers][:ipv4].each do |monitoring_server|
          %x[/sbin/iptables -D INPUT -p tcp -s #{monitoring_server} --dport #{nrpe_port} -j ACCEPT]
        end
        break if $?.exitstatus != 0
      end
      while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':#{nrpe_port}\s*$' | wc -l].strip.to_i > 0
        node[:nagios][:servers][:ipv6].each do |monitoring_server|
          %x[/sbin/ip6tables -D INPUT -p tcp -s #{monitoring_server} --dport #{nrpe_port} -j ACCEPT]
        end
        break if $?.exitstatus != 0
      end

      node[:nagios][:servers][:ipv4].each do |monitoring_server|
        %x[/sbin/iptables -I INPUT -p tcp -s #{monitoring_server} --dport #{nrpe_port} -j ACCEPT]
      end
      node[:nagios][:servers][:ipv6].each do |monitoring_server|
        %x[/sbin/ip6tables -I INPUT -p tcp -s #{monitoring_server} --dport #{nrpe_port} -j ACCEPT]
      end
    end
    %x[/sbin/service iptables save]
    %x[/sbin/service ip6tables save]
  end
  action :create
  only_if {node[:firewall]}
end


execute "start_nrpe_on_system_start" do
  command "chkconfig --level 345 nrpe on"
  action :run
end

execute "start_nrpe" do
  command "service nrpe restart"
  action :run
end
