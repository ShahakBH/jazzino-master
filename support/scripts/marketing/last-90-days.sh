#!/bin/bash

BASE_DIR=$(cd -P -- "$(dirname -- "$0")" >/dev/null && pwd -P)

source $BASE_DIR/config

echo "Extract Player information for activity in the last 90 days"

echo "* Checking environment"

if [ ! -f "$HOME/.pgpass" -o "$(grep $REDSHIFT_HOST:$REDSHIFT_PORT:$REDSHIFT_DB:$REDSHIFT_USER -c ~/.pgpass)" == "0" ]; then
    echo "$REDSHIFT_HOST:$REDSHIFT_PORT:$REDSHIFT_DB:$REDSHIFT_USER:$REDSHIFT_PASSWORD" >> ~/.pgpass
    chmod 600 ~/.pgpass
fi

EXTRACT_FILE="$HOME/Desktop/active_within_last_90_days-fb-$(date +'%Y%d%mT%H%M%S').csv"

echo "* Running DB extract to $EXTRACT_FILE"

psql -t -A -F',' -h $REDSHIFT_HOST -p $REDSHIFT_PORT -U $REDSHIFT_USER -w $REDSHIFT_DB -o $EXTRACT_FILE <<EOF
select distinct pad.game,
       pad.player_id,
       lu.external_id
  from player_activity_daily pad
  left join lobby_user lu
    on pad.player_id = lu.player_id
  where pad.activity_ts >= sysdate - interval '90 day'
    and pad.game <> ''
    and pad.game <> 'BINGO'
    and pad.game <> 'HISSTERIA'
    and lower(lu.provider_name) = 'facebook';
EOF

PSQL_EXIT_CODE=$?
if [ $PSQL_EXIT_CODE -eq 0 ]; then
    echo "* Complete; output can be found at $EXTRACT_FILE"
    exit 0
else
    echo "! psql query failed with error code $PSQL_EXIT_CODE"
    exit 1
fi
