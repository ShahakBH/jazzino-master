cookbook_file "/etc/bashrc" do
  source "etc_bashrc"
  owner "root"
  group "root"
  mode 0644
end

cookbook_file "/etc/bashrc.default" do
  source "bashrc.default"
  owner "root"
  group "root"
  mode 0644
end

cookbook_file "/etc/bash_profile.default" do
  source "bash_profile.default"
  owner "root"
  group "root"
  mode 0644
end

cookbook_file "/etc/profile.d/prompt.sh" do
  source "default_prompt.sh"
  owner "root"
  group "root"
  mode 0755
end

cookbook_file "/etc/profile.d/tooloptions.sh" do
  source "tooloptions.sh"
  owner "root"
  group "root"
  mode 0755
end

file "/bin/bashlogger.sh" do
  action :delete
  only_if {File.exists?("/bin/bashlogger.sh")}
end
