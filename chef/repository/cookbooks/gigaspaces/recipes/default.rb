gigaspaces_version = "9.7.0-1"
slf4j_version = '1.7.7'
logback_version = '1.1.2'

package "gigaspaces-platform" do
  version gigaspaces_version
  action :install
end

directory "/var/log/gs-agent" do
  owner "gsrun"
  group "gsrun"
  mode 0755
  action :create
  recursive true
end

link "/opt/gigaspaces/logs" do
  to "/var/log/gs-agent"
  action :create
end

cookbook_file "/opt/gigaspaces/gslicense.xml" do
  source "gslicense-#{node[:gigaspaces][:licence]}.xml"
  mode 0755
  owner "gsrun"
  group "gsrun"
end

template "/etc/init.d/gs-agent" do
    source "gs-agent.erb"
    mode 0755
    owner "root"
    group "wheel"
end

template "/etc/profile.d/gs.sh" do
  source "gs.sh.erb"
  mode 0755
  owner "root"
  group "root"
  variables(
    :gsc_count => node[:gigaspaces][:gsc_count],
    :gsm_count => node[:gigaspaces][:gsm_count],
    :gs_home => node[:gigaspaces][:directory],
    :gs_roles => node[:gigaspaces][:roles],
    :gs_zones => node[:gigaspaces][:zones],
    :gc_type => node[:gigaspaces][:gc_type],
    :lookup_locators => node[:gigaspaces][:lookup_locators],
    :lookup_group => node[:gigaspaces][:lookup_group],
    :nic_address => node[:gigaspaces][:host_ip],
    :logback_config => node[:gigaspaces][:logback_file],
    :gsc_min_heap => node[:gigaspaces][:java][:min_mem],
    :gsc_max_heap => node[:gigaspaces][:java][:max_mem],
    :gsm_min_heap => node[:gigaspaces][:java][:gsm_min_heap],
    :gsm_max_heap => node[:gigaspaces][:java][:gsm_max_heap],
    :lus_min_heap => node[:gigaspaces][:java][:lus_min_heap],
    :lus_max_heap => node[:gigaspaces][:java][:lus_max_heap],
    :lrmi_max_threads => node[:gigaspaces][:lrmi][:max_threads],
    :lrmi_selector_threads => node[:gigaspaces][:lrmi][:selector_threads],
    :lrmi_thread_timeout => node[:gigaspaces][:lrmi][:thread_timeout],
    :gsc_debug => node[:gigaspaces][:gsc_debug],
    :logback_version => logback_version,
    :slf4j_version => slf4j_version
  )
end

directory "/opt/gigaspaces/yazino/lib" do
  owner "gsrun"
  group "gsrun"
  mode 0755
  action :create
  recursive true
end

remote_file "/opt/gigaspaces/yazino/lib/slf4j-api-#{slf4j_version}.jar" do
  source "http://central.maven.org/maven2/org/slf4j/slf4j-api/#{slf4j_version}/slf4j-api-#{slf4j_version}.jar"
end

remote_file "/opt/gigaspaces/yazino/lib/logback-core-#{logback_version}.jar" do
  source "http://central.maven.org/maven2/ch/qos/logback/logback-core/#{logback_version}/logback-core-#{logback_version}.jar"
end

remote_file "/opt/gigaspaces/yazino/lib/logback-classic-#{logback_version}.jar" do
  source "http://central.maven.org/maven2/ch/qos/logback/logback-classic/#{logback_version}/logback-classic-#{logback_version}.jar"
end

ruby_block "gigaspaces-firewall-rules" do
  block do
    def delete_accept_rule(port, source_ip = nil)
      source_delete = if source_ip then "-s #{source_ip}" else "" end
      while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':#{port}\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/iptables -D INPUT -p tcp --dport #{port} #{source_delete} -j ACCEPT]
        break if $?.exitstatus != 0
      end
      while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':#{port}\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/ip6tables -D INPUT -p tcp --dport #{port} #{source_delete} -j ACCEPT]
        break if $?.exitstatus != 0
      end
    end

    node[:gigaspaces][:grid_networks].each do |network|
      iptables = if network =~ /\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}/ then 'iptables' else 'ip6tables' end
      while %x[/sbin/#{iptables} --list -n | grep ACCEPT | egrep -c '\s+#{network}\s+'].strip.to_i > 0
        %x[/sbin/#{iptables} -D INPUT -p tcp -s #{network} -j ACCEPT]
        break if $?.exitstatus != 0
      end
      %x[/sbin/#{iptables} -I INPUT -p tcp -s #{network} -j ACCEPT]
    end

    delete_accept_rule(8081)
    node[:gigaspaces][:web_ui][:whitelist].each do |whitelist_ip|
      delete_accept_rule(8081, whitelist_ip)

      iptables = if whitelist_ip =~ /\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}/ then 'iptables' else 'ip6tables' end
      %x[/sbin/#{iptables} -I INPUT -p tcp --dport 8081 -s #{whitelist_ip} -j ACCEPT]
    end

    %x[/sbin/service iptables save]
    %x[/sbin/service ip6tables save]
  end
  action :create
  only_if {node[:firewall]}
end

cookbook_file "/usr/sbin/gs-agent" do
    source "gs-agent"
    mode 0755
    owner "root"
    group "wheel"
end
