#!/bin/bash

VHOST=maggie-test

RABBITMQCTL=/usr/sbin/rabbitmqctl
if [ ! -f "$RABBITMQCTL" ]; then
	RABBITMQCTL=/usr/local/sbin/rabbitmqctl
	if [ ! -f "$RABBITMQCTL" ]; then
		RABBITMQCTL=rabbitmqctl
	fi
fi

if [ $($RABBITMQCTL | grep -c 'root or rabbitmq') != "0" ]; then
    RABBITMQCTL="sudo $RABBITMQCTL"

    if ! tty -s; then
        echo "Bash is not in interactive mode and sudo is required; if the build fails please run the $0 script manually to configure RabbitMQ."
        exit 0
    fi
fi

function add_or_update_user { # user_name, password
    local USERNAME=$1
    local PASSWORD=$2

    if [ $(rabbit list_users | grep -c "^$USERNAME") = "0" ]; then
        rabbit add_user $USERNAME $PASSWORD
    else
        rabbit change_password $USERNAME $PASSWORD
    fi
}

function delete_user { # username
    local USERNAME=$1

    if [ $(rabbit list_users | grep -c "^$USERNAME") != "0" ]; then
        rabbit delete_user $USERNAME
    fi
}

function add_vhost { # vhost_name
    local VHOST_NAME=$1

    if [ -z $(rabbit list_vhosts | grep -e "^$VHOST_NAME$") ]; then
        rabbit add_vhost $VHOST_NAME
    fi
}

function rabbit {
    $RABBITMQCTL "$@"
    if [ $? -ne 0 ]; then
        echo "Failed to execute $RABBITMQCTL"
        exit 1
    fi
}

$(rabbit status > /dev/null)
if [ $? != 0 ]; then
	echo "Couldn't execute $RABBITMQCTL"
	exit 1
fi

delete_user guest

add_or_update_user worker w0rk3r%
add_vhost $VHOST
rabbit set_permissions \-p $VHOST worker worker worker worker
