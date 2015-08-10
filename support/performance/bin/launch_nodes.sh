#!/bin/bash

BIN_DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

source $BIN_DIR/config.sh

INSTANCE_COUNT=$1
if [ -z "$INSTANCE_COUNT" ]; then
    echo "No count supplied, using grinder.properties to determine one"
    NODE_COUNT="$(cat $GRINDER_CONF | grep -e '^yazino.agents' | awk '{print $NF}')"
    if [ -z "$NODE_COUNT" ]; then
        echo "Couldn't determine node count. Exiting."
        exit 1
    fi
    let INSTANCE_COUNT=NODE_COUNT+1
fi

echo "Creating $INSTANCE_COUNT nodes"

OUTPUT_FILE="$NODES_FILE"

INSTANCES=( `$AWS run-instances --region="$EC2_REGION" $AMI_ID -n $INSTANCE_COUNT -k $EC2_KEYPAIR -t "$EC2_INSTANCE_TYPE" | grep "$EC2_INSTANCE_TYPE" | awk '{print $2}'` )
if [ $? -ne 0 ]; then
    echo 'Instance creation failed. Exiting...'
    exit 1
fi

echo "Waiting for startup of instances: ${INSTANCES[@]}"

DNS_NAMES=()

for INSTANCE in ${INSTANCES[@]}; do
    STATUS="pending"
    while [ "$STATUS" == "pending" ]; do
        INSTANCE_STATE=$($AWS din --region="$EC2_REGION" $INSTANCE | grep "$EC2_INSTANCE_TYPE")
        STATUS=$(echo $INSTANCE_STATE | awk '{ print $7 }' | awk 'BEGIN {FS = "="}; {print $2}')
        if [ "$STATUS" == "pending" ]; then
            sleep 5
        fi
    done
    echo "$INSTANCE: $STATUS"

    if [ "$STATUS" != "terminated" ]; then
        DNS_NAMES[${#DNS_NAMES[*]}]=$(echo $INSTANCE_STATE | awk '{ print $11 }')
    fi
done

if [ -f $OUTPUT_FILE ]; then
    rm $OUTPUT_FILE
fi

for DNS_NAME in ${DNS_NAMES[@]}; do
    echo -e "$DNS_NAME" >> $OUTPUT_FILE
done

CONTROLLER_INTERNAL_DNS_NAME=$($AWS din --region="$EC2_REGION" ${INSTANCES[0]} | grep "$EC2_INSTANCE_TYPE" | awk '{print $9}' | tr -d '\n' | tr -d '\r')
echo ""
echo "Internal DNS name for ${INSTANCES[0]} is $CONTROLLER_INTERNAL_DNS_NAME"

REPLACE="grinder.consoleHost = $CONTROLLER_INTERNAL_DNS_NAME"
SED_SCRIPT="s%grinder\.consoleHost =.*$%$REPLACE%g"
sed -i'' -e "$SED_SCRIPT" "$GRINDER_CONF"
echo "Updated grinder.consoleHost in $GRINDER_CONF"

echo ""
echo "Created $INSTANCE_COUNT nodes and wrote DNS names to $OUTPUT_FILE"

echo "Giving nodes a moment to bring sshd up..."
sleep 10

echo "Uploading to nodes..."
$BIN_DIR/upload.sh -all

if [ $? -ne 0 ]; then
    echo "Upload failed."
else
    echo "Complete"
fi
