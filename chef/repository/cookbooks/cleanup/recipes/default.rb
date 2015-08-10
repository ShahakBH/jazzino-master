# Clean up obsolete web apps
execute "clean-up-old-webapps" do
  command "cd /opt/jetty/webapps && rm -f web-opengraph-worker.war game-server.war "
  action :run
  only_if {File.exists?('/opt/jetty/webapps')}
end
