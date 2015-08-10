#!/bin/bash
# run with mysql as root on server db

# Grant strataprod user permission to read user-related tables in strataproddw database
# (required for BackOffice Web authentication)
#
# If given an argument it will be used as the password for the default user

mysql -u root -p <<EOF

grant file on *.* to 'strataproddw';
grant file on *.* to 'strataproddw'@'%';
grant file on *.* to 'strataproddw'@'localhost';

flush privileges;
EOF

if [ $? -ne 0 ]; then
    echo "Failed to grant permissions."
else
    echo "Permissions granted."
fi
