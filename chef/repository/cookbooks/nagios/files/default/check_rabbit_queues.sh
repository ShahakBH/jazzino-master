#/bin/sh

# Simple script to check number of queues in RabbitMQ

STATE_OK=0
STATE_WARNING=1
STATE_CRITICAL=2
STATE_UNKNOWN=3
STATE_DEPENDENT=4

RABBITMQCTL=/usr/sbin/rabbitmqctl

WARNING_THRESHOLD=${WARNING_THRESHOLD:="5000"}
CRITICAL_THRESHOLD=${CRITICAL_THRESHOLD:="10000"}
EXIT_CODE=$STATE_OK
STATUS=OK

print_usage_and_exit() {
    echo "Usage: $0 -p <virtual host> -w <warning count=$WARNING_THRESHOLD> -c <critical count=$CRITICAL_THRESHOLD>"
    exit $STATE_UNKNOWN
}

while [ $# -gt 0 ]; do
    case "$1" in
        -w | --warning)
            shift
            WARNING_THRESHOLD=$1
            ;;
        -c | --critical)
            shift
            CRITICAL_THRESHOLD=$1
            ;;
        -p | --virtual-host)
            shift
            VIRTUAL_HOST=$1
            ;;
        *)  echo "Unknown argument: $1"
            print_usage_and_exit
            ;;
        esac
shift
done

if [ -z "$VIRTUAL_HOST" ]; then
    echo "No virtual host specified"
    print_usage_and_exit
fi

QUEUES=$(sudo $RABBITMQCTL list_queues -p $VIRTUAL_HOST | wc -l)

if [ $QUEUES -gt $CRITICAL_THRESHOLD ]; then
    EXIT_CODE=$STATE_CRITICAL
    STATUS=CRITICAL
elif [ $QUEUES -gt $WARNING_THRESHOLD ]; then
    EXIT_CODE=$STATE_WARNING
    STATUS=WARNING
fi

echo "QUEUES $STATUS: Queue Count is $QUEUES|queues=$QUEUES;$WARNING_THRESHOLD;$CRITICAL_THRESHOLD;0;"
exit $EXIT_CODE
