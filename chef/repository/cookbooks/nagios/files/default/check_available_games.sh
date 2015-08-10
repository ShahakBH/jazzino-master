#/bin/bash

MONITOR_URL="http://localhost:8080/command/monitor"
GAME_TYPES=$1                     
                         
if [ -z "$GAME_TYPES" ]; then
	echo "Usage: $0 [game_type,game_type,...] <monitor_url=$MONITOR_URL>"
	exit 1
fi

if [ -n "$2" ]; then
	MONITOR_URL=$2
fi

MONITOR_OUTPUT=$(wget --save-headers -O - ${MONITOR_URL})
SUCCESS=$(echo $MONITOR_OUTPUT | grep -c "200 OK")   

for GAME_TYPE in $(echo $GAME_TYPES | tr "," "\n"); do
	AVAILABLE=$(echo $MONITOR_OUTPUT | grep -c "\"${GAME_TYPE}\": true")

	if [ $SUCCESS -eq 1 -a $AVAILABLE -eq 0 ]; then
		echo $GAME_TYPE" game is unavailable | '"${GAME_TYPE}"'=0;;;;"
		exit 2             
	fi  
	
	if [ "$SUCCESS" -eq 0 ]; then
		echo "Monitor unavailable. Check ${MONITOR_URL}"
		exit 3                  
	fi
done

echo $GAME_TYPES" are available | '"${GAME_TYPES}"'=1;;;;"
exit 0
