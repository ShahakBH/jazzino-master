#/bin/sh

# Simple custom script to check memory usage in RabbitMQ

MEMORY=`sudo /usr/sbin/rabbitmqctl list_queues -p rabbit.yazino.com memory | sort | tail -n 3 | grep -v done | grep -v queues`

echo "Largest queue using "$MEMORY" bytes of memory | memory="$MEMORY"B;;;;"
