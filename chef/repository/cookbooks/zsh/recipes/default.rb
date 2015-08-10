package "zsh" do
  action :install
end

cookbook_file "/home/spanner/.zshrc" do
  source "zshrc"
  owner "spanner"
  group "spanner"
  mode 0755
end

cookbook_file "/root/.zshrc" do
  source "zshrc"
  owner "root"
  group "root"
  mode 0755
end

execute "change-shell-for-spanner-to-zsh" do
  command "chsh -s /bin/zsh spanner"
  action :run
  only_if {%x(echo $SHELL) != '/bin/zsh'}
end
