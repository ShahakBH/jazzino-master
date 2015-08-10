#!/bin/bash

BIN_DIR=$(cd -P -- "$(dirname -- "$0")" && pwd -P)

source $BIN_DIR/config.sh

if [ ! -x "$BASE_DIR/grinder" ]; then
    ln -sf "$BASE_DIR/grinder-$GRINDER_VERSION" "$BASE_DIR/grinder"
fi

source $BIN_DIR/remote/launchAgent.sh
