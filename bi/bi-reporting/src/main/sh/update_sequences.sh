#!/bin/bash

PSQL=psql
if [ -x /usr/local/bin/psql ]; then
    PSQL=/usr/local/bin/psql
fi

if [ -z "$2" ] ; then
    echo "USAGE: $0 [DATABASE] [USER]"
    exit 1
fi

DATABASE="$1"
USER="$2"

echo "Granting update rights to public sequences on $DATABASE to $USER"
$PSQL -U $USER -d $DATABASE -qAt -c "SELECT 'GRANT update ON ' || relname || ' TO $USER;' FROM pg_statio_all_sequences WHERE schemaname = 'public'" | $PSQL -U $USER -d $DATABASE
