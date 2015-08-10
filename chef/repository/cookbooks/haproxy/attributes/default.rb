# IP ranges for the many, many Edgecast CDNs can be found on my.edgecast.com (Customer Origin -> Check Origin DNS)
set_unless[:haproxy][:whitelist] = ['151.237.239.34', # London Office
                                    '80.45.134.109', # dvujnovic home
                                    '174.143.223.1', '174.143.223.15', '174.143.223.4', '72.32.145.29', '72.32.75.206', # Zong callback servers
                                    '5.104.71.0/24', '46.22.74.0/23', '72.21.90.0/24', '93.184.210.0/24', '93.184.211.0/24', # Edgecast London CDNs
                                    '93.184.215.0/24', '93.184.216.0/24', '93.184.219.0/24', '93.184.220.0/24', '93.184.221.0/24', '93.184.222.0/24', '93.184.223.0/24'] #Edgecast Other CDNs (subset)
