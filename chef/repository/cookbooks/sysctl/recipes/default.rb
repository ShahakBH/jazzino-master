template "/etc/sysctl.conf" do
  source "sysctl.conf.erb"
  mode 0644
  owner "root"
  group "root"
  variables(
    :keepalive_time => node[:sysctl][:keepalive_time],
    :keepalive_interval => node[:sysctl][:keepalive_interval],
    :close_wait => node[:sysctl][:close_wait],
    :fin_wait => node[:sysctl][:fin_wait],
    :syn_recv => node[:sysctl][:syn_recv],
    :syn_sent => node[:sysctl][:syn_sent],
    :time_wait => node[:sysctl][:time_wait],
    :max_connections => node[:sysctl][:max_connections],
    :firewall => node[:firewall],
    :vm => node[:vm],
    :swappiness => node[:sysctl][:swappiness]
  )
end

execute "set-swappiness" do
  command "sysctl vm.swappiness=#{node[:sysctl][:swappiness]}"
  action :run
end

execute "load_sysctl_settings" do
  command "sysctl -p"
  action :run
end
