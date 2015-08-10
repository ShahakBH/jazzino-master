#!/bin/bash

RABBITMQCTL="/usr/sbin/rabbitmqctl"

VERBOSE=

WARNING=100
CRITICAL=500
VHOST=rabbit.yazino.com
QUEUE_REGEX=*
EXCL_QUEUE_REGEX=nonExistentQueue

for ARG in "$@"; do
	case $ARG in
		'--help')
		echo 'Usage: $0 [-v] [-wWARNING] [-cCRITICAL] [-rREGEX] [-eEXCLUDEREGEX] [-hVHOST]'
		exit 0
		;;
		'-v' )
		VERBOSE="true"
		;;
		-w[0-9]* )
		WARNING=${ARG:2}
		;;
		-c[0-9]* )
		CRITICAL=${ARG:2}
		;;
		-r* )
		QUEUE_REGEX=${ARG:2}
		;;
		-e* )
		EXCL_QUEUE_REGEX=${ARG:2}
		;;
		*)
		VHOST=${ARG}
		;;
	esac
done

if [ -n "$VERBOSE" ]; then
	echo "VHost: $VHOST"
	echo "Warning threshold: $WARNING"
	echo "Critical threshold: $CRITICAL"
	echo "Queue Regex filter: $QUEUE_REGEX"
	echo "Exclude Queue Regex filter: $EXCL_QUEUE_REGEX"
fi

if [ ! -x "$RABBITMQCTL" ]; then
	echo "UNKNOWN: $RABBITMQCTL does not exist or is not executable"
	exit 3
fi

MESSAGES=$(sudo $RABBITMQCTL list_queues -p $VHOST name messages | grep -v '^amq.gen' | grep -E "$QUEUE_REGEX"| grep  -v -E "$EXCL_QUEUE_REGEX"| awk '{total+=$2}END{print total}' 2>&1)
if [ $? -ne 0 ]; then
	echo "UNKNOWN: Could not execute rabbitmqctl: $MESSAGES"
	exit 3
fi

if [ -z "$MESSAGES" ]; then
	echo "UNKNOWN: No output from RabbitCTL"
	exit 3
fi

if (($MESSAGES > $CRITICAL)); then
	echo "CRITICAL: Queue message count of $MESSAGES exceeds critical threshold of $CRITICAL | Messages=$MESSAGES"
	exit 2
elif (($MESSAGES > $WARNING)); then
	echo "WARNING: Queue message count of $MESSAGES exceeds warning threshold of $WARNING | Messages=$MESSAGES"
	exit 1
else
	echo "OK: Queue message count of $MESSAGES is satisfactory | Messages=$MESSAGES"
	exit 0
fi
