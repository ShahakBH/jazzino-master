include_recipe "mysql::client"

execute "remove-community-mysql-server" do
  command "yum remove -y MySQL-server-community"
  action :run
  only_if {%x(yum list installed | grep MySQL-server-community) =~ /installed/}
end

major_version = "56"
version = "5.6.17-rel65.0"
system = 'el6'
# Update version in the mysql_upgrade check below as well!

service "mysql" do
  supports :status => true, :restart => true, :reload => true
end

package "Percona-Server-server-#{major_version}" do
  version "#{version}.#{system}"
  action :install
end

package "MySQL-python" do
  action :install
end

package "xtrabackup" do
  action :install
end

if node[:mysql][:role] == "master"
  binlogdir = node[:mysql][:binlog_dir]
  binlogfile = node[:mysql][:binlog_dir]

  binlogfile = binlogfile.sub(/.*\//, "")
  binlogdir = binlogdir.sub(/\/#{binlogfile}$/, "")

  directory node[:mysql][:datadir] do
    owner "mysql"
    group "mysql"
    mode "0755"
    recursive true
    action :create
    not_if "test -d #{node[:mysql][:datadir]}"
  end

  directory binlogdir do
    owner "mysql"
    group "mysql"
    mode "0755"
    recursive true
    action :create
    not_if "test -d #{binlogdir}"
  end

  file "#{node[:mysql][:binlog_dir]}.index" do
    owner "mysql"
    group "mysql"
    mode "0644"
    action :create_if_missing
  end

# there's a chef bug in the recurive creation of directories -- it doesn't
# chown for parent dirs, so we have to do it "manually"
  execute "Change_owner_of_var_lib_mysql" do
    command "chown -R mysql.mysql /var/lib/mysql"
      action :run
  end

  execute "create_mysql_database" do
    command "mysql_install_db; chown -R mysql.mysql /var/lib/mysql"
    action :run
    only_if {Dir["#{node[:mysql][:datadir]}/mysql/*"].empty?}
  end

end

if !node[:mysql][:root_password].nil?
  %x(mysql -u root -p'#{node[:mysql][:root_password]}' -e 'SELECT 0 FROM DUAL')
  if $?.exit_status != 0
    execute "set_root_password" do
      command "mysqladmin -u root password #{node[:mysql][:root_password]}"
      action :run
    end
  end
end

template "/etc/my.cnf" do
  source "my.cnf.erb"
  owner "root"
  group "root"
  mode "0644"
  variables(
    :role => node[:mysql][:role],
    :dbname => node[:mysql][:db],
    :max_connections => node[:mysql][:tunable][:max_connections],
    :max_allowed_packet => node[:mysql][:tunable][:max_allowed_packet],
    :innodb_buffer_pool_size => node[:mysql][:tunable][:innodb_buffer_pool_size],
    :innodb_log_file_size => node[:mysql][:tunable][:innodb_log_file_size],
    :innodb_log_buffer_size => node[:mysql][:tunable][:innodb_log_buffer_size],
    :thread_cache_size => node[:mysql][:tunable][:thread_cache_size],
    :table_open_cache => node[:mysql][:tunable][:table_open_cache],
    :data_dir => node[:mysql][:datadir],
    :backup_dir => node[:mysql][:backupdir],
    :binlog_dir => node[:mysql][:binlog_dir],
    :use_percona => true
  )
  notifies :restart, resources(:service => "mysql"), :immediately
end

directory "/etc/mysql" do
  owner "root"
  group "root"
  mode "0755"
  action :create
  not_if "test -d /etc/mysql"
end

if node[:mysql][:role] == "master"
  template "/etc/mysql/repl-grants.sql" do
    source "repl-grants.sql.erb"
    owner "root"
    group "root"
    mode "0644"
    variables(
      :repl_username => node[:mysql][:replication][:username],
      :repl_password => node[:mysql][:replication][:password]
    )
    action :create
  end

  service "mysql" do
    action :start
  end

  mysql_user = %x(mysql -u root #{node[:mysql][:db]} -e "SELECT user FROM mysql.user WHERE user='#{node[:mysql][:replication][:username]}'")
  if mysql_user.nil? || mysql_user.strip.length == 0
    execute "mysql-replication-privileges" do
      command "/usr/bin/mysql -u root < /etc/mysql/repl-grants.sql"
      action :run
    end
  end
end

execute "start-mysql-if-it-is-dead" do
  command "service mysql start"
  only_if {%x(service mysql status) =~ /not running/}
end

execute "upgrade-mysql" do
  command "sudo -u mysql mysql_upgrade -u root"
  action :run
  only_if "function tst() { GREP=`grep -v '5\.6\.17' #{node[:mysql][:datadir]}/mysql_upgrade_info 1>&2 2>/dev/null`; if [ $? == 0 ]; then return 0; else return 1; fi }; tst"
end

ruby_block "mysql-firewall-rules" do
  block do
    while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':3306\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/iptables -D INPUT -p tcp --dport 3306 -j ACCEPT]
      break if $?.exitstatus != 0
    end
    %x[/sbin/iptables -I INPUT -p tcp --dport 3306 -j ACCEPT]
    %x[/sbin/service iptables save]
  end
  action :create
  only_if {node[:firewall]}
end

execute "boot_mysqld" do
  command "/sbin/chkconfig mysql on --level 235"
  action :run
end
