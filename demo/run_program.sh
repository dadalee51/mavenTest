#!/bin/bash

echo "Available programs:"
echo "1) Matrix Benchmark GUI"
echo "2) Main Program"
echo "3) Matrix Multiplication Benchmark (Console)"

read -p "Enter program number to run: " choice

case $choice in
  1)
    mvn -f pom.xml compile exec:java -Dexec.mainClass="com.example.MatrixBenchmarkGUI"
    ;;
  2)
    mvn -f pom.xml compile exec:java -Dexec.mainClass="com.example.Main"
    ;;
  3)
    mvn -f pom.xml compile exec:java -Dexec.mainClass="com.example.MatrixMultiplicationBenchmark"
    ;;
  *)
    echo "Invalid selection"
    ;;
esac
