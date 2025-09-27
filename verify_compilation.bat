@echo off
REM Simple script to verify Maven compilation status

REM Use the JDK bundled with the project
echo "Using bundled JDK in project directory..."
set JAVA_HOME=%cd%\java\jdk1.8.0_281

REM Check if the bundled JDK exists
if not exist "%JAVA_HOME%\bin\javac.exe" (
    echo "ERROR: Bundled JDK not found or incomplete!"
    pause
    exit /b 1
)

set PATH=%JAVA_HOME%\bin;%PATH%

REM Try to compile the project
mvn clean compile

REM Check if compilation was successful
if %ERRORLEVEL% EQU 0 (
    echo "=================================================="
    echo "COMPILATION SUCCESSFUL!"
    echo "Checking for generated class files..."
    dir target\classes\com\iot\plc /s /b
    echo "Total class files:" 
    dir target\classes\com\iot\plc /s /b | find /c ".class"
    echo "=================================================="
) else (
    echo "ERROR: Compilation failed!"
    echo "Please check the Maven output above for details."
)

pause