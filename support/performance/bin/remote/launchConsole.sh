#!/bin/bash

if [ -z "$BASE_DIR" ]; then
    BASE_DIR="$(cd -P -- "$(dirname -- "$0")" >/dev/null && pwd -P)/../.."
fi
LIB_DIR="$BASE_DIR/lib"
PYTHON_DIR="$LIB_DIR/jython-2.5.3"
CLASSPATH="$BASE_DIR/grinder-3.11/lib/grinder.jar"
JAVA7_HOME=/opt/java7
JAVA8_HOME=/opt/java8

if [ "$(uname)" == 'Darwin' ]; then
    export JAVA_HOME="$(/usr/libexec/java_home -v '1.8*')"
elif [ -d "$JAVA8_HOME" ]; then
    export JAVA_HOME=$JAVA8_HOME
elif [ -d "$JAVA7_HOME" ]; then
    export JAVA_HOME=$JAVA7_HOME
fi

if [ -n "$JAVA_HOME" ]; then
    echo "Using Java home directory: $JAVA_HOME"
    export PATH=$JAVA_HOME/bin:$PATH
fi

pushd $BASE_DIR >/dev/null
nohup java -cp $CLASSPATH net.grinder.Console &
RESULT=$?
popd >/dev/null
exit $RESULT
