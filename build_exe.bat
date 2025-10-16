@echo off

rem Simple build script for PLC Manager - compatible with spaces in paths

echo ============================
echo Building PLC Manager EXE...
echo ============================

rem Execute Maven package command directly
cmd /c "mvn clean package"

rem Check result
if %ERRORLEVEL% equ 0 (
    echo.
    echo BUILD SUCCESS!
    echo PLCManager.exe is ready in target folder
) else (
    echo.
    echo BUILD FAILED!
    pause
    exit /b 1
)

echo.
pause