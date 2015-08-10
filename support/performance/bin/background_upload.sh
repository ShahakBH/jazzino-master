#!/bin/bash
if [ -z "$PROPERTIES_ONLY" ]; then
    scp $SSH_OPTS $FULL_ARCHIVE root@$NODE:$FULL_ARCHIVE
    if [ $? -ne 0 ]; then
        echo "Error copying archive to $NODE"
    else
        ssh $SSH_OPTS root@$NODE "rm -rf $REMOTE_WORKING_DIR && mkdir -p $REMOTE_WORKING_DIR && cd $REMOTE_WORKING_DIR && tar xzf $FULL_ARCHIVE"
        if [ $? -ne 0 ]; then
            echo "Error extracting archive remotely on $NODE"
        fi
    fi
else
    scp $SSH_OPTS $LOCAL_WORKING_DIR/etc/grinder.properties root@$NODE:$REMOTE_WORKING_DIR/etc/
    if [ $? -ne 0 ]; then
        echo "Error copying properties to $NODE"
    else
        scp $SSH_OPTS $LOCAL_WORKING_DIR/yazino.py root@$NODE:$REMOTE_WORKING_DIR
        if [ $? -ne 0 ]; then
            echo "Error copying Grinder script to $NODE"
        fi
    fi
fi
