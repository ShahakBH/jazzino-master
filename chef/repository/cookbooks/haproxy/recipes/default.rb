yum_package "haproxy" do
  action :install
end

directory "/etc/haproxy" do
  owner "root"
  group "root"
  mode 0755
  action :create
end

cookbook_file "/usr/local/sbin/hatop" do
  action :create
  source "sbin_hatop"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "/usr/local/bin/hatop" do
  action :create
  source "hatop"
  mode 0755
  owner "root"
  group "root"
end

cookbook_file "/usr/local/share/man/man1/hatop.1" do
  action :create
  source "hatop.1"
  mode 0644
  owner "root"
  group "root"
end

execute "prepare hatop man" do
  command "gzip /usr/local/share/man/man1/hatop.1"
  action :run
  only_if {File.exists?("/usr/local/share/man/man1/hatop.1") && !File.exists?("/usr/local/share/man/man1/hatop.1.gz")}
end
