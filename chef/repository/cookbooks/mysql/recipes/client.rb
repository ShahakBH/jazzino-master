execute "install-percona-repo" do
  command "rpm -Uhv http://www.percona.com/downloads/percona-release/percona-release-0.0-1.x86_64.rpm"
  action :run
  only_if {%x(yum list installed | grep percona-release) !~ /percona-release/}
end

execute "remove-community-mysql-client" do
  command "yum remove -y MySQL-client-community"
  action :run
  only_if {%x(yum list installed | grep MySQL-client-community) =~ /installed/}
end

old_major_version = "55"
major_version = "56"
version = "5.6.17-rel65.0"
system = 'el6'

ruby_block "break-if-fast-checksum-enabled" do
  block do
    throw "You must disable innodb_fast_checksum before upgrading - this means an export/import of the tables!"
  end
  action :create
  only_if {%x(egrep innodb_fast_checksum /etc/my.cnf) =~ /innodb_fast_checksum/}
end

execute "remove-old-percona-server" do
  command "rpm -qa | grep Percona-Server | xargs rpm -e --nodeps"
  action :run
  only_if {old_major_version != major_version && %x(yum list installed | egrep 'Percona-Server-.*-#{old_major_version}') =~ /Percona-Server-/}
end

package "Percona-Server-shared-#{major_version}" do
  version "#{version}.#{system}"
  action :install
end

package "Percona-Server-devel-#{major_version}" do
  version "#{version}.#{system}"
  action :install
end

package "Percona-Server-client-#{major_version}" do
  version "#{version}.#{system}"
  action :install
end
