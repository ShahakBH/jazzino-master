package "ucarp" do
  action :install
end

node[:ucarp][:vips].each_pair do |vip_no_str, carp_attrs|

  carp_if_str = carp_attrs[:interface]
  
  if carp_if_str.nil?
    carp_if_str = node[:ucarp][:interface]
  end
  
  template "/etc/sysconfig/carp/vip-#{vip_no_str}.conf" do
    source "vip.erb"
    owner "root"
    group "root"
    mode "0644"
    variables(
  		:carp_if => carp_if_str,
  		:vip => carp_attrs[:vip],
  		:vip_half => carp_attrs[:vip].split('.')[2] + '.' + carp_attrs[:vip].split('.')[3],
  		:vip_no => vip_no_str,
  		:advbase => node[:ucarp]["#{carp_attrs[:role]}"][:advbase],
  		:advskew => node[:ucarp]["#{carp_attrs[:role]}"][:advskew]
  	 )
  end
  
  template "/etc/sysconfig/network-scripts/ifcfg-#{carp_if_str}:#{vip_no_str}" do
    source "ifcfg.erb"
    owner "root"
    group "root"
    mode "0644"
    variables(
  		:carp_if => carp_if_str,
  		:vip => carp_attrs[:vip],
  		:vip_no => vip_no_str
  	 )
  end
end
  
service "carp" do
  action [:enable, :restart]
end

