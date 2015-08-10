#Template variable names should follow the names of the nested xml elements.

directory node[:lightstreamer][:log_dir] do
  owner node[:lightstreamer][:user]
  group node[:lightstreamer][:group]
  mode 0755
  action :create
end

template "/opt/lightstreamer/conf/lightstreamer_conf.xml" do
  source "#{node[:lightstreamer][:short_version]}/lightstreamer_conf.xml.erb"
  owner node[:lightstreamer][:user]
  group node[:lightstreamer][:group]
  mode "0644"
  variables(
    :http_server_port => node[:lightstreamer][:http_port],
    :https_server_port => node[:lightstreamer][:https_port],
    :https_active => node[:lightstreamer][:https],
    :keystore => node[:lightstreamer][:https_keystore][:path],
    :keystore_password => node[:lightstreamer][:https_keystore][:password],
    :web_server_enabled => node[:lightstreamer][:web][:enabled],
    :monitor_provider_public => node[:lightstreamer][:monitor][:public],
    :monitor_username => node[:lightstreamer][:monitor][:username],
    :monitor_password => node[:lightstreamer][:monitor][:password]
  )
end

template "/opt/lightstreamer/conf/lightstreamer_version_conf.xml" do
  source "#{node[:lightstreamer][:short_version]}/lightstreamer_version_conf.xml.erb"
  owner node[:lightstreamer][:user]
  group node[:lightstreamer][:group]
  mode "0644"
  variables(
    :license__type => node[:lightstreamer][:license_type],
    :license__demo__edition => node[:lightstreamer][:edition],
    :license_file__client_id => node[:lightstreamer][:client_id],
    :license_file__license_path => node[:lightstreamer][:license_path]
  )
end

template "/opt/lightstreamer/conf/lightstreamer_log_conf.xml" do
  source "#{node[:lightstreamer][:short_version]}/lightstreamer_log_conf.xml.erb"
  owner node[:lightstreamer][:user]
  group node[:lightstreamer][:group]
  mode "0644"
  variables(
    :log_dir => node[:lightstreamer][:log_dir]
  )
end

template "/opt/lightstreamer/bin/unix-like/LS.sh" do
  source "#{node[:lightstreamer][:short_version]}/LS.sh.erb"
  owner node[:lightstreamer][:user]
  group node[:lightstreamer][:group]
  mode "0644"
  variables(
    :java_opts => "-server -XX:+UseG1GC -Xmn#{node[:lightstreamer][:java][:min_mem]} -Xmx#{node[:lightstreamer][:java][:max_mem]}",
    :java_home => node[:java7][:home]
  )
end

cookbook_file "/etc/init.d/lightstreamer" do
  source "#{node[:lightstreamer][:short_version]}/lightstreamer"
  mode 0755
  owner "root"
  group "root"
end
