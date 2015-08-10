#/bin/bash

# Check for vnstat
which vnstat &>/dev/null
if [ $? -ne 0 ]; then
  echo "ERROR: vnstat not found"
  exit 3
fi

function help {
echo "Plugin shows a snapshot (over 5 seconds) of network traffic on interface Eth0"
echo "Usage: `basename $0`:"
echo -e "\t-w <RX Mbit/s>,<RX packets/s>,<TX Mbit/s>,<TX packets/s>"
echo -e "\t-c <RX Mbit/s>,<RX packets/s>,<TX Mbit/s>,<TX packets/s>"
exit -1
}

# Get parameters
while getopts "w:c:h" OPT; do
        case $OPT in
                "w") warning=$OPTARG;;
                "c") critical=$OPTARG;;
                "h") help;;
        esac
done

# Adjusting the four warn and crit levels to kbit/s:
crit_rxspeed=`echo $critical | awk 'BEGIN{FS=",";}; {print $1 * 1024}'`
crit_rxpps=`echo $critical | awk 'BEGIN{FS=",";}; {print $2}'`
crit_txspeed=`echo $critical | awk 'BEGIN{FS=",";}; {print $3 * 1024}'`
crit_txpps=`echo $critical | awk 'BEGIN{FS=",";}; {print $4}'`

warn_rxspeed=`echo $warning | awk 'BEGIN{FS=",";}; {print $1 * 1024}'`
warn_rxpps=`echo $warning | awk 'BEGIN{FS=",";}; {print $2}'`
warn_txspeed=`echo $warning | awk 'BEGIN{FS=",";}; {print $3 * 1024}'`
warn_txpps=`echo $warning | awk 'BEGIN{FS=",";}; {print $4}'`

# Checking parameters:
if ( [ "$warn_rxspeed" == "" ] || [ "$warn_rxpps" == "" ] || [ "$warn_txspeed" == "" ] || [ "$warn_txpps" == "" ] || \
     [ "$crit_rxspeed" == "" ] || [ "$crit_rxpps" == "" ] || [ "$crit_txspeed" == "" ] || [ "$crit_txpps" == "" ] ); then
     echo "ERROR: You must specify all warning and critical levels"
     help
     exit 3
fi
if ( [[ "$warn_rxspeed" -ge  "$crit_rxspeed" ]] || \
     [[ "$warn_txspeed" -ge  "$crit_txspeed" ]] || \
     [[ "$warn_rxpps" -ge  "$crit_rxpps" ]] || \
     [[ "$warn_txpps" -ge  "$crit_txpps" ]] ); then
     echo "ERROR: critical levels must be highter than warning levels"
     help
     exit 3
fi

# Collecting the stats
stats=`vnstat -tr`

rx=`echo $stats | awk 'BEGIN{FS="[rt]x";}; {print $2}'`
tx=`echo $stats | awk 'BEGIN{FS="[rt]x";}; {print $3}'`

rxspeed=`echo $rx | awk 'BEGIN{FS=" ";}; { if ( $2=="kbit/s" ) print $1; else print $1 * 1024}'`
txspeed=`echo $tx | awk 'BEGIN{FS=" ";}; { if ( $2=="kbit/s" ) print $1; else print $1 * 1024}'`
rxpps=`echo $rx | awk 'BEGIN{FS=" ";}; {print $3}'`
txpps=`echo $tx | awk 'BEGIN{FS=" ";}; {print $3}'`

# Comparing the result and setting the correct level:
if ( [ "`echo "$rxspeed >= $crit_rxspeed" | bc`" == "1" ] || [ "`echo "$txspeed >= $crit_txspeed" | bc`" == "1" ] || \
     [ "`echo "$rxpps >= $crit_rxpps" | bc`" == "1" ] || [ "`echo "$txpps >= $crit_txpps" | bc`" == "1" ] ); then
        msg="CRITICAL"
        status=2
else if ( [ "`echo "$rxspeed >= $crit_rxspeed" | bc`" == "1" ] || [ "`echo "$txspeed >= $crit_txspeed" | bc`" == "1" ] || \
          [ "`echo "$rxpps >= $crit_rxpps" | bc`" == "1" ] || [ "`echo "$txpps >= $crit_txpps" | bc`" == "1" ] ); then
        msg="WARNING"
        status=1
     else
        msg="OK"
        status=0
     fi
fi

echo $msg" - rx:"$rx" tx:"$tx" | 'rx kbit/s'=${rxspeed};$warn_rxspeed;$crit_rxspeed;0; 'rx packets/s'=${rxpps};$warn_rxpps;$crit_rxpps;0; 'tx kbit/s'=${txspeed};$warn_txspeed;$crit_txspeed;0; 'tx packets/s'=${txpps};$warn_txpps;$crit_txpps;0;"

exit $status

