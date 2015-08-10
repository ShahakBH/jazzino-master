set_unless[:firewall] = true

if node[:environment] =~ /makemycasino/
    set_unless[:mysql][:db] = "strataprod"
    set_unless[:mysql][:role] = "standalone"
    set_unless[:mysql][:root_password] = nil
    set_unless[:mysql][:datadir] = "/var/lib/mysql/database/"
    set_unless[:mysql][:backupdir] = "/data/backups/mysql/"
    set_unless[:mysql][:binlog_dir] = "/var/lib/mysql-bin-logs/mysql-bin"
    set_unless[:mysql][:replication][:username] = "repl"
    set_unless[:mysql][:replication][:password] = "MySQL_r3pl1c4t!on"
    set_unless[:mysql][:tunable][:max_connections] = "1500"
    set_unless[:mysql][:tunable][:max_allowed_packet] = "16M"
    set_unless[:mysql][:tunable][:innodb_buffer_pool_size] = "256MB"
    set_unless[:mysql][:tunable][:innodb_log_file_size] = "256MB"
    set_unless[:mysql][:tunable][:innodb_log_buffer_size] = "8MB"
    set_unless[:mysql][:tunable][:thread_cache_size] = 64
    set_unless[:mysql][:tunable][:table_open_cache] = 1024
else
    set_unless[:mysql][:db] = "strataprod"
    set_unless[:mysql][:role] = "standalone"
    set_unless[:mysql][:root_password] = nil
    set_unless[:mysql][:datadir] = "/var/lib/mysql"
    set_unless[:mysql][:backupdir] = "/data/backups/mysql/"
    set_unless[:mysql][:binlog_dir] = "/var/lib/mysql-log-bin/mysql-bin"
    set_unless[:mysql][:replication][:username] = "repl"
    set_unless[:mysql][:replication][:password] = "MySQL_r3pl1c4t!on"
    set_unless[:mysql][:tunable][:max_connections] = "1500"
    set_unless[:mysql][:tunable][:max_allowed_packet] = "16M"
    set_unless[:mysql][:tunable][:innodb_buffer_pool_size] = "256MB"
    set_unless[:mysql][:tunable][:innodb_log_file_size] = "256MB"
    set_unless[:mysql][:tunable][:innodb_log_buffer_size] = "8MB"
    set_unless[:mysql][:tunable][:thread_cache_size] = 64
    set_unless[:mysql][:tunable][:table_open_cache] = 1024
end
