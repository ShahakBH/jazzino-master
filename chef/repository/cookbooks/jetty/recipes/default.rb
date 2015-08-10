jetty_version = "9.2.1.v20140609"
old_jetty_version = "9.1.3.v20140225"

execute "remove-hightide" do
  command "yum remove -y jetty-hightide"
  action :run
  only_if {%x(yum list installed | grep jetty-hightide) =~ /jetty-hightide/}
end

execute "kill-old-jetty" do
  command "pkill -f #{old_jetty_version}"
  action :run
  only_if "pgrep -f #{old_jetty_version}"
end

package "jetty-distribution" do
  version "#{jetty_version}-1"
  action :upgrade
end

service "jetty" do
  action :nothing
  supports :start => true, :restart => true, :status => true
end

execute "upgrade-jetty-webapps" do
  command "cp -u /opt/jetty-hightide-#{old_jetty_version}/webapps/*.war /opt/jetty-distribution-#{jetty_version}/webapps/"
  notifies :restart, "service[jetty]"
  action :run
  only_if {File.exists?("/opt/jetty-hightide-#{old_jetty_version}/webapps") && Dir.entries("/opt/jetty-hightide-#{old_jetty_version}/webapps").size > 2}
end

execute "upgrade-jetty-keystore" do
  command "cp -f /opt/jetty-hightide-#{old_jetty_version}/etc/keystore /opt/jetty-distribution-#{jetty_version}/etc/keystore"
  notifies :restart, "service[jetty]"
  action :run
  only_if {File.exists?("/opt/jetty-hightide-#{old_jetty_version}/etc/keystore")}
end

execute "cleanup-old-jetty" do
  command "rm -rf /opt/jetty-hightide-#{old_jetty_version} /etc/default/jetty"
  action :run
  only_if {File.exists?("/opt/jetty-hightide-#{old_jetty_version}")}
end

execute "delete-jetty-demo" do
  command "rm -rf /opt/jetty/start.d/900-demo.ini"
  action :run
  only_if {File.exists?("/opt/jetty/start.d/900-demo.ini")}
end

directory "/opt/jetty/work" do
  owner "jetty"
  group "jetty"
  mode 0755
  action :create
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

execute "add-jetty-to-content-group" do
  command "/usr/sbin/usermod -a -G contentuser jetty"
  action :run
  only_if {%x(cat /etc/group | grep '^contentuser:' | grep jetty).strip.length == 0}
end

execute "remove-default-logs-dir" do
  command "rm -rf /opt/jetty/logs"
  action :run
  only_if {File.exists?("/opt/jetty/logs")}
end

link "/opt/jetty/logs" do
  to node[:jetty][:logs]
  action :create
end

if !File.exists?(node[:jetty][:logs])
  directory node[:jetty][:logs] do
    owner "jetty"
    group "jetty"
    mode 0755
    action :create
    recursive true
  end
end

execute "delete-default-keystore" do
  command "rm -rf /opt/jetty/etc/keystore"
  action :run
  only_if {%x(md5sum /opt/jetty/etc/keystore | awk '{print $1}') =~ /6478d2eef8b6d85551c17141da2a7a93/}
end

execute "delete-test-webapps" do
  command "rm -rf /opt/jetty/contexts/hightide.xml /opt/jetty/contexts/javadoc.xml /opt/jetty/contexts/test.d /opt/jetty/contexts/test-annotations.xml /opt/jetty/contexts/test-jaas.xml /opt/jetty/contexts/test-jndi.xml /opt/jetty/contexts/test.xml /opt/jetty/webapps/test-annotations /opt/jetty/webapps/test-jaas /opt/jetty/webapps/test-jndi /opt/jetty/webapps/test.war /opt/jetty/webapps/async-rest /opt/jetty/webapps/cometd.war /opt/jetty/webapps/root /opt/jetty/webapps/spdy.war"
  action :run
end

cookbook_file "/etc/init.d/jetty" do
  source "jetty"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "/opt/jetty/etc/keystore" do
  action :create_if_missing
  source "default_keystore"
  mode 0600
  owner "jetty"
  group "jetty"
end

cookbook_file "/etc/cron.daily/compress-rolled-jetty-logs" do
  source "compress-rolled-jetty-logs"
  mode 0755
  owner "root"
  group "root"
end

template "/opt/jetty/etc/jetty.conf" do
  source "jetty.conf.erb"
  mode 0644
  owner "jetty"
  group "jetty"
  variables(
    :access_logs => node[:jetty][:access_logs][:active],
    :rewrites => node[:jetty][:rewrites]
  )
end

template "/opt/jetty/etc/jetty.xml" do
  source "jetty.xml.erb"
  mode 0644
  owner "jetty"
  group "jetty"
  variables(
    :ssl_port => node[:jetty][:ssl_port],
    :min_threads => node[:jetty][:min_threads],
    :max_threads => node[:jetty][:max_threads],
    :idle_timeout => node[:jetty][:thread_idle_timeout],
    :detailed_dump => node[:jetty][:detailed_dump],
    :rewrites => node[:jetty][:rewrites],
    :max_form_content_size => node[:jetty][:max_form_content_size],
    :output_buffer_size => node[:jetty][:output_buffer_size],
    :request_header_size => node[:jetty][:request_header_size],
    :response_header_size => node[:jetty][:response_header_size],
    :header_cache_size => node[:jetty][:header_cache_size]
  )
