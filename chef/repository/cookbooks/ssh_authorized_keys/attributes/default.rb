set_unless[:firewall] = true
set_unless[:ssh][:users]  = {'spanner' => 's1gn4tur3'}
set_unless[:ssh][:sudoers] = ['spanner']
set_unless[:ssh][:harden] = true
set_unless[:ssh][:port] = 5123
