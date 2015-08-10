set_unless[:firewall] = true
set_unless[:httpd][:port] = 8180
set_unless[:httpd][:ssl_port] = 8183
set_unless[:httpd][:ssl_cert_directory] = "/home/content/ssl-certs"
set_unless[:jetty][:port] = 7900
set_unless[:jetty][:ssl_port] = 7943
