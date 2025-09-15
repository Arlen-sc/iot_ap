@echo off
setlocal enabledelayedexpansion

REM 检查参数，确定是编译还是运行
if "%1" == "run" (
    echo 运行应用程序...
    set CLASSPATH=.;target\classes
    for %%i in (lib\*.jar) do set CLASSPATH=!CLASSPATH!;%%i
    java -cp "!CLASSPATH!" com.iot.plc.JavaFXMain
    pause
    exit /b
)

echo 正在编译PLC任务管理系统...

REM 创建输出目录
if not exist "target\classes" mkdir target\classes

REM 设置类路径
set CLASSPATH=.;target\classes

REM 下载必要的依赖jar包（如果不存在）
if not exist "lib" mkdir lib
if not exist "lib\sqlite-jdbc-3.42.0.0.jar" (
    echo 下载SQLite JDBC驱动...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar' -OutFile 'lib\sqlite-jdbc-3.42.0.0.jar'"
)

if not exist "lib\jackson-databind-2.15.2.jar" (
    echo 下载Jackson Databind...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar' -OutFile 'lib\jackson-databind-2.15.2.jar'"
)

if not exist "lib\jackson-core-2.15.2.jar" (
    echo 下载Jackson Core...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar' -OutFile 'lib\jackson-core-2.15.2.jar'"
)

if not exist "lib\jackson-annotations-2.15.2.jar" (
    echo 下载Jackson Annotations...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar' -OutFile 'lib\jackson-annotations-2.15.2.jar'"
)

if not exist "lib\jssc-2.9.4.jar" (
    echo 下载JSSC库...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/io/github/java-native/jssc/2.9.4/jssc-2.9.4.jar' -OutFile 'lib\jssc-2.9.4.jar'"
)

if not exist "lib\gson-2.10.1.jar" (
    echo 下载Gson库...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar' -OutFile 'lib\gson-2.10.1.jar'"
)

REM 添加jar包到类路径
for %%i in (lib\*.jar) do set CLASSPATH=!CLASSPATH!;%%i

REM 编译Java文件，只编译不依赖Netty的类
javac -encoding UTF-8 -cp "%CLASSPATH%" -d target\classes ^
    src\main\java\com\iot\plc\*.java ^
    src\main\java\com\iot\plc\ui\*.java ^
    src\main\java\com\iot\plc\model\*.java ^
    src\main\java\com\iot\plc\database\*.java ^
    src\main\java\com\iot\plc\scheduler\*.java ^
    src\main\java\com\iot\plc\util\*.java ^
    src\main\java\com\iot\plc\service\TaskListService.java ^
    src\main\java\com\iot\plc\service\ConfigService.java ^
    src\main\java\com\iot\plc\service\LogService.java

if %errorlevel% equ 0 (
    echo 编译成功！
    echo 运行程序请执行: java -cp "%CLASSPATH%;target\classes" com.iot.plc.JavaFXMain
) else (
    echo 编译失败！
)

pause