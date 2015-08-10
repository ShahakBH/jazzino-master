#!/bin/bash

BIN_DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

source $BIN_DIR/config.sh

echo "Starting Remote Agents"
for NODE in $AGENTS; do
    . $BIN_DIR/background_start_agent.sh &
done

wait

echo "Starting Remote Console"
ssh $SSH_OPTS root@$CONSOLE "killall java &>/dev/null"
ssh -Y $SSH_OPTS root@$CONSOLE "cd $REMOTE_WORKING_DIR && bin/remote/launchConsole.sh"
