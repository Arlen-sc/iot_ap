@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "JAVA_HOME=%SCRIPT_DIR%java"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo Checking Java at: %JAVA_HOME%
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo Error: Java not found at:
    echo %JAVA_HOME%
    echo Please ensure Java is installed in the 'java' directory
    pause
    exit /b 1
)

echo Starting application...
cd /D "%SCRIPT_DIR%"
"%JAVA_HOME%\bin\java" -cp "target\classes;lib\*" com.iot.plc.MainApp
if errorlevel 1 (
    echo Application failed with error %errorlevel%
    echo Possible reasons:
    echo 1. Project not compiled (run 'mvn compile' first)
    echo 2. Java not properly installed in java directory
    pause
)
endlocal