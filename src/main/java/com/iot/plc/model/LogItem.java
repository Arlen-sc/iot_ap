package com.iot.plc.model;

import java.time.LocalDateTime;

/**
 * 日志项模型类
 * 用于存储各类日志数据
 */
public class LogItem {
    private String logType;
    private LocalDateTime timestamp;
    private String dataContent;
    private String status;

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDataContent() {
        return dataContent;
    }

    public void setDataContent(String dataContent) {
        this.dataContent = dataContent;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}