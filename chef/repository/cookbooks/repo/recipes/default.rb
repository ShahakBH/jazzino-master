File.delete("/etc/yum.repos.d/signtechno.repo") if File.exists?("/etc/yum.repos.d/signtechno.repo")
File.delete("/etc/yum.repos.d/dag.repo") if File.exists?("/etc/yum.repos.d/dag.repo")

repo_source = "yazino-yum-6.repo"

cookbook_file "/etc/yum.repos.d/yazino-yum.repo" do
  source repo_source
  mode "0644"
  owner "root"
  group "root"
end
