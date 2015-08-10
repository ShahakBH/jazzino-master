set_unless[:firewall] = true
set_unless[:httpd][:content][:port] = 8188
set_unless[:httpd][:content][:ssl_port] = 8143
set_unless[:httpd][:content][:directory] = "/home/content/public"
set_unless[:httpd][:content][:ssl_cert_directory] = "/home/content/ssl-certs"
set_unless[:httpd][:content][:username] = "content"
set_unless[:httpd][:content][:password] = "A9MUzFE|7OaI*h"