end

template "/opt/jetty/etc/yazino-ssl.xml" do
  source "yazino-ssl.xml.erb"
  mode 0644
  owner "jetty"
  group "jetty"
  variables(
    :keystore => node[:jetty][:keystore],
    :keystore_password => node[:jetty][:keystore_password],
    :certificate => node[:jetty][:certificate]
  )
end

template "/opt/jetty/etc/yazino-logging.xml" do
  source "yazino-logging.xml.erb"
  mode 0644
  owner "jetty"
  group "jetty"
  variables(
    :log_file => node[:jetty][:stderrout_logs][:file],
    :retain_days => node[:jetty][:stderrout_logs][:retention_days]
  )
end

template "/opt/jetty/etc/yazino-requestlog.xml" do
  source "yazino-requestlog.xml.erb"
  mode 0644
  owner "jetty"
  group "jetty"
  variables(
    :access_log_file => node[:jetty][:access_logs][:file],
    :retain_days => node[:jetty][:access_logs][:retention_days]
  )
end

template "/opt/jetty/etc/yazino-rewrite.xml" do
  source "yazino-rewrite.xml.erb"
  mode 0644
  owner "jetty"
  group "jetty"
end

template "/opt/jetty/start.d/yazino.ini" do
  source "yazino.ini.erb"
  mode 0644
  owner "jetty"
  group "jetty"
  variables(
    :rewrites => node[:jetty][:rewrites],
    :access_logs => node[:jetty][:access_logs][:active],
    :http_port => node[:jetty][:port],
    :https_port => node[:jetty][:ssl_port],
    :http_timeout => node[:jetty][:http_idle_timeout],
    :max_mem => node[:jetty][:java][:max_mem],
    :min_mem => node[:jetty][:java][:min_mem],
    :private_ip => node[:private_ip],
    :debug => node[:jetty][:debug],
    :debug_port => node[:jetty][:debug_port],
    :jmx_port => node[:jetty][:jmx_port],
    :gc_type => node[:jetty][:gc_type],
    :logs => node[:jetty][:logs],
    :gigaspace_timeout => node[:gigaspaces][:unicast_timeout]
  )
end

file "/opt/jetty/start.d/http.ini" do
  action :delete
end

template "/etc/xinetd.d/jetty" do
  source "jetty.xinetd.erb"
  mode 0644
  owner "root"
  group "root"
  variables(
    :port => node[:jetty][:port],
    :ssl_port => node[:jetty][:ssl_port]
  )
end

cookbook_file "/opt/jetty/webapps/jetty-monitor.war" do
  source "jetty-monitor.war"
  mode 0644
  owner "jetty"
  group "jetty"
end

service "jetty" do
  action [:start, :enable]
end

execute "reload_xinetd_jetty" do
  command "/sbin/service xinetd reload"
  action :run
end

ruby_block "jetty-firewall-rules" do
  block do
    def delete_accept_rule(port)
      while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':#{port}\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/iptables -D INPUT -p tcp --dport #{port} -j ACCEPT]
        break if $?.exitstatus != 0
      end
      while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':#{port}\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/ip6tables -D INPUT -p tcp --dport #{port} -j ACCEPT]
        break if $?.exitstatus != 0
      end
    end

    def delete_old_redirect_rules(from, to)
      while %x[/sbin/iptables --list -n -t nat | grep -e ':#{from} redir ports #{to}\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/iptables -t nat -D PREROUTING -j REDIRECT -p tcp --dport #{from} --to-ports #{to}]
        break if $?.exitstatus != 0
      end
      while %x[/sbin/ip6tables --list -n -t nat | grep -e ':#{from} redir ports #{to}\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/ip6tables -t mangle -I PREROUTING -p tcp --dport #{from} -j TPROXY --on-port #{to}]
        break if $?.exitstatus != 0
      end
    end

    # Clean up old Jetty rules
    delete_accept_rule(80)
    delete_accept_rule(443)

    delete_old_redirect_rules(80, node[:jetty][:port])
    delete_old_redirect_rules(443, node[:jetty][:ssl_port])

    # Clean up Tomcat rules, if present
    if !node[:tomcat].nil?
      delete_redirect_rule(80, node[:tomcat][:port]) if !node[:tomcat][:port].nil?
      delete_redirect_rule(443, node[:tomcat][:ssl_port]) if !node[:tomcat][:ssl_port].nil?
    end

    %x[/sbin/iptables -I INPUT -p tcp --dport 80 -j ACCEPT]
    %x[/sbin/iptables -I INPUT -p tcp --dport 443 -j ACCEPT]
    %x[/sbin/service iptables save]

    %x[/sbin/ip6tables -I INPUT -p tcp --dport 80 -j ACCEPT]
    %x[/sbin/ip6tables -I INPUT -p tcp --dport 443 -j ACCEPT]
    %x[/sbin/service ip6tables save]
  end
  action :create
  only_if {node[:firewall] && node[:jetty][:port] != 80}
end
