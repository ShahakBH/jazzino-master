#!/bin/bash

BASE_DIR="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"

if [ -z "$1" ]; then
  echo "Usage: $0 <cookbook matching regex>"
  echo "e.g. use $0 'bob-.*' to refresh all cookbooks start with 'bob'"
  exit 1
fi
NODE_REGEX="$1"

if [ -z "$(which knife 2>&1)" ]; then
  echo "Chef is not installed or not on the path. Please run 'gem install chef'."
  exit 1
fi

for FILE in $(ls $BASE_DIR); do
  if [ -d "$BASE_DIR/$FILE/recipes" ]; then
    if [[ "$FILE" =~ $NODE_REGEX ]]; then
      knife cookbook upload "$FILE"
    fi
  fi
done
