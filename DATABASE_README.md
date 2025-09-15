# 数据库配置说明

## 当前数据库配置
✅ **数据库类型**: SQLite  
✅ **数据库文件**: `plc_tasks.db` (自动创建在当前目录)  
✅ **JDBC驱动**: `org.sqlite.JDBC`  
✅ **连接URL**: `jdbc:sqlite:plc_tasks.db`

## 数据库表结构

### tasks表 (任务配置表)
- `id`: INTEGER PRIMARY KEY AUTOINCREMENT
- `device_id`: TEXT NOT NULL (设备号)
- `cron_expression`: TEXT NOT NULL (定时任务cron表达式)
- `description`: TEXT (说明)
- `task_type`: TEXT NOT NULL (任务类型: PLC读取/PLC传送/数据解析/数据保存)
- `task_name`: TEXT NOT NULL (任务名称)
- `enabled`: BOOLEAN DEFAULT TRUE (启用状态)
- `created_at`: DATETIME DEFAULT CURRENT_TIMESTAMP (创建时间)

### plc_data表 (PLC数据表)
- `id`: INTEGER PRIMARY KEY AUTOINCREMENT
- `device_id`: TEXT NOT NULL (设备号)
- `data_json`: TEXT NOT NULL (JSON格式数据)
- `created_at`: DATETIME DEFAULT CURRENT_TIMESTAMP (创建时间)

## 使用说明
数据库会在应用程序首次运行时自动创建。所有数据将保存在当前目录的 `plc_tasks.db` SQLite数据库文件中。

无需额外配置，系统已完全支持SQLite数据库。