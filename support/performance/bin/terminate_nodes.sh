#!/bin/bash

BIN_DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

source $BIN_DIR/config.sh

if [ ! -f "$NODES_FILE" ]; then
    echo "Cannot find nodes file: $NODES_FILE"
    exit 1
fi

HOSTS=( $(cat $NODES_FILE) )

for HOST in ${HOSTS[@]}; do
    (
        INSTANCE_ID=$($AWS din --filter dns-name=$HOST --region $EC2_REGION | grep $HOST | awk '{ print $2 }')
        $AWS tin $INSTANCE_ID --region $EC2_REGION
    ) &
done
wait

rm $NODES_FILE
