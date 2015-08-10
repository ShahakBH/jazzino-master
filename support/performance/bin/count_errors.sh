#!/bin/bash

BIN_DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

source $BIN_DIR/config.sh

echo "Counting error logs on each agent"
for NODE in $AGENTS; do
    echo "$NODE:"
    ssh $SSH_OPTS root@$NODE "find /tmp/grinder/* | grep -c error"
done
