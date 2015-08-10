#!/bin/bash
# run with mysql as root on server db

# Grant strataprod user permission to read user-related tables in strataproddw database
# (required for BackOffice Web authentication)
#
# If given an argument it will be used as the password for the default user

DBPWD="strataprod"

if [ -n "$1" ]; then
    DBPWD="$1"
fi

mysql -u root <<EOF

grant select, insert, update, delete on strataproddw.USER to 'strataprod' identified by '${DBPWD}';
grant select, insert, update, delete on strataproddw.USER to 'strataprod'@'%' identified by '${DBPWD}';
grant select, insert, update, delete on strataproddw.USER to 'strataprod'@'localhost' identified by '${DBPWD}';

grant select, insert, update, delete on strataproddw.ROLE to 'strataprod' identified by '${DBPWD}';
grant select, insert, update, delete on strataproddw.ROLE to 'strataprod'@'%' identified by '${DBPWD}';
grant select, insert, update, delete on strataproddw.ROLE to 'strataprod'@'localhost' identified by '${DBPWD}';

grant select, insert, update, delete on strataproddw.ROLE_PEOPLE to 'strataprod' identified by '${DBPWD}';
grant select, insert, update, delete on strataproddw.ROLE_PEOPLE to 'strataprod'@'%' identified by '${DBPWD}';
grant select, insert, update, delete on strataproddw.ROLE_PEOPLE to 'strataprod'@'localhost' identified by '${DBPWD}';

grant select, insert, update, delete on strataproddw.PARTNER_USERS to 'strataprod' identified by '${DBPWD}';
grant select, insert, update, delete on strataproddw.PARTNER_USERS to 'strataprod'@'%' identified by '${DBPWD}';
grant select, insert, update, delete on strataproddw.PARTNER_USERS to 'strataprod'@'localhost' identified by '${DBPWD}';

grant select, insert, update, delete on strataproddw.REQUESTMAP to 'strataprod' identified by '${DBPWD}';
grant select, insert, update, delete on strataproddw.REQUESTMAP to 'strataprod'@'%' identified by '${DBPWD}';
grant select, insert, update, delete on strataproddw.REQUESTMAP to 'strataprod'@'localhost' identified by '${DBPWD}';

flush privileges;
EOF

if [ $? -ne 0 ]; then
    echo "Failed to grant permissions."
else
    echo "Permissions granted."
    echo "* DB       = strataproddw"
    echo "* User     = strataprod"
    echo "* Password = $DBPWD"
    echo "User has CRUD privileges on strataproddw tables (USER, ROLE, ROLE_PEOPLE, PARTNER_USERS, REQUESTMAP)."
fi
