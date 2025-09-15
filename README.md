# PLC任务管理系统

一个基于Java Swing的PLC设备任务管理和数据配置桌面应用程序。

## 功能特性

- **任务列表管理**: 查看和管理所有PLC任务
- **配置界面**: 配置PLC设备的数据读取、传送和解析任务
- **定时任务**: 支持cron表达式定时执行任务
- **数据存储**: 使用SQLite数据库存储任务配置和采集数据
- **JSON支持**: 数据以JSON格式处理和存储

## 系统要求

- Java 11+
- Maven 3.6+

## 安装和运行

1. 克隆项目到本地
2. 运行构建命令:
   ```bash
   mvn clean compile
   ```
3. 启动应用程序:
   ```bash
   mvn exec:java
   ```
   或者双击运行 `run.bat` (Windows)

## 使用说明

### 任务列表界面
- 显示所有已配置的任务
- 包含字段: 设备号、定时任务cron、说明、任务类型、任务名称、状态
- 支持任务刷新和删除操作

### 配置界面
- **设备号**: PLC设备的唯一标识
- **定时任务cron**: 任务执行的时间表达式
- **说明**: 任务描述信息
- **任务类型**: 下拉选择(PLC读取、PLC传送、数据解析、数据保存)
- **任务名称**: 任务的名称标识

### PLC通信功能
1. **PLC读取**: 通过TCP连接读取PLC设备数据
2. **数据解析**: 解析原始数据并转换为JSON格式
3. **数据保存**: 将JSON数据保存到数据库
4. **PLC传送**: 向PLC设备发送JSON格式的指令数据

## 技术栈

- Java Swing - 用户界面
- SQLite - 数据库存储
- Quartz - 定时任务调度
- Jackson - JSON处理
- Logback - 日志记录

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── com/iot/plc/
│   │       ├── MainApp.java          # 主应用程序
│   │       ├── model/
│   │       │   └── Task.java         # 任务模型
│   │       ├── ui/
│   │       │   ├── TaskListPanel.java # 任务列表界面
│   │       │   └── ConfigPanel.java   # 配置界面
│   │       ├── database/
│   │       │   └── DatabaseManager.java # 数据库管理
│   │       ├── service/
│   │       │   └── PlcService.java    # PLC服务
│   │       ├── scheduler/
│   │       │   └── TaskScheduler.java # 任务调度器
│   │       └── util/
│   │           └── Config.java       # 配置工具
│   └── resources/
│       ├── config.properties         # 配置文件
│       └── logback.xml               # 日志配置
```

## 开发说明

项目使用Maven进行依赖管理和构建，所有依赖项已在pom.xml中配置。