set_unless[:firewall] = true
set_unless[:nagios][:servers][:ipv4] = ['109.74.203.53', '212.71.255.65'] # monitoring.yazino.com, monitoring.breakmycasino.com
set_unless[:nagios][:servers][:ipv6] = ['2a01:7e00::f03c:91ff:fe6e:9cdb', '2a01:7e00::f03c:91ff:fe6e:bb80']
