package com.iot.plc.model;

import java.time.LocalDateTime;

/**
 * 任务项模型类
 */
public class TaskItem {
    private int id;
    private String taskName;
    private String cronExpression;
    private String deviceId;
    private int status;
    private String remark;
    private LocalDateTime startTime;
    private LocalDateTime lastExecuteTime;

    // 无参构造函数
    public TaskItem() {
    }
    
    // 有参构造函数
    public TaskItem(int id, String taskName, String cronExpression, String deviceId, int status, 
                   String remark, LocalDateTime startTime, LocalDateTime lastExecuteTime) {
        this.id = id;
        this.taskName = taskName;
        this.cronExpression = cronExpression;
        this.deviceId = deviceId;
        this.status = status;
        this.remark = remark;
        this.startTime = startTime;
        this.lastExecuteTime = lastExecuteTime;
    }

    // Getter和Setter方法
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
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

    public LocalDateTime getLastExecuteTime() {
        return lastExecuteTime;
    }

    public void setLastExecuteTime(LocalDateTime lastExecuteTime) {
        this.lastExecuteTime = lastExecuteTime;
    }
    
    // 额外的方法，满足JavaFXTaskListPanel的调用需求
    public String getPlcAddress() {
        // 此处可以根据实际需求实现
        return "";
    }
    
    public void setPlcAddress(String plcAddress) {
        // 此处可以根据实际需求实现
    }
    
    public int getBarcodeCount() {
        // 此处可以根据实际需求实现
        return 0;
    }
    
    public void setBarcodeCount(int barcodeCount) {
        // 此处可以根据实际需求实现
    }
    
    public int getProcessedCount() {
        // 此处可以根据实际需求实现
        return 0;
    }
    
    public void setProcessedCount(int processedCount) {
        // 此处可以根据实际需求实现
    }
    
    public LocalDateTime getEndTime() {
        // 此处可以根据实际需求实现
        return null;
    }
    
    public String getBurnStatus() {
        // 此处可以根据实际需求实现
        return "";
    }
    
    public void setBurnStatus(String burnStatus) {
        // 此处可以根据实际需求实现
    }
}