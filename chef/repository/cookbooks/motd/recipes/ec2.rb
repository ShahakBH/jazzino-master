template "/etc/motd" do
  source "ec2.motd.erb"
  owner "root"
  group "root"
  mode 0644
end
