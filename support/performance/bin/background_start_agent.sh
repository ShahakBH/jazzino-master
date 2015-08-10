#!/bin/bash

BIN_DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

source $BIN_DIR/config.sh

LIB_DIR=$BASE_DIR
CLASSPATH="lib:lib/json_simple-1.1.jar:lib/mysql-connector-java-5.1.13.jar:$REMOTE_WORKING_DIR/grinder/lib/grinder.jar"
ssh $SSH_OPTS root@$NODE "killall java &>/dev/null"
ssh $SSH_OPTS root@$NODE "nohup bash $REMOTE_WORKING_DIR/bin/remote/launchAgent.sh &> output.txt &"
if [ $? -ne 0 ]; then
    echo "Error starting remote agent"
fi
