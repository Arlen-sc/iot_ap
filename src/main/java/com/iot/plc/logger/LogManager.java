package com.iot.plc.logger;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.service.ConfigService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class LogManager {
    private static final String LOG_RETENTION_PERIOD_KEY = "log_retention_period";
    private static volatile LogManager instance;
    private final ConfigService configService;
    private boolean shouldLog = true; // 默认开启日志

    private LogManager() {
        configService = ConfigService.getInstance();
        // 初始化时检查日志配置
        checkLogConfig();
    }

    public static LogManager getInstance() {
        if (instance == null) {
            synchronized (LogManager.class) {
                if (instance == null) {
                    instance = new LogManager();
                }
            }
        }
        return instance;
    }

    /**
     * 检查日志配置，决定是否应该记录日志
     */
    public void checkLogConfig() {
        try {
            String retentionDays = configService.getConfigValueByKey(LOG_RETENTION_PERIOD_KEY);
            if (retentionDays != null) {
                int days = Integer.parseInt(retentionDays);
                Logger.getInstance().debug("日志保留天数配置值: " + days);
                // 0表示永不清除日志，无论设置为何值都应该记录日志
                shouldLog = true;
                Logger.getInstance().info("日志记录状态设置为: 开启 (保留天数: " + days + ")");
            } else {
                // 如果配置不存在，设置默认值并保存到数据库
                String defaultDays = "0"; // 默认永不清除日志
                shouldLog = true;
                configService.saveConfigItem(LOG_RETENTION_PERIOD_KEY, defaultDays, "日志保留天数，0表示永不清除日志", "string");
                Logger.getInstance().info("日志记录配置不存在，已创建默认配置: 永不清除日志");
            }
        } catch (Exception e) {
            // 配置读取失败时，默认开启日志且永不清除
            shouldLog = true;
            Logger.getInstance().error("读取日志配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存条码数据日志
     */
    public void saveBarcodeData(String deviceId, String barcode, String portName) {
        if (!shouldLog) {
            return;
        }
        
        String sql = "INSERT INTO barcode_data (device_id, barcode, scan_time, port_name) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, deviceId);
            pstmt.setString(2, barcode);
            pstmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(4, portName);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.getInstance().error("保存条码数据日志失败: " + e.getMessage());
        }
    }

    /**
     * 保存验证结果日志
     */
    public void saveValidationResult(boolean isValid, String message, int expectedCount, int actualCount) {
        if (!shouldLog) {
            return;
        }
        
        String sql = "INSERT INTO validation_result (is_valid, message, expected_count, actual_count) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBoolean(1, isValid);
            pstmt.setString(2, message);
            pstmt.setInt(3, expectedCount);
            pstmt.setInt(4, actualCount);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.getInstance().error("保存验证结果日志失败: " + e.getMessage());
        }
    }

    /**
     * 保存烧录结果日志
     */
    public void saveProgramResult(String deviceId, String barcode, boolean result, String remark, Timestamp programTime) {
        Logger.getInstance().debug("尝试保存烧录结果: deviceId=" + deviceId + ", barcode=" + barcode + ", result=" + result + ", remark=" + remark + ", programTime=" + programTime);
        Logger.getInstance().debug("shouldLog标志值: " + shouldLog);
        
        if (!shouldLog) {
            Logger.getInstance().debug("日志记录已禁用，跳过保存烧录结果");
            return;
        }
        
        // 修改SQL语句，移除batch_id字段，将remark改为error_message以匹配表结构
        // 不插入created_at字段，让数据库使用默认值CURRENT_TIMESTAMP
        String sql = "INSERT INTO program_result (device_id, barcode, result, error_message, program_time,created_at) VALUES (?, ?, ?, ?, ?,?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, deviceId);
            pstmt.setString(2, barcode);
            pstmt.setBoolean(3, result);
            pstmt.setString(4, remark);
            pstmt.setTimestamp(5, programTime);
            pstmt.setTimestamp(6, programTime);
            
            pstmt.executeUpdate();
            Logger.getInstance().debug("烧录结果保存成功: deviceId=" + deviceId + ", barcode=" + barcode);
        } catch (SQLException e) {
            Logger.getInstance().error("保存烧录结果日志失败: " + e.getMessage(), e);
        } catch (Exception e) {
            Logger.getInstance().error("保存烧录结果时发生意外错误: " + e.getMessage(), e);
        }
    }

    /**
     * 保存PLC数据日志
     */
    public void savePlcData(String deviceId, String jsonData) {
        if (!shouldLog) {
            return;
        }
        
        String sql = "INSERT INTO plc_data (device_id, data_json) VALUES (?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, deviceId);
            pstmt.setString(2, jsonData);
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.getInstance().error("保存PLC数据日志失败: " + e.getMessage());
        }
    }

    /**
     * 获取当前日志记录状态
     */
    public boolean isLoggingEnabled() {
        return shouldLog;
    }
}