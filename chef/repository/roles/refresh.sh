#!/bin/bash

BASE_DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

if [ -z "$1" ]; then
	echo "Usage: $0 <role matching regex>"
	echo "e.g. use $0 'bob' to refresh all roles containing 'bob'"
	exit 1
fi
NODE_REGEX="$1"

if [ -z `which knife` ]; then
	echo "Chef is not installed or not on the path. Please run 'gem install chef'."
	exit 1
fi

for FILE in `ls $BASE_DIR`; do
	if [[ $FILE =~ .*\.json$ ]]; then
		if [[ $FILE =~ $NODE_REGEX ]]; then
			knife role from file $FILE
		fi
	fi
done
