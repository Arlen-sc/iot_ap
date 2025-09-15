package com.iot.plc.scheduler;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.logger.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class LogCleanupJob implements Job {
    private static final String LOG_RETENTION_PERIOD_KEY = "log_retention_period";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        Logger.getInstance().info("开始执行日志清理任务");
        try {
            int retentionDays = getLogRetentionPeriod();
            Logger.getInstance().info("当前日志清理周期: " + retentionDays + " 天");

            if (retentionDays == 0) {
                // 不记录日志 - 清空所有日志
                cleanupAllLogs();
                Logger.getInstance().info("日志清理周期设置为0，已清空所有日志");
            } else if (retentionDays > 0) {
                // 按天数清理日志
                LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
                cleanupLogsBefore(cutoffTime);
                Logger.getInstance().info("已清理 " + retentionDays + " 天前的历史日志");
            }
        } catch (Exception e) {
            Logger.getInstance().error("执行日志清理任务失败: " + e.getMessage());
            throw new JobExecutionException("日志清理任务执行失败", e);
        }
        Logger.getInstance().info("日志清理任务执行完成");
    }

    private int getLogRetentionPeriod() {
        try {
            String sql = "SELECT config_value FROM config_items WHERE config_key = ?";
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, LOG_RETENTION_PERIOD_KEY);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String value = rs.getString(1);
                    return Integer.parseInt(value);
                }
            }
        } catch (SQLException e) {
            Logger.getInstance().error("获取日志保留周期配置失败: " + e.getMessage());
        } catch (NumberFormatException e) {
            Logger.getInstance().error("日志保留周期配置格式错误: " + e.getMessage());
        }
        // 默认值：不清理日志（实际是不保留日志，因为0表示不记录日志）
        return 0;
    }

    private void cleanupAllLogs() throws SQLException {
        // 清空条码数据日志
        cleanupTable("barcode_data");
        
        // 清空程序执行结果日志
        cleanupTable("program_result");
        
        // 如果有其他日志表，也需要在这里添加对应的清理逻辑
        Logger.getInstance().info("已清空所有日志表数据");
    }

    private void cleanupLogsBefore(LocalDateTime cutoffTime) throws SQLException {
        String cutoffDateStr = cutoffTime.format(DATE_FORMATTER);
        
        // 清理条码数据日志
        cleanupTableBefore("barcode_data", "scan_time", cutoffDateStr);
        
        // 清理程序执行结果日志
        cleanupTableBefore("program_result", "timestamp", cutoffDateStr);
        
        // 如果有其他日志表，也需要在这里添加对应的清理逻辑
        Logger.getInstance().info("已清理所有早于 " + cutoffDateStr + " 的历史日志");
    }

    private void cleanupTable(String tableName) throws SQLException {
        String sql = "DELETE FROM " + tableName;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int deletedRows = pstmt.executeUpdate();
            Logger.getInstance().info("从表 " + tableName + " 中删除了 " + deletedRows + " 条记录");
        }
    }

    private void cleanupTableBefore(String tableName, String dateColumn, String cutoffDateStr) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE " + dateColumn + " < ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, cutoffDateStr);
            int deletedRows = pstmt.executeUpdate();
            Logger.getInstance().info("从表 " + tableName + " 中删除了 " + deletedRows + " 条历史记录");
        }
    }
}