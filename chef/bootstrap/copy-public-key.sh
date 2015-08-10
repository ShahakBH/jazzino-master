#!/bin/bash

BASE_DIR=$(cd -P -- "$(dirname -- "$0")" >/dev/null && pwd -P)

SSH_USERNAME=root
SSH_PORT=22
SSH_OPTS="-q -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o LogLevel=ERROR"
SCP_OPTS="$SSH_OPTS"

DEPLOYMENT_KEY_PUB="$BASE_DIR/deployment-key.pub"

HOST=$1
if [ -z "$HOST" ]; then
    echo "Usage: $0 <hostname>"
    exit 1
fi

if [ ! -f "$DEPLOYMENT_KEY_PUB" ]; then
    echo "Cannot find deployment key public file: $DEPLOYMENT_KEY_PUB"
    exit 1
fi

scp -P $SSH_PORT $SCP_OPTS "$DEPLOYMENT_KEY_PUB" "$SSH_USERNAME@$HOST:~/public-key" || {
    echo "Copy of deployment key failed with error $?"
    exit 1
}


ssh -p $SSH_PORT $SSH_OPTS $SSH_USERNAME@$HOST 'if [ ! -d "~/.ssh" ]; then mkdir -p ~/.ssh; chmod 755 .ssh; fi && cat ~/public-key >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys && rm -f ~/public-key' || {
    echo "Setup of authorised key failed with error $?"
    exit 1
}

echo "Key has been authorised on host $HOST"
