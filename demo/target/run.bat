@echo off

REM Script to run the DisRecorderDemo JAR file and allow user to choose main class (Windows)

REM --- Configuration ---
set JAR_FILE="dis-demo-1.0-SNAPSHOT-jar-with-dependencies.jar"
set DEFAULT_MAIN_CLASS="com.techtest.recorder.DisRecorderDemo"
set GUI_CLASS="com.techtest.recorder.gui.RecorderGui"

REM --- Script Logic ---

REM Check if the JAR file exists
if not exist "%JAR_FILE%" (
  echo Error: JAR file "%JAR_FILE%" not found.
  exit /b 1
)

echo Available options:
echo 1. Run with default main class: %DEFAULT_MAIN_CLASS%
echo 2. Run with GUI class: %GUI_CLASS%
echo 3. Specify a custom main class
set /p CHOICE="Choose an option (1-3): "

if "%CHOICE%"=="1" (
  echo Attempting to run "%JAR_FILE%" with default main class "%DEFAULT_MAIN_CLASS%".
  java -jar "%JAR_FILE%"
) else if "%CHOICE%"=="2" (
  echo Attempting to run "%JAR_FILE%" with GUI class "%GUI_CLASS%".
  java -cp "%JAR_FILE%" %GUI_CLASS%
) else if "%CHOICE%"=="3" (
  set /p CUSTOM_MAIN_CLASS="Enter the fully qualified name of the main class: "
  if not "%CUSTOM_MAIN_CLASS%"=="" (
    echo Attempting to run "%JAR_FILE%" with custom main class "%CUSTOM_MAIN_CLASS%".
    java -cp "%JAR_FILE%" %CUSTOM_MAIN_CLASS%
  ) else (
    echo Error: No custom main class provided.
    exit /b 1
  )
) else (
  echo Invalid choice. Please select 1, 2, or 3.
  exit /b 1
)

exit /b 0

