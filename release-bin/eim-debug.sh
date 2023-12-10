#!/bin/bash

java="jre/bin/java"
if [ ! -f java ]; then
    java="java"
fi

echo $java -Xms1G -XX:+UseZGC --enable-preview --enable-native-access=ALL-UNNAMED -jar EchoInMirror.jar "$@"
$java -Xms1G -XX:+UseZGC --enable-preview --enable-native-access=ALL-UNNAMED -jar EchoInMirror.jar "$@"
