package com.iot.plc.database;

import com.iot.plc.model.*;
import com.iot.plc.model.LogItem;
import com.iot.plc.model.TaskItem;
import com.iot.plc.logger.LogManager;
import com.iot.plc.logger.Logger;
import java.sql.*;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:plc_tasks.db";
    private static final Logger logger = Logger.getInstance();
    
    static {
        try {
            Class.forName("org.sqlite.JDBC");
            initializeDatabase();
        } catch (Exception e) {
            logger.error("JDBC驱动加载失败: " + e.getMessage(), e);
        }
    }
    
    public static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            logger.info("开始初始化数据库");
            
            String createTasksTable = "CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "device_id TEXT NOT NULL," +
                    "cron_expression TEXT NOT NULL," +
                    "description TEXT," +
                    "task_type TEXT NOT NULL," +
                    "task_name TEXT NOT NULL," +
                    "enabled BOOLEAN DEFAULT TRUE," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            
            String createDataTable = "CREATE TABLE IF NOT EXISTS plc_data (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "device_id TEXT NOT NULL," +
                    "data_json TEXT NOT NULL," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            
            String createTaskDetailsTable = "CREATE TABLE IF NOT EXISTS task_details (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "task_id INTEGER NOT NULL," +
                    "field_name TEXT NOT NULL," +
                    "field_value TEXT," +
                    "data_type TEXT NOT NULL DEFAULT 'string'," +
                    "required BOOLEAN DEFAULT FALSE," +
                    "description TEXT," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE" +
                    ")";
            
            String createBarcodeDataTable = "CREATE TABLE IF NOT EXISTS barcode_data (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "device_id VARCHAR(20) NOT NULL," +
                    "barcode VARCHAR(50) NOT NULL," +
                    "scan_time DATETIME NOT NULL," +
                    "port_name VARCHAR(20)," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            
            String createValidationResultTable = "CREATE TABLE IF NOT EXISTS validation_result (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "is_valid BOOLEAN NOT NULL," +
                    "message TEXT NOT NULL," +
                    "expected_count INTEGER NOT NULL," +
                    "actual_count INTEGER NOT NULL," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            
            String createProgramResultTable = "CREATE TABLE IF NOT EXISTS program_result (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "batch_id VARCHAR(50)," +
                    "device_id VARCHAR(20) ," +
                    "barcode VARCHAR(50) NOT NULL," +
                    "result BOOLEAN NOT NULL," +
                    "error_message TEXT," +
                    "program_time DATETIME NOT NULL," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";
                    
            // 创建配置表
            String createConfigTable = "CREATE TABLE IF NOT EXISTS config_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "config_key TEXT NOT NULL UNIQUE," +
                    "config_value TEXT NOT NULL," +
                    "description TEXT," +
                    "data_type TEXT NOT NULL DEFAULT 'string'," +
                    "required BOOLEAN DEFAULT FALSE," +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            
            stmt.execute(createTasksTable);
            stmt.execute(createDataTable);
            stmt.execute(createTaskDetailsTable);
            stmt.execute(createBarcodeDataTable);
            stmt.execute(createValidationResultTable);
            stmt.execute(createProgramResultTable);
            stmt.execute(createConfigTable);
            
            // 创建索引
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_barcode_device ON barcode_data(device_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_barcode_scan_time ON barcode_data(scan_time)");
            // 移除batch_id索引，因为不再需要batch_id字段
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_program_device ON program_result(device_id)");
            
            logger.info("数据库初始化完成");
        } catch (Exception e) {
            logger.error("数据库初始化失败: " + e.getMessage(), e);
        }
    }
    
    public static Connection getConnection() throws SQLException {
        logger.debug("获取数据库连接");
        return DriverManager.getConnection(DB_URL);
    }
    
    public static void saveTask(Task task) throws SQLException {
        if (task.getId() > 0) {
            // 更新现有任务
            String sql = "UPDATE tasks SET device_id = ?, cron_expression = ?, description = ?, task_type = ?, task_name = ?, enabled = ? WHERE id = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, task.getDeviceId());
                pstmt.setString(2, task.getCronExpression());
                pstmt.setString(3, task.getDescription());
                pstmt.setString(4, task.getTaskType());
                pstmt.setString(5, task.getTaskName());
                pstmt.setBoolean(6, task.isEnabled());
                pstmt.setInt(7, task.getId());
                
                pstmt.executeUpdate();
            }
        } else {
            // 添加新任务
            String sql = "INSERT INTO tasks (device_id, cron_expression, description, task_type, task_name) " +
                        "VALUES (?, ?, ?, ?, ?)";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, task.getDeviceId());
                pstmt.setString(2, task.getCronExpression());
                pstmt.setString(3, task.getDescription());
                pstmt.setString(4, task.getTaskType());
                pstmt.setString(5, task.getTaskName());
                
                pstmt.executeUpdate();
            }
        }
    }
    
    public static List<TaskItem> getAllTasks() throws SQLException {
        List<TaskItem> tasks = new ArrayList<>();
        String sql = "SELECT id, task_name, cron_expression, device_id, status, remark, start_time, last_execute_time FROM tasks";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                TaskItem taskItem = new TaskItem(
                    rs.getInt("id"),
                    rs.getString("task_name"),
                    rs.getString("cron_expression"),
                    rs.getString("device_id"),
                    rs.getInt("status"),
                    rs.getString("remark"),
                    rs.getTimestamp("start_time") != null ? rs.getTimestamp("start_time").toLocalDateTime() : null,
                    rs.getTimestamp("last_execute_time") != null ? rs.getTimestamp("last_execute_time").toLocalDateTime() : null
                );
                tasks.add(taskItem);
            }
        }
        return tasks;
    }
    
    // 修改savePlcData方法，通过LogManager调用
    public static void savePlcData(String deviceId, String jsonData) throws SQLException {
        LogManager.getInstance().savePlcData(deviceId, jsonData);
    }
    
    public static void deleteTask(int taskId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, taskId);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * 获取所有日志数据
     */
    public static List<LogItem> getAllLogs() throws SQLException {
        List<LogItem> logs = new ArrayList<>();
        String sql = "SELECT 'PLC数据' as log_type, id, created_at, data_json as content, '成功' as status FROM plc_data " +
                      "UNION ALL " +
                      "SELECT '条码数据' as log_type, id, created_at, barcode as content, '成功' as status FROM barcode_data " +
                      "UNION ALL " +
                      "SELECT '验证结果' as log_type, id, created_at, message as content, CASE WHEN is_valid THEN '成功' ELSE '失败' END as status FROM validation_result " +
                      "UNION ALL " +
                      "SELECT '烧录结果' as log_type, id, created_at, '设备ID:' || device_id || ', 条码:' || barcode as content, CASE WHEN result THEN '成功' ELSE '失败' END as status FROM program_result " +
                      "ORDER BY created_at DESC LIMIT 1000";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                LogItem logItem = new LogItem();
                logItem.setLogType(rs.getString("log_type"));
                logItem.setTimestamp(rs.getTimestamp("created_at").toLocalDateTime());
                logItem.setDataContent(rs.getString("content"));
                logItem.setStatus(rs.getString("status"));
                logs.add(logItem);
            }
        }
        return logs;
    }
    
    /**
     * 根据日志类型获取日志
     */
    public static List<LogItem> getLogsByType(String logType) throws SQLException {
        List<LogItem> logs = new ArrayList<>();
        String sql = "";
        
        if ("PLC数据".equals(logType)) {
            sql = "SELECT id, created_at, data_json as content, '成功' as status FROM plc_data ORDER BY created_at DESC LIMIT 500";
        } else if ("条码数据".equals(logType)) {
            sql = "SELECT id, created_at, barcode as content, '成功' as status FROM barcode_data ORDER BY created_at DESC LIMIT 500";
        } else if ("验证结果".equals(logType)) {
            sql = "SELECT id, created_at, message as content, CASE WHEN is_valid THEN '成功' ELSE '失败' END as status FROM validation_result ORDER BY created_at DESC LIMIT 500";
        }
        
        if (!sql.isEmpty()) {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    LogItem logItem = new LogItem();
                    logItem.setLogType(logType);
                    logItem.setTimestamp(rs.getTimestamp("created_at").toLocalDateTime());
                    logItem.setDataContent(rs.getString("content"));
                    logItem.setStatus(rs.getString("status"));
                    logs.add(logItem);
                }
            }
        }
        
        return logs;
    }
    
    /**
     * 获取烧录结果日志
     */
    public static List<LogItem> getBurnResultLogs() throws SQLException {
        List<LogItem> logs = new ArrayList<>();
        String sql = "SELECT id, created_at, '设备ID:' || device_id || ', 条码:' || barcode as content, CASE WHEN result THEN '成功' ELSE '失败' END as status FROM program_result ORDER BY created_at DESC LIMIT 500";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                LogItem logItem = new LogItem();
                logItem.setLogType("烧录结果");
                logItem.setTimestamp(rs.getTimestamp("created_at").toLocalDateTime());
                logItem.setDataContent(rs.getString("content"));
                logItem.setStatus(rs.getString("status"));
                logs.add(logItem);
            }
        }
        
        return logs;
    }
    
    /**
     * 获取最近的条码数据
     */
    public static List<String> getRecentBarcodeData() throws SQLException {
        List<String> barcodes = new ArrayList<>();
        String sql = "SELECT barcode FROM barcode_data ORDER BY created_at DESC LIMIT 100";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                barcodes.add(rs.getString("barcode"));
            }
        }
        
        return barcodes;
    }
    
    /**
     * 清空所有日志
     */
    public static void clearAllLogs() throws SQLException {
        String[] tables = {"plc_data", "barcode_data", "validation_result", "program_result"};
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                for (String table : tables) {
                    String sql = "DELETE FROM " + table;
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
    
    /**
     * 根据日志类型清空日志
     */
    public static void clearLogsByType(String logType) throws SQLException {
        String tableName = "";
        
        if ("PLC数据".equals(logType)) {
            tableName = "plc_data";
        } else if ("条码数据".equals(logType)) {
            tableName = "barcode_data";
        } else if ("验证结果".equals(logType)) {
            tableName = "validation_result";
        } else if ("烧录结果".equals(logType)) {
            tableName = "program_result";
        }
        
        if (!tableName.isEmpty()) {
            String sql = "DELETE FROM " + tableName;
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.executeUpdate();
            }
        }
    }
    
    /**
     * 根据天数清理过期日志
     */
    public static int cleanupLogsByDays(int days) throws SQLException {
        int totalDeleted = 0;
        String cutoffDate = LocalDateTime.now().minusDays(days).toString();
        String[] tables = {"plc_data", "barcode_data", "validation_result", "program_result"};
        
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                for (String table : tables) {
                    String sql = "DELETE FROM " + table + " WHERE created_at < ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                        pstmt.setTimestamp(1, Timestamp.valueOf(cutoffDate));
                        totalDeleted += pstmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        
        return totalDeleted;
    }
    // TaskDetail相关操作
    public static void saveTaskDetail(TaskDetail detail) throws SQLException {
        if (detail.getId() > 0) {
            // 更新现有详情
            String sql = "UPDATE task_details SET field_name = ?, field_value = ?, data_type = ?, required = ?, description = ? WHERE id = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, detail.getFieldName());
                pstmt.setString(2, detail.getFieldValue());
                pstmt.setString(3, detail.getDataType());
                pstmt.setBoolean(4, detail.isRequired());
                pstmt.setString(5, detail.getDescription());
                pstmt.setInt(6, detail.getId());
                
                pstmt.executeUpdate();
            }
        } else {
            // 添加新详情
            String sql = "INSERT INTO task_details (task_id, field_name, field_value, data_type, required, description) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, detail.getTaskId());
                pstmt.setString(2, detail.getFieldName());
                pstmt.setString(3, detail.getFieldValue());
                pstmt.setString(4, detail.getDataType());
                pstmt.setBoolean(5, detail.isRequired());
                pstmt.setString(6, detail.getDescription());
                
                pstmt.executeUpdate();
            }
        }
    }
    
    public static List<TaskDetail> getTaskDetailsByTaskId(int taskId) throws SQLException {
        List<TaskDetail> details = new ArrayList<>();
        String sql = "SELECT * FROM task_details WHERE task_id = ? ORDER BY field_name";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, taskId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                TaskDetail detail = new TaskDetail();
                detail.setId(rs.getInt("id"));
                detail.setTaskId(rs.getInt("task_id"));
                detail.setFieldName(rs.getString("field_name"));
                detail.setFieldValue(rs.getString("field_value"));
                detail.setDataType(rs.getString("data_type"));
                detail.setRequired(rs.getBoolean("required"));
                detail.setDescription(rs.getString("description"));
                detail.setCreatedAt(rs.getTimestamp("created_at"));
                
                details.add(detail);
            }
        }
        return details;
    }
    
    public static void deleteTaskDetail(int detailId) throws SQLException {
        String sql = "DELETE FROM task_details WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, detailId);
            pstmt.executeUpdate();
        }
    }
    
    public static TaskItem getTaskById(int taskId) throws SQLException {
        String sql = "SELECT id, task_name, cron_expression, device_id, status, remark, start_time, last_execute_time FROM tasks WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, taskId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new TaskItem(
                        rs.getInt("id"),
                        rs.getString("task_name"),
                        rs.getString("cron_expression"),
                        rs.getString("device_id"),
                        rs.getInt("status"),
                        rs.getString("remark"),
                        rs.getTimestamp("start_time") != null ? rs.getTimestamp("start_time").toLocalDateTime() : null,
                        rs.getTimestamp("last_execute_time") != null ? rs.getTimestamp("last_execute_time").toLocalDateTime() : null
                    );
                }
            }
        }
        return null;
    }
    
    // 修改saveBarcodeData方法，通过LogManager调用
    public static void saveBarcodeData(String deviceId, String barcode, String portName) throws SQLException {
        LogManager.getInstance().saveBarcodeData(deviceId, barcode, portName);
    }
    public static void saveBarcodeData(BarcodeData barcodeData) throws SQLException {
        LogManager.getInstance().saveBarcodeData(barcodeData.getDeviceId(), barcodeData.getBarcode(), barcodeData.getPortName());
    }
    
    public static List<BarcodeData> getAllBarcodes() throws SQLException {
        List<BarcodeData> barcodes = new ArrayList<>();
        String sql = "SELECT * FROM barcode_data ORDER BY scan_time DESC";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String deviceId = rs.getString("device_id");
                String barcode = rs.getString("barcode");
                String portName = rs.getString("port_name");
                
                BarcodeData barcodeData = new BarcodeData(deviceId, barcode, portName);
                barcodes.add(barcodeData);
            }
        }
        return barcodes;
    }
    
    // 修改saveValidationResult方法，通过LogManager调用
    public static void saveValidationResult(boolean isValid, String message, int expectedCount, int actualCount) throws SQLException {
        LogManager.getInstance().saveValidationResult(isValid, message, expectedCount, actualCount);
    }
    
    // 修改saveProgramResult方法，通过LogManager调用
    public static void saveProgramResult(String deviceId, String barcode, boolean result, String remark, Timestamp programTime) throws SQLException {
        LogManager.getInstance().saveProgramResult(deviceId, barcode, result, remark, programTime);
    }
    
    /**
     * 保存烧录结果到数据库
     * @param result 烧录结果
     */
    public static void saveProgramResult(ProgramResult result) {
        Logger.getInstance().debug("开始处理ProgramResult对象保存");
        if (result == null) {
            Logger.getInstance().error("传入的ProgramResult对象为空");
            return;
        }
        
        try {
            Logger.getInstance().debug("ProgramResult对象内容: " + result.toString());
            
            // 将String类型的result转换为boolean类型
            boolean success = "success".equalsIgnoreCase(result.getResult()) || 
                             "true".equalsIgnoreCase(result.getResult()) || 
                             "1".equals(result.getResult());
            
            Logger.getInstance().debug("转换后的结果值: " + success);
            Logger.getInstance().debug("准备调用LogManager保存数据");
            
            // 直接调用LogManager的保存方法，传入必要的参数
            LogManager.getInstance().saveProgramResult(
                result.getDeviceId() != null ? result.getDeviceId() : "unknown_device",
                result.getCode() != null ? result.getCode() : "",
                success,
                result.getRem() != null ? result.getRem() : "",
                result.getTime()
            );
            
            Logger.getInstance().debug("LogManager.saveProgramResult调用完成");
        } catch (Exception e) {
            Logger.getInstance().error("保存烧录结果失败: " + e.getMessage(), e);
            // 强制直接保存到数据库，绕过LogManager的shouldLog检查
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO program_result (device_id, barcode, result, error_message, program_time) VALUES (?, ?, ?, ?, ?)")) {
                
                boolean success = "success".equalsIgnoreCase(result.getResult()) || 
                                 "true".equalsIgnoreCase(result.getResult()) || 
                                 "1".equals(result.getResult());
                
                pstmt.setString(1, result.getDeviceId() != null ? result.getDeviceId() : "unknown_device");
                pstmt.setString(2, result.getCode() != null ? result.getCode() : "");
                pstmt.setBoolean(3, success);
                pstmt.setString(4, result.getRem() != null ? result.getRem() : "");
                pstmt.setTimestamp(5, result.getTime());
                
                pstmt.executeUpdate();
                Logger.getInstance().info("通过备用方案成功保存烧录结果到数据库");
            } catch (SQLException ex) {
                Logger.getInstance().error("备用保存方案也失败: " + ex.getMessage(), ex);
            }
        }
    }
    
    public static List<ProgramResult> getProgramResultsByBatchId(String batchId) throws SQLException {
        List<ProgramResult> results = new ArrayList<>();
        String sql = "SELECT * FROM program_result WHERE batch_id = ? ORDER BY program_time DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, batchId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String deviceId = rs.getString("device_id");
                String barcode = rs.getString("barcode");
                boolean success = rs.getBoolean("result");
                String errorMessage = rs.getString("error_message");
                Timestamp timestamp = rs.getTimestamp("program_time");
                
                // 创建ProgramResult对象
                ProgramResult programResult = new ProgramResult(barcode, success ? "成功" : "失败", deviceId);
                programResult.setTime(timestamp);
                programResult.setRem(errorMessage != null ? errorMessage : "");
                results.add(programResult);
            }
        }
        return results;
    }
    
    public static List<ProgramResult> getAllProgramResults() throws SQLException {
        List<ProgramResult> results = new ArrayList<>();
        String sql = "SELECT * FROM program_result ORDER BY program_time DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                String deviceId = rs.getString("device_id");
                String barcode = rs.getString("barcode");
                boolean success = rs.getBoolean("result");
                String errorMessage = rs.getString("error_message");
                Timestamp timestamp = rs.getTimestamp("program_time");
                
                // 创建ProgramResult对象
                ProgramResult programResult = new ProgramResult(barcode, success ? "成功" : "失败", deviceId);
                programResult.setTime(timestamp);
                programResult.setRem(errorMessage != null ? errorMessage : "");
                results.add(programResult);
            }
            
            logger.debug("获取所有烧录结果成功，共" + results.size() + "条记录");
            return results;
        } catch (SQLException e) {
            logger.error("获取所有烧录结果失败: " + e.getMessage(), e);
            throw e;
        }
    }
    
    // ConfigItem相关操作
    public static void saveConfigItem(ConfigItem configItem) throws SQLException {
        if (configItem.getId() > 0) {
            // 更新现有配置项
            String sql = "UPDATE config_items SET config_key = ?, config_value = ?, description = ?, data_type = ?, required = ? WHERE id = ?";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, configItem.getConfigKey());
                pstmt.setString(2, configItem.getConfigValue());
                pstmt.setString(3, configItem.getDescription());
                pstmt.setString(4, configItem.getDataType());
                pstmt.setBoolean(5, configItem.isRequired());
                pstmt.setInt(6, configItem.getId());
                
                pstmt.executeUpdate();
                logger.debug("更新配置项成功: " + configItem.getConfigKey());
            } catch (SQLException e) {
                logger.error("更新配置项失败: " + configItem.getConfigKey() + " - " + e.getMessage(), e);
                throw e;
            }
        } else {
            // 添加新配置项
            String sql = "INSERT INTO config_items (config_key, config_value, description, data_type, required) " +
                        "VALUES (?, ?, ?, ?, ?)";
            
            try (Connection conn = getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, configItem.getConfigKey());
                pstmt.setString(2, configItem.getConfigValue());
                pstmt.setString(3, configItem.getDescription());
                pstmt.setString(4, configItem.getDataType());
                pstmt.setBoolean(5, configItem.isRequired());
                
                pstmt.executeUpdate();
                logger.debug("添加新配置项成功: " + configItem.getConfigKey());
            } catch (SQLException e) {
                logger.error("添加新配置项失败: " + configItem.getConfigKey() + " - " + e.getMessage(), e);
                throw e;
            }
        }
    }
    
    public static List<ConfigItem> getAllConfigItems() throws SQLException {
        List<ConfigItem> configItems = new ArrayList<>();
        String sql = "SELECT * FROM config_items ORDER BY config_key";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                ConfigItem item = new ConfigItem();
                item.setId(rs.getInt("id"));
                item.setConfigKey(rs.getString("config_key"));
                item.setConfigValue(rs.getString("config_value"));
                item.setDescription(rs.getString("description"));
                item.setDataType(rs.getString("data_type"));
                item.setRequired(rs.getBoolean("required"));
                item.setCreatedAt(rs.getTimestamp("created_at"));
                
                configItems.add(item);
            }
        }
        return configItems;
    }
    
    public static ConfigItem getConfigItemById(int configId) throws SQLException {
        String sql = "SELECT * FROM config_items WHERE id = ?";
        ConfigItem configItem = null;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, configId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                configItem = new ConfigItem();
                configItem.setId(rs.getInt("id"));
                configItem.setConfigKey(rs.getString("config_key"));
                configItem.setConfigValue(rs.getString("config_value"));
                configItem.setDescription(rs.getString("description"));
                configItem.setDataType(rs.getString("data_type"));
                configItem.setRequired(rs.getBoolean("required"));
                configItem.setCreatedAt(rs.getTimestamp("created_at"));
            }
        }
        return configItem;
    }
    
    public static boolean deleteConfigItem(int configId) throws SQLException {
        String sql = "DELETE FROM config_items WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, configId);
            pstmt.executeUpdate();
            logger.debug("删除配置项成功: ID=" + configId);
        } catch (SQLException e) {
            logger.error("删除配置项失败: ID=" + configId + " - " + e.getMessage(), e);
            throw e;
        }
        return true;
    }
    
    /**
     * 根据ID获取任务信息
     * @param taskId 任务ID
     * @return 任务信息对象
     * @throws SQLException 数据库异常
     */
    public static TaskInfo getTaskInfoById(int taskId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, taskId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    TaskInfo taskInfo = new TaskInfo();
                    taskInfo.setId(rs.getInt("id"));
                    taskInfo.setDeviceId(rs.getString("device_id"));
                    taskInfo.setCronExpression(rs.getString("cron_expression"));
                    taskInfo.setDescription(rs.getString("description"));
                    taskInfo.setTaskType(rs.getString("task_type"));
                    taskInfo.setTaskName(rs.getString("task_name"));
                    taskInfo.setEnabled(rs.getBoolean("enabled"));
                    
                    // 处理创建时间
                    String createdAtStr = rs.getString("created_at");
                    if (createdAtStr != null) {
                        taskInfo.setCreatedAt(LocalDateTime.parse(createdAtStr));
                    }
                    
                    logger.debug("获取任务信息成功: ID=" + taskId);
                    return taskInfo;
                }
            }
        } catch (SQLException e) {
            logger.error("获取任务信息失败: ID=" + taskId + " - " + e.getMessage(), e);
            throw e;
        }
        
        return null; // 未找到对应的任务
    }
    
    /**
     * 删除任务信息
     * @param taskId 任务ID
     * @throws SQLException 数据库异常
     */
    public static void deleteTaskInfo(int taskId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, taskId);
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                logger.debug("删除任务成功: ID=" + taskId);
            } else {
                logger.warn("未找到要删除的任务: ID=" + taskId);
            }
        } catch (SQLException e) {
            logger.error("删除任务失败: ID=" + taskId + " - " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 更新任务状态
     * @param taskId 任务ID
     * @param status 新状态值
     * @throws SQLException 数据库异常
     */
    public static void updateTaskInfoStatus(int taskId, int status) throws SQLException {
        String sql = "UPDATE tasks SET status = ? WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, status);
            pstmt.setInt(2, taskId);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.debug("更新任务状态成功: ID=" + taskId + ", Status=" + status);
            } else {
                logger.warn("未找到要更新状态的任务: ID=" + taskId);
            }
        } catch (SQLException e) {
            logger.error("更新任务状态失败: ID=" + taskId + " - " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 更新任务信息
     * @param taskInfo 任务信息对象
     * @throws SQLException 数据库异常
     */
    public static void updateTaskInfo(TaskInfo taskInfo) throws SQLException {
        String sql = "UPDATE tasks SET device_id = ?, cron_expression = ?, description = ?, " +
                    "task_type = ?, task_name = ?, enabled = ?, remark = ?, " +
                    "start_time = ?, status = ?, last_execute_time = ? WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, taskInfo.getDeviceId());
            pstmt.setString(2, taskInfo.getCronExpression());
            pstmt.setString(3, taskInfo.getDescription());
            pstmt.setString(4, taskInfo.getTaskType());
            pstmt.setString(5, taskInfo.getTaskName());
            pstmt.setBoolean(6, taskInfo.isEnabled());
            pstmt.setString(7, taskInfo.getRemark());
            
            // 处理LocalDateTime类型
            if (taskInfo.getStartTime() != null) {
                pstmt.setString(8, taskInfo.getStartTime().toString());
            } else {
                pstmt.setNull(8, Types.VARCHAR);
            }
            
            pstmt.setInt(9, taskInfo.getStatus());
            
            if (taskInfo.getLastExecuteTime() != null) {
                pstmt.setString(10, taskInfo.getLastExecuteTime().toString());
            } else {
                pstmt.setNull(10, Types.VARCHAR);
            }
            
            pstmt.setInt(11, taskInfo.getId());
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.debug("更新任务信息成功: ID=" + taskInfo.getId());
            } else {
                logger.warn("未找到要更新的任务: ID=" + taskInfo.getId());
            }
        } catch (SQLException e) {
            logger.error("更新任务信息失败: ID=" + taskInfo.getId() + " - " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 获取所有任务信息
     * @return 任务信息列表
     * @throws SQLException 数据库异常
     */
    public static List<TaskInfo> getAllTaskInfo() throws SQLException {
        List<TaskInfo> taskList = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY id DESC";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                TaskInfo taskInfo = new TaskInfo();
                taskInfo.setId(rs.getInt("id"));
                taskInfo.setDeviceId(rs.getString("device_id"));
                taskInfo.setCronExpression(rs.getString("cron_expression"));
                taskInfo.setDescription(rs.getString("description"));
                taskInfo.setTaskType(rs.getString("task_type"));
                taskInfo.setTaskName(rs.getString("task_name"));
                taskInfo.setEnabled(rs.getBoolean("enabled"));
                taskInfo.setRemark(rs.getString("remark"));
                taskInfo.setStatus(rs.getInt("status"));
                
                // 处理LocalDateTime类型
                String createdAtStr = rs.getString("created_at");
                if (createdAtStr != null) {
                    taskInfo.setCreatedAt(LocalDateTime.parse(createdAtStr));
                }
                
                String startTimeStr = rs.getString("start_time");
                if (startTimeStr != null) {
                    taskInfo.setStartTime(LocalDateTime.parse(startTimeStr));
                }
                
                String lastExecuteTimeStr = rs.getString("last_execute_time");
                if (lastExecuteTimeStr != null) {
                    taskInfo.setLastExecuteTime(LocalDateTime.parse(lastExecuteTimeStr));
                }
                
                taskList.add(taskInfo);
            }
            
            logger.debug("获取所有任务信息成功，共" + taskList.size() + "条");
        } catch (SQLException e) {
            logger.error("获取所有任务信息失败: " + e.getMessage(), e);
            throw e;
        }
        
        return taskList;
    }
    
    /**
     * 添加任务信息
     * @param taskInfo 任务信息对象
     * @throws SQLException 数据库异常
     */
    public static void addTaskInfo(TaskInfo taskInfo) throws SQLException {
        String sql = "INSERT INTO tasks (device_id, cron_expression, description, task_type, " +
                    "task_name, enabled, remark, start_time, status, last_execute_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, taskInfo.getDeviceId());
            pstmt.setString(2, taskInfo.getCronExpression());
            pstmt.setString(3, taskInfo.getDescription());
            pstmt.setString(4, taskInfo.getTaskType());
            pstmt.setString(5, taskInfo.getTaskName());
            pstmt.setBoolean(6, taskInfo.isEnabled());
            pstmt.setString(7, taskInfo.getRemark());
            
            // 处理LocalDateTime类型
            if (taskInfo.getStartTime() != null) {
                pstmt.setString(8, taskInfo.getStartTime().toString());
            } else {
                pstmt.setNull(8, Types.VARCHAR);
            }
            
            pstmt.setInt(9, taskInfo.getStatus());
            
            if (taskInfo.getLastExecuteTime() != null) {
                pstmt.setString(10, taskInfo.getLastExecuteTime().toString());
            } else {
                pstmt.setNull(10, Types.VARCHAR);
            }
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.debug("添加任务信息成功: " + taskInfo.getTaskName());
            }
        } catch (SQLException e) {
            logger.error("添加任务信息失败: " + e.getMessage(), e);
            throw e;
        }
    }
    
    public static void addTask(TaskItem taskItem) throws SQLException {
        String sql = "INSERT INTO tasks (task_name, cron_expression, device_id, status, remark, start_time, last_execute_time) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, taskItem.getTaskName());
            stmt.setString(2, taskItem.getCronExpression());
            stmt.setString(3, taskItem.getDeviceId());
            stmt.setInt(4, taskItem.getStatus());
            if (taskItem.getRemark() != null) {
                stmt.setString(5, taskItem.getRemark());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            if (taskItem.getStartTime() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(taskItem.getStartTime()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            if (taskItem.getLastExecuteTime() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(taskItem.getLastExecuteTime()));
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }
            stmt.executeUpdate();
            logger.info("添加任务成功: " + taskItem.getTaskName());
        } catch (SQLException e) {
            logger.error("添加任务失败: " + e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 更新所有任务状态
     * @param status 新状态值
     * @throws SQLException 数据库异常
     */
    public static void updateAllTasksStatus(int status) throws SQLException {
        String sql = "UPDATE tasks SET status = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, status);
            int rowsAffected = pstmt.executeUpdate();
            
            logger.debug("更新所有任务状态成功，共" + rowsAffected + "条任务");
        } catch (SQLException e) {
            logger.error("更新所有任务状态失败: " + e.getMessage(), e);
            throw e;
        }
    }
    
    public static void updateTask(TaskItem taskItem) throws SQLException {
        String sql = "UPDATE tasks SET task_name = ?, cron_expression = ?, device_id = ?, status = ?, " +
                     "remark = ?, start_time = ?, last_execute_time = ? WHERE id = ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, taskItem.getTaskName());
            stmt.setString(2, taskItem.getCronExpression());
            stmt.setString(3, taskItem.getDeviceId());
            stmt.setInt(4, taskItem.getStatus());
            if (taskItem.getRemark() != null) {
                stmt.setString(5, taskItem.getRemark());
            } else {
                stmt.setNull(5, Types.VARCHAR);
            }
            if (taskItem.getStartTime() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(taskItem.getStartTime()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            if (taskItem.getLastExecuteTime() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(taskItem.getLastExecuteTime()));
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }
            stmt.setInt(8, taskItem.getId());
            stmt.executeUpdate();
            logger.info("更新任务成功，任务ID: " + taskItem.getId());
        } catch (SQLException e) {
            logger.error("更新任务失败: " + e.getMessage(), e);
            throw e;
        }
    }
}