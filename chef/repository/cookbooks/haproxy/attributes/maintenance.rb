set_unless[:haproxy][:maintenance][:http][:port] = 9180
set_unless[:haproxy][:maintenance][:http][:redirect_port] = 8180
set_unless[:haproxy][:maintenance][:http][:whitelist_port] = 7900
set_unless[:haproxy][:maintenance][:https][:port] = 9183
set_unless[:haproxy][:maintenance][:https][:redirect_port] = 8183
set_unless[:haproxy][:maintenance][:https][:whitelist_port] = 7943
