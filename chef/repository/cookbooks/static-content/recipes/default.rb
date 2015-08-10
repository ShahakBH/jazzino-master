link "/opt/jetty/webapps/marketing" do
  to "/var/marketing"
  action :create
  only_if {File.exists?('/var/marketing') && File.exists?('/opt/jetty/webapps')}
end

link "/opt/jetty/webapps/avatars" do
  action :delete
  only_if "test -L /opt/jetty/webapps/avatars"
end
