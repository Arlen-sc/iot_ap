#!/bin/bash
JAVA_HOME=$(dirname "$0")/../java
PATH=$JAVA_HOME/bin:$PATH
if [ ! -f "$JAVA_HOME/bin/java" ]; then
    echo "Java runtime not found in $JAVA_HOME"
    exit 1
fi
"$JAVA_HOME/bin/java" -cp "../target/classes:../target/lib/*" com.iot.plc.MainApp