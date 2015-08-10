#!/bin/bash

BIN_DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

PROPERTIES_ONLY="yes"
if [ "$1" = "-all" ]; then
    PROPERTIES_ONLY=""
fi

source $BIN_DIR/config.sh

if [ -z "$PROPERTIES_ONLY" ]; then
    echo "Creating archive"
    cd $LOCAL_WORKING_DIR
    if [ -h grinder ]; then
        rm -f grinder
    fi
    ln -sf grinder-$GRINDER_VERSION grinder
    tar czf $FULL_ARCHIVE ./*
    cd - > /dev/null
fi

echo "Uploading"

RETURN=1
for NODE in $NODES; do
    while [ $RETURN -ne 0 ]; do
        nc -z $NODE 22 1>/dev/null 2>&1
        RETURN=$?
        echo "Waiting for sshd on $NODE..."
        sleep 1;
    done

    echo -n "Checking for Java 8 on $NODE..."
    if [ "$(ssh $SSH_OPTS root@$CONSOLE 'if [ -d /opt/java8 ]; then echo PRESENT; else echo ABSENT; fi 2>/dev/null')" == 'ABSENT' ]; then
        echo "MISSING: Installing..."
        OUTPUT=$(ssh $SSH_OPTS root@$CONSOLE "rpm -Uvh $JAVA_RPM")
        if  [ $? -ne 0 ]; then
            echo "Couldn't install Java 8 on $NODE on $CONSOLE. Received: $OUTPUT"
        fi
    else
        echo "OK"
    fi

    . $BIN_DIR/background_upload.sh &

    RETURN=1
done

wait

echo "Checking xauth for console node (required to launch X11)"
ssh $SSH_OPTS root@$CONSOLE "which xauth &>/dev/null"
if [ $? -ne 0 ]; then
    echo "xauth not present. Installing..."
    ssh $SSH_OPTS root@$CONSOLE "yum install -y xauth &>/dev/null"
    if  [ $? -ne 0 ]; then
        echo "Couldn't install xauth. Check $CONSOLE manually for errors"
    fi
    ssh $SSH_OPTS root@$CONSOLE "sed -i'' -e 's%#X11Forwarding.*no%X11Forwarding yes%g' /etc/ssh/sshd_config && service sshd restart"
fi

echo -n "Checking for Java 8 on console node..."
if [ "$(ssh $SSH_OPTS root@$CONSOLE 'if [ -d /opt/java8 ]; then echo PRESENT; else echo ABSENT; fi 2>/dev/null')" == 'ABSENT' ]; then
    echo "MISSING: Installing..."
    OUTPUT=$(ssh $SSH_OPTS root@$CONSOLE "rpm -Uvh $JAVA_RPM")
    if  [ $? -ne 0 ]; then
        echo "Couldn't install Java 8 on $NODE on $CONSOLE. Received: $OUTPUT"
    fi
else
    echo "OK"
fi

echo "Upload complete"
