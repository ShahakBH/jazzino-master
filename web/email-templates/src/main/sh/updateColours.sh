#!/bin/bash

SEARCH_PATH=$1
PRIMARY_COLOR=$2
SECONDARY_COLOR=$3

find $SEARCH_PATH -name 'index.html' -exec sed -i \.pre-color-backup -e "s/PRIMARY_COLOR/#$PRIMARY_COLOR/g" -e "s/SECONDARY_COLOR/#$SECONDARY_COLOR/g" \{\} \;
find $SEARCH_PATH -name 'index.html\.pre-color-backup' | xargs rm

