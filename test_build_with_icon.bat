@echo off

REM Test build script - with custom ICO icon

REM Execute Maven build
echo Building PLC Manager EXE with custom icon...
mvn clean package

REM Check build result
if %ERRORLEVEL% equ 0 (
    echo Build successful!
    echo PLCManager.exe has been generated in the target folder
    echo Please check if the generated EXE file displays the custom icon
    echo.
    echo Icon location: src/main/resources/app-icon.ico
) else (
    echo Build failed! Please check error messages
    pause
    exit /b 1
)

REM Display build completion info
echo.
echo Build process completed. Please check the results.
pause