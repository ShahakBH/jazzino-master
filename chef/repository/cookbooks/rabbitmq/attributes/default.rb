set_unless[:firewall] = true
set_unless[:rabbitmq][:vhosts] = []
set_unless[:rabbitmq][:erlang_cookie] = 'RABBITCOOKIE'
set_unless[:rabbitmq][:heartbeat] = 0
set_unless[:rabbitmq][:cluster][:nodes] = []
set_unless[:rabbitmq][:cluster][:type] = 'disc' # 'ram' or 'disc'
set_unless[:rabbitmq][:cluster][:high_availability][:queues] = [] # regexes to queues to be run as HA
set_unless[:rabbitmq][:cluster][:high_availability][:sync_count] # number of nodes, or 'all'
set_unless[:rabbitmq][:cluster][:high_availability][:sync_type] = 'automatic' # 'automatic' or 'manual'
