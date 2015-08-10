set_unless[:firewall] = true

set_unless[:lightstreamer][:version] = "5.1.1b1623"
set_unless[:lightstreamer][:short_version] = "#{node[:lightstreamer][:version]}".gsub(/b.*$/,"")

set_unless[:lightstreamer][:client_id] = "Yazino"
set_unless[:lightstreamer][:license_path] = "Yazino_DEMO_AllegroPlus.lic"
set_unless[:lightstreamer][:http_port] = "8090"

#5.1.1
set_unless[:lightstreamer][:license_type] = "DEMO"
set_unless[:lightstreamer][:edition] = "Allegro"
set_unless[:lightstreamer][:java][:min_mem] = "64m"
set_unless[:lightstreamer][:java][:max_mem] = "512m"
set_unless[:java7][:home] = '/opt/java'
set_unless[:lightstreamer][:log_dir] = "/var/log/lightstreamer"

set_unless[:lightstreamer][:user] = "lightstreamer"
set_unless[:lightstreamer][:group] = "lightstreamer"

set_unless[:lightstreamer][:https] = true
set_unless[:lightstreamer][:https_port] = "8091"
set_unless[:lightstreamer][:https_keystore][:path] = "/opt/lightstreamer/conf/keystore"
set_unless[:lightstreamer][:https_keystore][:password] = "s1gn4tur3"

set_unless[:lightstreamer][:web][:enabled] = false,

set_unless[:lightstreamer][:monitor][:public] = false
set_unless[:lightstreamer][:monitor][:username] = "admin"
set_unless[:lightstreamer][:monitor][:password] = "LIGHT!s1gn4tur3"
