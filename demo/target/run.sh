#!/bin/bash

# Script to run the DisRecorderDemo JAR file and allow user to choose main class

# --- Configuration ---
JAR_FILE="dis-demo-1.0-SNAPSHOT-jar-with-dependencies.jar"
DEFAULT_MAIN_CLASS="com.techtest.recorder.DisRecorderDemo"
GUI_CLASS="com.techtest.recorder.gui.RecorderGui"

# --- Script Logic ---

# Check if the JAR file exists
if [ ! -f "$JAR_FILE" ]; then
  echo "Error: JAR file '$JAR_FILE' not found."
  exit 1
fi

echo "Available options:"
echo "1. Run with default main class: $DEFAULT_MAIN_CLASS"
echo "2. Run with GUI class: $GUI_CLASS"
echo "3. Specify a custom main class"
read -p "Choose an option (1-3): " CHOICE

case "$CHOICE" in
  1)
    echo "Attempting to run '$JAR_FILE' with default main class '$DEFAULT_MAIN_CLASS'."
    java -jar "$JAR_FILE"
    ;;
  2)
    echo "Attempting to run '$JAR_FILE' with GUI class '$GUI_CLASS'."
    java -cp "$JAR_FILE" "$GUI_CLASS"
    ;;
  3)
    read -p "Enter the fully qualified name of the main class: " CUSTOM_MAIN_CLASS
    if [ -n "$CUSTOM_MAIN_CLASS" ]; then
      echo "Attempting to run '$JAR_FILE' with custom main class '$CUSTOM_MAIN_CLASS'."
      java -cp "$JAR_FILE" "$CUSTOM_MAIN_CLASS"
    else
      echo "Error: No custom main class provided."
      exit 1
    fi
    ;;
  *)
    echo "Invalid choice. Please select 1, 2, or 3."
    exit 1
    ;;
esac

exit $?
