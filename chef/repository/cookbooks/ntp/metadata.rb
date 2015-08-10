maintainer        "Yazino Technologies"
maintainer_email  "chef@signtechno.com"
license           "Apache 2.0"
description       "Installs and configures ntp as a client"
version           "0.1"
name			 "ntp"

%w{ centos }.each do |os|
  supports os
end

