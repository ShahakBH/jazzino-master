package "httpd" do
  action :install
end

package "mod_ssl" do
  action :install
end

execute "boot_httpd" do
  command "/sbin/chkconfig  httpd  on --level 235"
  action :run
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

password_hash = %x(openssl passwd -1 '#{node[:httpd][:content][:password]}').strip

# We need ruby-shadow for all but the first run with user resoures, hence we do the user add manually
execute "add-content-user" do
  command "/usr/sbin/useradd -p '#{password_hash}' -G contentuser #{node[:httpd][:content][:username]}"
  action :run
  only_if {%x(grep #{node[:httpd][:content][:username]} /etc/passwd).strip.length == 0}
end

execute "make-content-user-public" do
  command "chmod +rx /home/content"
  action :run
end

directory node[:httpd][:content][:directory] do
  owner node[:httpd][:content][:username]
  group "contentuser"
  mode 0775
  action :create
end

directory node[:httpd][:content][:ssl_cert_directory] do
  owner node[:httpd][:content][:username]
  group "contentuser"
  mode 0755
  action :create
end

cookbook_file "#{node[:httpd][:content][:ssl_cert_directory]}/apache.crt" do
  action :create_if_missing
  source "apache.crt"
  mode 0644
  owner "apache"
  group "apache"
end

execute "httpd_selinux_certificate" do
  command "chcon -R -t cert_t #{node[:httpd][:content][:ssl_cert_directory]}/apache.crt"
  action :run
  only_if {system("/usr/sbin/selinuxenabled")}
end

cookbook_file "#{node[:httpd][:content][:ssl_cert_directory]}/apache.key" do
  action :create_if_missing
  source "apache.key"
  mode 0600
  owner "apache"
  group "apache"
end

execute "httpd_selinux_key" do
  command "chcon -R -t cert_t #{node[:httpd][:content][:ssl_cert_directory]}/apache.key"
  action :run
  only_if {system("/usr/sbin/selinuxenabled")}
end

cookbook_file "#{node[:httpd][:content][:ssl_cert_directory]}/apache-bundle.crt" do
  action :create_if_missing
  source "apache-bundle.crt"
  mode 0644
  owner "apache"
  group "apache"
end

execute "httpd_selinux_bundle" do
  command "chcon -R -t cert_t #{node[:httpd][:content][:ssl_cert_directory]}/apache-bundle.crt"
  action :run
  only_if {system("/usr/sbin/selinuxenabled")}
end

if File.exists?("/usr/sbin/semanage")
  selinux_status = `sudo /usr/sbin/semanage port -l | grep http_port_t | grep -e ' #{node[:httpd][:content][:port]}[, ]'`
  execute "content-selinux-rules-http" do
    command "/usr/sbin/semanage port -a -t http_port_t -p tcp #{node[:httpd][:content][:port]}"
    action :run
    only_if {(selinux_status =~ /^http_port_t/).nil?}
  end
end

if File.exists?("/usr/sbin/semanage")
  selinux_status = `sudo /usr/sbin/semanage port -l | grep http_port_t | grep -e ' #{node[:httpd][:content][:ssl_port]}[, ]'`
  execute "content-selinux-rules-ssl" do
    command "/usr/sbin/semanage port -a -t http_port_t -p tcp #{node[:httpd][:content][:ssl_port]}"
    action :run
    only_if {(selinux_status =~ /^http_port_t/).nil?}
  end
end

ruby_block "http-firewall-rules" do
  block do
    [node[:httpd][:port], node[:httpd][:ssl_port]].each do |port|
      while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':#{port}\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/iptables -D INPUT -p tcp --dport #{port} -j ACCEPT]
        break if $?.exitstatus != 0
      end
      while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':#{port}\s*$' | wc -l].strip.to_i > 0
        %x[/sbin/ip6tables -D INPUT -p tcp --dport #{port} -j ACCEPT]
        break if $?.exitstatus != 0
      end
      %x[/sbin/iptables -I INPUT -p tcp --dport #{port} -j ACCEPT]
      %x[/sbin/ip6tables -I INPUT -p tcp --dport #{port} -j ACCEPT]
    end
    %x[/sbin/service iptables save]
    %x[/sbin/service ip6tables save]
  end
  action :create
  only_if {node[:firewall]}
end

cookbook_file "/var/www/error/noindex.html" do
  source "index.html"
  mode 0644
  owner "root"
  group "root"
end

execute "remove-default-icons-README" do # OSVDB-3233
  command "rm /var/www/icons/README"
  action :run
  only_if {File.exists?('/var/www/icons/README')}
end

execute "remove-default-icons-README.html" do # OSVDB-3233
  command "rm /var/www/icons/README.html"
  action :run
  only_if {File.exists?('/var/www/icons/README.html')}
end

execute "deactivate-directory-indexes" do # OSVDB-3268
  command 'sed -i\'\' -e \'s%\(\s\)*Options\(.*\) Indexes\(.*\)%\1Options\2\3%g\' /etc/httpd/conf/httpd.conf'
  action :run
  only_if {%x[grep -c -e '.*Options.*Indexes.*' /etc/httpd/conf/httpd.conf].strip != '0'}
end

execute "disable-http-trace" do # OSVDB-877
  command "echo 'TraceEnable off' >> /etc/httpd/conf/httpd.conf"
  action :run
  only_if {%x[grep -c -e 'TraceEnable off' /etc/httpd/conf/httpd.conf].strip == '0'}
end

if File.exists?("/usr/sbin/semanage")
  selinux_status = `sudo /usr/sbin/semanage port -l | grep http_port_t | grep -e ' #{node[:httpd][:port]}[, ]'`
  execute "selinux-rules" do
    command "/usr/sbin/semanage port -a -t http_port_t -p tcp #{node[:httpd][:port]}"
    action :run
    only_if {(selinux_status =~ /^http_port_t/).nil?}
  end
end

execute "disable_default_ssl_listen_ports" do
  command "sed -i -r 's/^Listen (.*)$/# Listen \\1/g' /etc/httpd/conf.d/ssl.conf"
  action :run
  only_if {%x(grep '^Listen' /etc/httpd/conf.d/ssl.conf) =~ /Listen/}
end
