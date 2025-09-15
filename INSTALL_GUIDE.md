# Java PLC任务管理系统 - 安装指南

## 第一步：安装Java开发环境

### 下载并安装Java JDK 11+
1. 访问Oracle官网：https://www.oracle.com/java/technologies/javase-jdk11-downloads.html
2. 下载Windows x64 Installer
3. 运行安装程序，按照默认设置安装

### 设置环境变量
安装完成后，需要设置JAVA_HOME环境变量：
1. 右键点击"此电脑" → 属性 → 高级系统设置
2. 点击"环境变量"
3. 在"系统变量"中新建：
   - 变量名：`JAVA_HOME`
   - 变量值：`C:\Program Files\Java\jdk-11.0.x` (根据实际安装路径)
4. 编辑Path变量，添加：`%JAVA_HOME%\bin`

### 验证安装
打开命令提示符，输入：
```cmd
java -version
javac -version
```
应该显示Java版本信息。

## 第二步：运行PLC任务管理系统

### 方法一：使用编译脚本（推荐）
1. 双击运行 `compile.bat` 自动下载依赖并编译
2. 双击运行 `run_java.bat` 启动应用程序

### 方法二：手动编译运行
```cmd
# 创建目录
mkdir target\classes
mkdir lib

# 下载依赖（如果compile.bat无法运行）
# 手动下载以下jar包到lib目录：
# - sqlite-jdbc-3.42.0.0.jar
# - jackson-databind-2.15.2.jar  
# - jackson-core-2.15.2.jar
# - jackson-annotations-2.15.2.jar

# 编译
javac -cp ".;lib/*" -d target\classes src\main\java\com\iot\plc\*.java src\main\java\com\iot\plc\*\*.java

# 运行
java -cp ".;target\classes;lib/*" com.iot.plc.MainApp
```

## 系统功能
启动后您将看到：
1. **任务列表标签页** - 显示所有配置的PLC任务
2. **配置管理标签页** - 添加新的PLC任务配置

## 故障排除
如果遇到问题：
1. 确认Java已正确安装：`java -version`
2. 检查lib目录是否有所有必需的jar文件
3. 确保没有防火墙阻止程序运行