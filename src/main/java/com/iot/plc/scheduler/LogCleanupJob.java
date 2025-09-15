package com.iot.plc.scheduler;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.logger.Logger;
import com.iot.plc.model.ConfigItem;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@DisallowConcurrentExecution
public class LogCleanupJob implements Job {
    private static final String LOG_RETENTION_PERIOD_KEY = "log_retention_period";
    private static final int DEFAULT_RETENTION_PERIOD = 0; // 默认不保留日志

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 支持手动调用时传入null上下文
        Logger logger = Logger.getInstance();
        logger.info("开始执行日志清理任务");

        try {
            // 获取日志保留周期配置
            int retentionWeeks = getLogRetentionPeriod();
            logger.info("当前日志保留周期: " + retentionWeeks + " 周");

            // 如果保留周期为0，表示不保留日志，直接删除所有日志
            if (retentionWeeks == 0) {
                logger.info("日志保留周期为0，开始删除所有历史日志");
                cleanupAllLogs();
            } else {
                // 计算保留截止日期
                LocalDateTime cutoffDate = LocalDateTime.now().minus(retentionWeeks, ChronoUnit.WEEKS);
                logger.info("开始清理 " + cutoffDate + " 之前的历史日志");
                cleanupLogsBefore(cutoffDate);
            }

            logger.info("日志清理任务执行完成");
        } catch (Exception e) {
            logger.error("日志清理任务执行失败: " + e.getMessage());
            throw new JobExecutionException("日志清理任务执行失败", e);
        }
    }

    private int getLogRetentionPeriod() {
        try {
            List<ConfigItem> configItems = DatabaseManager.getAllConfigItems();
            for (ConfigItem item : configItems) {
                if (LOG_RETENTION_PERIOD_KEY.equals(item.getConfigKey())) {
                    try {
                        return Integer.parseInt(item.getConfigValue());
                    } catch (NumberFormatException e) {
                        Logger.getInstance().error("日志保留周期配置值格式错误: " + item.getConfigValue());
                        return DEFAULT_RETENTION_PERIOD;
                    }
                }
            }

            // 如果配置不存在，创建默认配置
            ConfigItem defaultConfig = new ConfigItem();
            defaultConfig.setConfigKey(LOG_RETENTION_PERIOD_KEY);
            defaultConfig.setConfigValue(String.valueOf(DEFAULT_RETENTION_PERIOD));
            defaultConfig.setDescription("日志保留周期(单位:周)，0表示不保留日志");
            defaultConfig.setDataType("integer");
            defaultConfig.setRequired(false);
            DatabaseManager.saveConfigItem(defaultConfig);

            return DEFAULT_RETENTION_PERIOD;
        } catch (SQLException e) {
            Logger.getInstance().error("获取日志保留周期配置失败: " + e.getMessage());
            return DEFAULT_RETENTION_PERIOD;
        }
    }

    private void cleanupAllLogs() throws SQLException {
        // 清理所有日志表
        cleanupTable("plc_data");
        cleanupTable("barcode_data");
        cleanupTable("validation_result");
        cleanupTable("program_result");
    }

    private void cleanupLogsBefore(LocalDateTime cutoffDate) throws SQLException {
        // 清理指定日期之前的日志
        cleanupTableBefore("plc_data", "created_at", cutoffDate);
        cleanupTableBefore("barcode_data", "scan_time", cutoffDate);
        cleanupTableBefore("validation_result", "created_at", cutoffDate);
        cleanupTableBefore("program_result", "program_time", cutoffDate);
    }

    private void cleanupTable(String tableName) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM " + tableName)) {
            int deletedRows = pstmt.executeUpdate();
            Logger.getInstance().info("清理表 " + tableName + "，删除了 " + deletedRows + " 条记录");
        }
    }

    private void cleanupTableBefore(String tableName, String dateColumn, LocalDateTime cutoffDate) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "DELETE FROM " + tableName + " WHERE " + dateColumn + " < ?")) {
            pstmt.setTimestamp(1, java.sql.Timestamp.valueOf(cutoffDate));
            int deletedRows = pstmt.executeUpdate();
            Logger.getInstance().info("清理表 " + tableName + " 中 " + cutoffDate + " 之前的记录，删除了 " + deletedRows + " 条记录");
        }
    }
}