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
    private boolean shouldLog = true; // 默认记录日志

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
                // 设置为0表示不记录日志
                shouldLog = days != 0;
                Logger.getInstance().info("日志记录配置更新: " + (shouldLog ? "已启用" : "已禁用"));
            } else {
                // 如果配置不存在，设置默认值并保存到数据库
                String defaultDays = "7"; // 默认保留7天日志
                shouldLog = true;
                configService.saveConfigItem(LOG_RETENTION_PERIOD_KEY, defaultDays, "日志保留天数，0表示不记录日志");
                Logger.getInstance().info("日志记录配置不存在，已创建默认配置: 保留7天");
            }
        } catch (Exception e) {
            // 配置读取失败时，默认记录日志
            shouldLog = true;
            Logger.getInstance().error("读取日志配置失败: " + e.getMessage());
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
    public void saveProgramResult(String batchId, String deviceId, String barcode, boolean result, String errorMessage, LocalDateTime programTime) {
        if (!shouldLog) {
            return;
        }
        
        String sql = "INSERT INTO program_result (batch_id, device_id, barcode, result, error_message, program_time) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, batchId);
            pstmt.setString(2, deviceId);
            pstmt.setString(3, barcode);
            pstmt.setBoolean(4, result);
            pstmt.setString(5, errorMessage);
            pstmt.setTimestamp(6, Timestamp.valueOf(programTime));
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            Logger.getInstance().error("保存烧录结果日志失败: " + e.getMessage());
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