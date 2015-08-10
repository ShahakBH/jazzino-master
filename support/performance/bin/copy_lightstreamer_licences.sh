#!/bin/bash

# Copies the test LightStreamer licences to the remote hosts. These will expire after 210 minutes.

BIN_DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

source $BIN_DIR/config.sh

LICENCE_CONFIG="/opt/lightstreamer/conf/lightstreamer_version_conf.xml"

function print_usage_and_exit {
    echo "Usage: $0 [enable|disable]"
    exit 1
}

ACTION="$1"
if [ -z "$ACTION" ]; then
    print_usage_and_exit
fi

IFS=$','
for HOST in $(cat $GRINDER_CONF | grep -e '^lightstreamer.nodes' | awk '{print $3}'); do
    HOSTS=("${HOSTS[@]}" "$HOST")
done
unset IFS

for HOST in "${HOSTS[@]}"; do
    cd $BASE_DIR/etc/lightstreamer
    for LICENCE_FILE in $(ls Yazino_*); do
        if [ "$ACTION" == 'enable' ]; then
            echo "Copying licence file $LICENCE_FILE to $HOST"

            scp $HOST_SCP_OPTS $LICENCE_FILE spanner@$HOST:~/
            if [ $? -ne 0 ]; then
                echo "SCP copy failed with code $?"
                exit 1
            fi
            FILES="$()"
            ssh $HOST_SSH_OPTS spanner@$HOST "sudo mv ~/$LICENCE_FILE /opt/lightstreamer/conf/ && sudo sed -i'' -e 's%<type>DEMO</type>%<type>LICENSE_FILE</type>%g' $LICENCE_CONFIG && sudo sed -i'' -e 's%\(<license_path>.*</license_path>\)%\1 <license_path>$LICENCE_FILE</license_path>%g' $LICENCE_CONFIG"
            if [ $? -ne 0 ]; then
                echo "SSH command failed with code $?"
                exit 1
            fi

        elif [ "$ACTION" == 'disable' ]; then
            echo "Deleting licence file $LICENCE_FILE from $HOST"
            ssh $HOST_SSH_OPTS spanner@$HOST "sudo sed -i'' -e 's%\(<license_path>$LICENCE_FILE</license_path>\)%%g' $LICENCE_CONFIG && sudo sed -i'' -e 's%<type>LICENSE_FILE</type>%<type>DEMO</type>%g' $LICENCE_CONFIG && sudo rm -f /opt/lightstreamer/conf/$LICENCE_FILE"
            if [ $? -ne 0 ]; then
                echo "SSH command failed with code $?"
                exit 1
            fi

        else
            echo "Unknown action: $ACTION"
            print_usage_and_exit
        fi
    done

    ssh $HOST_SSH_OPTS spanner@$HOST 'sudo /sbin/service lightstreamer restart'
    if [ $? -ne 0 ]; then
        echo "Service restart failed with code $?"
        exit 1
    fi
done
