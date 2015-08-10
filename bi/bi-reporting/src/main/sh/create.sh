#!/bin/bash

# If given an argument it will be used as the password for the default user

DBNAME="reporting"
DBOWNER="reporting"
DBPWD="S1gn4tur3"

echo "If you see failures below, rerun the script as the postgres user."

if [ -x /usr/local/bin/psql ]; then
    PSQL_PREFIX=/usr/local/bin
elif [ -x /usr/pgsql-9.3/bin/psql ]; then
    PSQL_PREFIX=/usr/pgsql-9.3/bin
else
    PSQL_PREFIX=$(dirname `which psql`)
fi

if [ ! -x "$PSQL_PREFIX/psql" ]; then
 echo "! Cannot find psql; exiting..."
 exit 1
fi

export PGPORT="5432"

 if [ -n "$1" ]; then
     DBPWD="$1"
 fi

 if [ -z $(which psql) ]; then
     echo "! Cannot find psql; exiting..."
     exit 1
 fi

 # add pgpass stuff
 # going to have to sort out this user stuff later
echo "Checking to see if DB already exists ..."

echo "Using PSQL from $PSQL_PREFIX"

if ! $PSQL_PREFIX/psql -dtemplate1 -c'SELECT datname FROM pg_database' 1>/dev/null 2>&1; then
    echo "Failed to invoke psql - if the build fails, please correct this. If it passes it is probably due to postgresql perms."
    exit 0
fi

if [ $($PSQL_PREFIX/psql -dtemplate1 -c"SELECT datname FROM pg_database;" | grep -c "$DBNAME") = 0 ]; then
    echo "* Database $DBNAME does not exist; creating...";
    $PSQL_PREFIX/createdb $DBNAME
    echo "adding changelog table to $DBNAME...";
    $PSQL_PREFIX/psql -d$DBNAME <<EOF
        CREATE TABLE changelog (
        change_number INTEGER NOT NULL,
        complete_dt TIMESTAMP NOT NULL,
        applied_by VARCHAR(100) NOT NULL,
        description VARCHAR(500) NOT NULL
        );

        ALTER TABLE changelog ADD CONSTRAINT Pkchangelog PRIMARY KEY (change_number)
EOF
     if [ $? -ne 0 ]; then
         echo "! DB creation failed."
         exit 1
     fi
 
     echo "* Updating privileges"

 $PSQL_PREFIX/psql -d$DBNAME <<EOF
    CREATE GROUP SCHEMA_MANAGER;
    CREATE GROUP READ_WRITE;
    CREATE GROUP READ_ONLY;

    CREATE USER reporting IN GROUP SCHEMA_MANAGER PASSWORD 'reporting';
    CREATE USER admin IN GROUP SCHEMA_MANAGER PASSWORD 'admin';
    CREATE USER readwrite IN GROUP READ_WRITE PASSWORD 'readwrite';
    CREATE USER readonly IN GROUP READ_ONLY PASSWORD 'readonly';

    GRANT ALL PRIVILEGES ON DATABASE reporting to SCHEMA_MANAGER;
    GRANT ALL PRIVILEGES ON SCHEMA public to SCHEMA_MANAGER;
    GRANT ALL PRIVILEGES ON changelog TO SCHEMA_MANAGER;

    Alter TABLE changelog owner to admin;
EOF
 
if [ $? -ne 0 ]; then
     echo "! Privilege update failed."
     exit 1
 else
     echo "* DB creation/update complete."
 fi
else
     echo "* Database $DBNAME already exists; no changes will be made."
fi
