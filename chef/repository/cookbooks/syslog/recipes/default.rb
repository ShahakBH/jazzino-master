rsyslogconf_source = nil

package "rsyslog" do
  action :install
end

if node.name !~ /-mon/
  if node[:environment] =~ /production/
    rsyslogconf = "rsyslog.conf-client-prd"
  elsif node[:environment] =~ /breakmycasino/
    rsyslogconf = "rsyslog.conf-client-bmc"
  elsif node[:environment] =~ /makemycasino/
    rsyslogconf = "rsyslog.conf-client-mmc"
  # devs local VMs
  else
    rsyslogconf = "rsyslog.conf-client-other"
  end
else
  rsyslogconf = "rsyslog.conf-server"
end

cookbook_file "/etc/rsyslog.conf" do
  source rsyslogconf
  mode "0644"
  owner "root"
  group "root"
  only_if {rsyslogconf != nil}
  action :create
end

execute "disable-syslog-on-centos5" do
  command "/sbin/chkconfig syslog off"
  action :run
  only_if {%x(cat /etc/redhat-release) =~ /CentOS release 5/}
end

execute "stop-syslog-on-centos5" do
  command "/sbin/service syslog stop"
  action :run
  only_if {%x(cat /etc/redhat-release) =~ /CentOS release 5/}
end

execute "enable-rsyslog-on-centos" do
  command "/sbin/chkconfig rsyslog on"
  action :run
end

execute "start-rsyslog-on-centos" do
  command "/sbin/service rsyslog start"
  action :run
end
