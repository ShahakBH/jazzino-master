#!/bin/bash

# If given an argument it will be used as the password for the default user

DBNAME="strataprod"
DBUSER="strataprod"
DBPWD="strataprod"

NAGIOS_USER="nagios"
NAGIOS_PWD="nagios"

if [ -n "$1" ]; then
    DBPWD="$1"
fi

if [ -z $(which mysql) ]; then
    echo "! Cannot find mysql; exiting..."
    exit 1
fi

if [ $(mysql -u root mysql -e 'show databases\G' | grep -c "^Database: $DBNAME$") = 0 ]; then
    echo "* Database $DBNAME does not exist; creating..."

    mysql -u root <<EOF
create database $DBNAME character set latin1;
use ${DBNAME}
CREATE TABLE changelog (
    change_number BIGINT NOT NULL,
    delta_set VARCHAR(10) NOT NULL,
    start_dt TIMESTAMP NOT NULL,
    complete_dt TIMESTAMP NULL,
    applied_by VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL
) engine=InnoDB charset=utf8;
set global event_scheduler = 1;
EOF

    if [ $? -ne 0 ]; then
        echo "! DB creation failed."
        exit 1
    fi
else
    echo "* Database $DBNAME already exists; no changes will be made."
fi

echo "* Updating privileges"

mysql -u root <<EOF
grant all privileges on ${DBNAME}.* to '${DBUSER}' identified by '${DBPWD}';
grant all privileges on ${DBNAME}.* to '${DBUSER}'@'%' identified by '${DBPWD}';
grant all privileges on ${DBNAME}.* to '${DBUSER}'@'localhost' identified by '${DBPWD}';
GRANT SELECT, PROCESS, REPLICATION CLIENT ON *.* TO '${NAGIOS_USER}'@'%' IDENTIFIED BY '${NAGIOS_PWD}';
flush privileges;
EOF

if [ $? -ne 0 ]; then
    echo "! Privilege update failed."
    exit 1
else
    echo "* DB creation/update complete."
fi
