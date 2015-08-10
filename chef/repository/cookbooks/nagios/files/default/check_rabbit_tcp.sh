#!/bin/sh

STATS_FILE=$(mktemp /tmp/rabbit_tcp.XXXXXXXX)
netstat -antp | grep :5672 > $STATS_FILE

established=$(grep -c ESTABLISHED $STATS_FILE)
listen=$(grep -c LISTEN $STATS_FILE)
closewait=$(grep -c CLOSE_WAIT $STATS_FILE)

if [ -z "$established" ]; then
  if [ -z "$listen" ]; then
  echo "No clients connected to RabbitMQ. Listener Down.|'ESTABLISHED'=0;;;; 'LISTEN'=0;;;; 'CLOSE_WAIT'="$closewait";;;;"
  exit 2
  fi
echo "No clients connected to RabbitMQ. Listener OK.|'ESTABLISHED'=0;;;; 'LISTEN'="$listen";;;; 'CLOSE_WAIT'="$closewait";;;;"
exit 2
fi

echo $established" clients connected to RabbitMQ.|'ESTABLISHED'="$established";;;; 'LISTEN'="$listen";;;; 'CLOSE_WAIT'="$closewait";;;;"
exit 0
