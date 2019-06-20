#!/bin/bash

MVN_PATH=/home/vincent/apache-maven-3.6.0/bin/mvn
JAVA_PATH=java
MVN_STATIC_ARGS="-Dexec.executable=$JAVA_PATH -Dexec.classpathScope=runtime -T 1C org.codehaus.mojo:exec-maven-plugin:1.6.0:exec -q"
MVN_DYNAMIC_ARGS="-Dexec.args=-classpath %classpath org.opensilex.dev.Main"
for ARG in "$@"
do
    MVN_DYNAMIC_ARGS="$MVN_DYNAMIC_ARGS $ARG"
done

CMD="$MVN_PATH \"$MVN_DYNAMIC_ARGS\" $MVN_STATIC_ARGS"

#java -classpath "./opensilex-dev-tools/target/opensilex-dev-tools-1.0.0-SNAPSHOT.jar:./opensilex/target/opensilex-1.0.0-SNAPSHOT-full.jar" org.opensilex.dev.Main

eval $CMD
