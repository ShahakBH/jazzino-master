set_unless[:haproxy][:lobby][:listen_ipv4] = "37.59.33.217"
set_unless[:haproxy][:lobby][:listen_ipv6] = "2001:41d0:8:48d9::1"
set_unless[:haproxy][:lobby][:servers] = {'ovh-mmc-lobby1' => 'ovh-mmc-lobby1.local.makemycasino.com', 'ovh-mmc-lobby2' => 'ovh-mmc-lobby2.local.makemycasino.com'}
set_unless[:haproxy][:mobile][:listen_ipv4] = "94.23.120.33"
set_unless[:haproxy][:mobile][:listen_ipv6] = "2001:41d0:8:48d9::2"
set_unless[:haproxy][:mobile][:servers] = {'ovh-mmc-mobile1' => 'ovh-mmc-mobile1.local.makemycasino.com', 'ovh-mmc-mobile2' => 'ovh-mmc-mobile2.local.makemycasino.com'}
set_unless[:haproxy][:worker][:listen_ipv4] = "192.168.1.11"
set_unless[:haproxy][:worker][:listen_ipv6] = "2001:41d0:8:48d9::3"
set_unless[:haproxy][:worker][:servers] = {'ovh-mmc-worker1-local' => 'ovh-mmc-worker1.local.makemycasino.com', 'ovh-mmc-worker2-local' => 'ovh-mmc-worker2.local.makemycasino.com'}
