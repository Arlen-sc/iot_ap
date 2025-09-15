package com.iot.plc.database;

import com.iot.plc.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:plc_tasks.db";
    
    static {
        try {
            Class.forName("org.sqlite.JDBC");
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private static void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
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
                    "batch_id VARCHAR(50) NOT NULL," +
                    "device_id VARCHAR(20) NOT NULL," +
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
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_program_batch ON program_result(batch_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_program_device ON program_result(device_id)");
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static Connection getConnection() throws SQLException {
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
    
    public static List<Task> getAllTasks() throws SQLException {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY created_at DESC";
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Task task = new Task();
                task.setId(rs.getInt("id"));
                task.setDeviceId(rs.getString("device_id"));
                task.setCronExpression(rs.getString("cron_expression"));
                task.setDescription(rs.getString("description"));
                task.setTaskType(rs.getString("task_type"));
                task.setTaskName(rs.getString("task_name"));
                task.setEnabled(rs.getBoolean("enabled"));
                
                tasks.add(task);
            }
        }
        return tasks;
    }
    
    public static void savePlcData(String deviceId, String jsonData) throws SQLException {
        String sql = "INSERT INTO plc_data (device_id, data_json) VALUES (?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, deviceId);
            pstmt.setString(2, jsonData);
            
            pstmt.executeUpdate();
        }
    }
    
    public static void deleteTask(int taskId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, taskId);
            pstmt.executeUpdate();
        }
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
    
    public static Task getTaskById(int taskId) throws SQLException {
        String sql = "SELECT * FROM tasks WHERE id = ?";
        Task task = null;
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, taskId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                task = new Task();
                task.setId(rs.getInt("id"));
                task.setDeviceId(rs.getString("device_id"));
                task.setCronExpression(rs.getString("cron_expression"));
                task.setDescription(rs.getString("description"));
                task.setTaskType(rs.getString("task_type"));
                task.setTaskName(rs.getString("task_name"));
                task.setEnabled(rs.getBoolean("enabled"));
            }
        }
        return task;
    }
    
    // 条码数据相关操作
    public static void saveBarcodeData(String deviceId, String barcode, String portName) throws SQLException {
        String sql = "INSERT INTO barcode_data (device_id, barcode, scan_time, port_name) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, deviceId);
            pstmt.setString(2, barcode);
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(4, portName);
            
            pstmt.executeUpdate();
        }
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
    
    // 验证结果相关操作
    public static void saveValidationResult(boolean isValid, String message, int expectedCount, int actualCount) throws SQLException {
        String sql = "INSERT INTO validation_result (is_valid, message, expected_count, actual_count) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBoolean(1, isValid);
            pstmt.setString(2, message);
            pstmt.setInt(3, expectedCount);
            pstmt.setInt(4, actualCount);
            
            pstmt.executeUpdate();
        }
    }
    
    // 烧录结果相关操作
    public static void saveProgramResult(String batchId, String deviceId, String barcode, boolean result, String errorMessage, LocalDateTime programTime) throws SQLException {
        String sql = "INSERT INTO program_result (batch_id, device_id, barcode, result, error_message, program_time) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, batchId);
            pstmt.setString(2, deviceId);
            pstmt.setString(3, barcode);
            pstmt.setBoolean(4, result);
            pstmt.setString(5, errorMessage);
            pstmt.setTimestamp(6, Timestamp.valueOf(programTime));
            
            pstmt.executeUpdate();
        }
    }
    
    public static List<ProgramResult> getProgramResultsByBatchId(String batchId) throws SQLException {
        List<ProgramResult> results = new ArrayList<>();
        String sql = "SELECT * FROM program_result WHERE batch_id = ? ORDER BY program_time DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, batchId);
            ResultSet rs = pstmt.executeQuery();
            
            ProgramResult programResult = new ProgramResult();
            programResult.setBatchId(batchId);
            
            while (rs.next()) {
                String deviceId = rs.getString("device_id");
                String barcode = rs.getString("barcode");
                boolean success = rs.getBoolean("result");
                String errorMessage = rs.getString("error_message");
                String timestamp = rs.getString("program_time");
                
                DeviceResult deviceResult = new DeviceResult(deviceId, barcode, success, errorMessage);
                programResult.addDeviceResult(deviceResult);
                programResult.setTimestamp(timestamp);
            }
            
            if (!programResult.getResults().isEmpty()) {
                results.add(programResult);
            }
        }
        return results;
    }
    
    public static List<ProgramResult> getAllProgramResults() throws SQLException {
        List<ProgramResult> results = new ArrayList<>();
        Map<String, ProgramResult> batchMap = new HashMap<>();
        String sql = "SELECT * FROM program_result ORDER BY batch_id, program_time DESC";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                String batchId = rs.getString("batch_id");
                String deviceId = rs.getString("device_id");
                String barcode = rs.getString("barcode");
                boolean success = rs.getBoolean("result");
                String errorMessage = rs.getString("error_message");
                String timestamp = rs.getString("program_time");
                
                // 获取或创建该批次的ProgramResult对象
                ProgramResult programResult = batchMap.get(batchId);
                if (programResult == null) {
                    programResult = new ProgramResult();
                    programResult.setBatchId(batchId);
                    programResult.setTimestamp(timestamp);
                    batchMap.put(batchId, programResult);
                }
                
                DeviceResult deviceResult = new DeviceResult(deviceId, barcode, success, errorMessage);
                programResult.addDeviceResult(deviceResult);
            }
            
            // 将Map中的所有ProgramResult对象添加到结果列表
            results.addAll(batchMap.values());
            
            // 按时间倒序排序
            results.sort((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()));
            
            return results;
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
    
    public static void deleteConfigItem(int configId) throws SQLException {
        String sql = "DELETE FROM config_items WHERE id = ?";
        
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, configId);
            pstmt.executeUpdate();
        }
    }
}