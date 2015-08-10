#!/bin/bash

BASE_DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

if [ -z "$1" ]; then
    echo "Usage: $0 <data bag matching regex>"
    echo "e.g. use $0 'bob' to refresh all bags containing 'bob'"
    exit 1
fi
NODE_REGEX="$1"

if [ -z "$(which knife 2>/dev/null)" ]; then
    echo "Chef is not installed or not on the path. Please run 'gem install chef'."
    exit 1
fi

for FILE in $(ls $BASE_DIR); do
    BAGS="$(knife data bag list)"
    if [[ -d "$BASE_DIR/$FILE" && "$FILE" =~ $NODE_REGEX ]]; then
        for BAG in $(ls "$BASE_DIR/$FILE"); do
            if ! echo $BAGS | grep $FILE >/dev/null 2>&1; then
                knife data bag create $FILE
            fi

            if [[ "$BAG" =~ .*\.json$ ]]; then
                knife data bag from file "$FILE" "$BASE_DIR/$FILE/$BAG"
            fi
        done
    fi
done
