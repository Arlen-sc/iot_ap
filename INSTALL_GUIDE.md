# Java PLC任务管理系统 - 安装指南

## 第一步：项目编译打包
1. 双击运行 `compile.bat` 自动下载依赖并编译
2. 编译完成后，在 `target` 目录下生成 `classes` 文件夹和 `lib` 文件夹
3. 项目打包：
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
java -cp ".;target\classes;lib/*" com.iot.plc.JavaFXMain
```

## 第二步：运行PLC任务管理系统

1. 双击运行 `start.bat` 启动应用程序

## 系统功能
启动后您将看到：
1. **任务列表标签页** - 显示所有配置的PLC任务
2. **配置管理标签页** - 添加新的PLC任务配置

## 故障排除
如果遇到问题：
2. 检查lib目录是否有所有必需的jar文件
3. 确保没有防火墙阻止程序运行