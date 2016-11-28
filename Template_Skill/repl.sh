#!/bin/sh

gradle compileScala

if [ $? == 0 ]; then
    CLASSPATH=$(gradle classpath -q)
    CP=$(echo $CLASSPATH | grep '/' | tr '\n ' ':' | tr ':' '\n' | sort | uniq | tr '\n' ':')
    scala -Dscala.color -classpath $CP
fi
