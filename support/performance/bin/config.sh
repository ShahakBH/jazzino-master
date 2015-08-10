#!/bin/bash

BASE_DIR=$(cd -P -- "$(dirname -- "$0")/.." && pwd -P)

if [ ! -d "$BASE_DIR/etc" ]; then
    mkdir $BASE_DIR/etc
fi

NODES_FILE="$BASE_DIR/etc/nodes.txt"
if [ -f "$NODES_FILE" ]; then
    NODES=$(cat $NODES_FILE)
    if [ -n "$NODES" ]; then
        CONSOLE=$(head -1 $NODES_FILE)
        AGENTS=`grep -v $CONSOLE $NODES_FILE`
    fi
fi

GRINDER_VERSION="3.11"
JAVA_RPM="http://yum.london.yazino.com/centos/6/x86_64/oracle-jdk-1.8.0_05-1.x86_64.rpm"
LOCAL_WORKING_DIR="$BASE_DIR"
REMOTE_WORKING_DIR="/root/grinder"
FULL_ARCHIVE="/tmp/grinder.tgz"
GRINDER_CONF="$BASE_DIR/etc/grinder.properties"

# We're using the AWS script, as ec2-api-tools is rubbish
# See http://www.timkay.com/aws/ or https://github.com/timkay/aws
SECRETS_FILE="$BASE_DIR/etc/awssecret"
if [ ! -f "$SECRETS_FILE" ]; then
    echo "Cannot find secret file: $SECRETS_FILE"
    exit 1
fi
chmod 600 $SECRETS_FILE

HOST_SSH_KEY=$BASE_DIR/keys/deployment-key
if [ ! -f "$HOST_SSH_KEY" ]; then
    echo "Cannot find deployment key: $HOST_SSH_KEY"
    exit 1
fi
chmod 600 $HOST_SSH_KEY

export AWS="$BASE_DIR/bin/aws --secrets-file=$SECRETS_FILE"
EC2_REGION="$(grep yazino.agent-region $GRINDER_CONF | awk '{print $3}')"
EC2_INSTANCE_TYPE="m1.large"

if [ "$EC2_REGION" == "eu-west-1" ]; then
    AMI_ID="ami-0ca7a878" # rightscale-eu/RightImage_CentOS_6.3_x64_v5.8.8.8.manifest.xml
    EC2_KEYPAIR="signature_testing_eu"
    SSH_KEY="$BASE_DIR/keys/yazino"
elif [ "$EC2_REGION" == "us-west-1" ]; then
    AMI_ID="ami-4f0b8026" # rightscale-us-east/RightImage_CentOS_6.3_x64_v5.8.8.8.manifest.xml
    EC2_KEYPAIR="yazino_testing_us"
    SSH_KEY="$BASE_DIR/keys/yazino_testing_us"
else
    echo "Unsupported region: $EC2_REGION"
    exit 1
fi

chmod 600 $SSH_KEY
SSH_OPTS="-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o LogLevel=ERROR -i $SSH_KEY"

HOST_SSH_OPTS="-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o LogLevel=ERROR -i $HOST_SSH_KEY -p 22"
HOST_SCP_OPTS="-o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o LogLevel=ERROR -i $HOST_SSH_KEY -P 22"
