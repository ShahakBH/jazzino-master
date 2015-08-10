set_unless[:firewall] = true
set_unless[:ssh][:allowed_clients] = ["151.237.239.34"]
set_unless[:internal_interface] = ""
set_unless[:monitoring][:email] = "monitoring@yazino.com"
set_unless[:monitoring][:suppress] = false
set_unless[:security][:denyhosts] = true
set_unless[:disk][:buffer][:ra] = {} # device => size
set_unless[:disk][:mount_options] = { "/" => "defaults,noatime" } # mount point => options
set_unless[:disk][:io_scheduler] = {} # device => scheduler; CentOS default is cfq