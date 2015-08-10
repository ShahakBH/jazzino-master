#!/bin/bash

BASE_DIR=$(cd -P -- "$(dirname -- "$0")" >/dev/null && pwd -P)

source $BASE_DIR/config

echo "Extract *all* mobile players (note: at aelahmar's request this IGNORES the opt-in flag)"

TIMESTAMP=$(date +'%Y%d%mT%H%M%S')

EXTRACT_FILE="$HOME/Desktop/opted-in-ios-$TIMESTAMP.csv"
echo "* Running iOS extract to $EXTRACT_FILE"

mysql -A -h $DBDW_HOST -u $DBDW_USER -p$DBDW_PASSWORD -P$DBDW_PORT $DBDW_PRODSCHEMA > $EXTRACT_FILE.tmp <<EOF
SELECT DISTINCT PD.GAME_TYPE,
       PD.PLAYER_ID
  FROM IOS_PLAYER_DEVICE PD;
EOF
MYSQL_EXIT_CODE=$?

if [ $MYSQL_EXIT_CODE -eq 0 ]; then
    cat $EXTRACT_FILE.tmp | sed "s/[[:space:]]/,/g" > $EXTRACT_FILE
    rm -f $EXTRACT_FILE.tmp
    echo "* iOS extract; output can be found at $EXTRACT_FILE"
else
    echo "! iOS extract failed with error code $MYSQL_EXIT_CODE"
fi

EXTRACT_FILE="$HOME/Desktop/opted-in-android-$TIMESTAMP.csv"
echo "* Running Android extract to $EXTRACT_FILE"

mysql -A -h $DBDW_HOST -u $DBDW_USER -p$DBDW_PASSWORD -P$DBDW_PORT $DBDW_DBDWSCHEMA > $EXTRACT_FILE.tmp <<EOF
SELECT DISTINCT PD.GAME_TYPE,
       PD.PLAYER_ID
  FROM GCM_PLAYER_DEVICE PD;
EOF
MYSQL_EXIT_CODE=$?

if [ $MYSQL_EXIT_CODE -eq 0 ]; then
    cat $EXTRACT_FILE.tmp | sed "s/[[:space:]]/,/g" > $EXTRACT_FILE
    rm -f $EXTRACT_FILE.tmp
    echo "* Android extract; output can be found at $EXTRACT_FILE"
    exit 0
else
    echo "! Android extract failed with error code $MYSQL_EXIT_CODE"
    exit 1
fi
