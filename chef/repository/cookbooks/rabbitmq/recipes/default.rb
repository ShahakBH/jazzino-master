# if you change the rabbit version, make sure you check the patched init script we're using below is still valid
rabbitmq_version = "3.2.4"
erlang_package = "esl-erlang"
erlang_version = "R16B-2"

yum_package "erlang-erts" do
  action :remove
end

yum_package erlang_package do
  version erlang_version
  action :install
  flush_cache [ :before, :after ]
end

if erlang_package == 'esl-erlang'
  yum_package 'esl-erlang-compat' do
    action :install
  end
end

execute "stop-rabbitmq-server" do
  command "service rabbitmq-server stop"
  ignore_failure true
  timeout 30
  only_if {File.exists?('/etc/init.d/rabbitmq-server')}
end

execute "really-stop-rabbitmq-server" do
  # Rabbit stop will sometimes hang, and in such cases a second stop will work =/
  command "service rabbitmq-server stop"
  ignore_failure true
  timeout 30
  only_if {File.exists?('/etc/init.d/rabbitmq-server')}
end

execute "clean_up_rabbitmq" do
  command "pkill -u rabbitmq rabbitmq; pkill -u rabbitmq epmd"
  returns [0, 1]
  action :run
  only_if 'getent passwd | grep rabbitmq'
end

yum_package "rabbitmq-server" do
  version "#{rabbitmq_version}-1"
  allow_downgrade true
  action :install
end

template "/var/lib/rabbitmq/.erlang.cookie" do
  source "doterlang.cookie.erb"
  owner "rabbitmq"
  group "rabbitmq"
  mode 0400
end

template "/etc/rabbitmq/rabbitmq-env.conf" do
  source "rabbitmq-env.conf.erb"
  owner "root"
  group "root"
  mode 0644
end

template "/etc/rabbitmq/rabbitmq.config" do
  source "rabbitmq.config.erb"
  owner "root"
  group "root"
  mode 0644
  variables(
    :heartbeat => node[:rabbitmq][:heartbeat],
    :cluster_nodes => node[:rabbitmq][:cluster][:nodes],
    :cluster_type => node[:rabbitmq][:cluster][:type]
  )
end

cookbook_file "/etc/rabbitmq/exchange-declarer.jar" do
  action :create
  source "exchange-declarer.jar"
  mode 0644
  owner "root"
  group "root"
end

cookbook_file "/etc/init.d/rabbitmq-server" do
  action :create
  source "rabbitmq-server"
  mode 0755
  owner "root"
  group "root"
end

template "/etc/rabbitmq/set_permissions.sh" do
  source "set_permissions.sh.erb"
  owner "root"
  group "root"
  mode 0755
  variables(
    :vhosts => node[:rabbitmq][:vhosts],
    :ha_queues => node[:rabbitmq][:cluster][:high_availability][:queues],
    :ha_sync_count => node[:rabbitmq][:cluster][:high_availability][:sync_count],
    :ha_sync_type => node[:rabbitmq][:cluster][:high_availability][:sync_type]
  )
end

ruby_block "rabbitmq-firewall-rules" do
  block do
    while %x[/sbin/iptables --list -n | grep ACCEPT | grep -e ':5672\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/iptables -D INPUT -p tcp --dport 5672 -j ACCEPT]
      break if $?.exitstatus != 0
    end
    while %x[/sbin/ip6tables --list -n | grep ACCEPT | grep -e ':5672\s*$' | wc -l].strip.to_i > 0
      %x[/sbin/ip6tables -D INPUT -p tcp --dport 5672 -j ACCEPT]
      break if $?.exitstatus != 0
    end
    %x[/sbin/iptables -I INPUT -p tcp --dport 5672 -j ACCEPT]
    %x[/sbin/service iptables save]
    %x[/sbin/ip6tables -I INPUT -p tcp --dport 5672 -j ACCEPT]
    %x[/sbin/service ip6tables save]
  end
  action :create
end

execute "start_rabbitmq" do
  command "/sbin/service rabbitmq-server start"
  action :run
end

execute "set_permissions" do
  command "/etc/rabbitmq/set_permissions.sh"
  action :run
end
