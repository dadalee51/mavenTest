#!/bin/bash

echo "Available programs:"
echo "1) DisRecorder GUI"
echo "2) DisRecorder ConsoleApp"
echo "3) DisExample test"

read -p "Enter program number to run: " choice

case $choice in
  1)
    mvn -f pom.xml compile exec:java -Dexec.mainClass="com.techtest.gui.RecorderGui"
    ;;
  2)
    mvn -f pom.xml compile exec:java -Dexec.mainClass="com.techtest.recorder.DisRecorderDemo"
    ;;
  3)
    mvn -f pom.xml compile exec:java -Dexec.mainClass="com.techtest.DisExample"
    ;;
  *)
    echo "Invalid selection"
    ;;
esac
