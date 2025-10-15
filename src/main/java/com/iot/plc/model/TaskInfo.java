package com.iot.plc.model;

import java.time.LocalDateTime;

/**
 * 任务信息类
 */
public class TaskInfo {
    private int id;
    private String deviceId;
    private String cronExpression;
    private String description;
    private String taskType;
    private String taskName;
    private boolean enabled;
    private LocalDateTime createdAt;
    private String remark;
    private LocalDateTime startTime;
    private int status;
    private LocalDateTime lastExecuteTime;

    // 无参构造函数
    public TaskInfo() {
    }

    // 有参构造函数
    public TaskInfo(int id, String deviceId, String cronExpression, String description, 
                   String taskType, String taskName, boolean enabled, LocalDateTime createdAt,
                   String remark, LocalDateTime startTime, int status, LocalDateTime lastExecuteTime) {
        this.id = id;
        this.deviceId = deviceId;
        this.cronExpression = cronExpression;
        this.description = description;
        this.taskType = taskType;
        this.taskName = taskName;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.remark = remark;
        this.startTime = startTime;
        this.status = status;
        this.lastExecuteTime = lastExecuteTime;
    }

    // getter和setter方法
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
    
    public LocalDateTime getLastExecuteTime() {
        return lastExecuteTime;
    }
    
    public void setLastExecuteTime(LocalDateTime lastExecuteTime) {
        this.lastExecuteTime = lastExecuteTime;
    }
}