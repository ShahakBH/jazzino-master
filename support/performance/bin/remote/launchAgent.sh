#!/bin/bash

if [ -z "$BASE_DIR" ]; then
    BASE_DIR="$(cd -P -- "$(dirname -- "$0")/../.." >/dev/null && pwd -P)"
fi
LIB_DIR="$BASE_DIR/lib"
PYTHON_DIR="$LIB_DIR/jython-2.5.3"
CLASSPATH="$LIB_DIR:$PYTHON_DIR/jython.jar:$LIB_DIR/ls-client.jar:$LIB_DIR/json_simple-1.1.jar:$LIB_DIR/commons-io-1.4.jar:$LIB_DIR/commons-codec-1.4.jar:$LIB_DIR/commons-cli-1.1.jar:$LIB_DIR/amqp-client-2.3.1.jar:$BASE_DIR/grinder/lib/grinder.jar"
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

ulimit -s 10000

pushd $BASE_DIR >/dev/null
java -cp $CLASSPATH -Dpython.home=$PYTHON_DIR net.grinder.Grinder -daemon 5 "$BASE_DIR/etc/grinder.properties"
RESULT=$?
popd >/dev/null

exit $RESULT
