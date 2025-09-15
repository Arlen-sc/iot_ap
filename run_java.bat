@echo off
setlocal enabledelayedexpansion
echo 正在启动PLC任务管理系统...

REM 设置类路径
set CLASSPATH=.;target\classes

REM 添加jar包到类路径
for %%i in (lib\*.jar) do set CLASSPATH=!CLASSPATH!;%%i

REM 运行程序（使用JavaFX启动GUI界面）
java -cp "%CLASSPATH%;target\classes" com.iot.plc.JavaFXMain

pause