#!/bin/bash

TEMPLATE_PATH=$1
BUILD_PATH=$2
ASSET_PATH=$3

if [ -z "$TEMPLATE_PATH" -o -z "$BUILD_PATH" -o -z "$ASSET_PATH" ]; then
    echo "Usage: $0 /path/to/template /path/to/new-build /path/to/assets"
    exit 255
fi

mkdir -p "$BUILD_PATH/content-images"
cp -R "$ASSET_PATH" "$BUILD_PATH/content-images"
sed -e 's/class=\"tpl-content\"//g' $TEMPLATE_PATH/index.html > $BUILD_PATH/index.html
open $BUILD_PATH/index.html

