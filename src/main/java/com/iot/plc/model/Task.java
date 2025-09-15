package com.iot.plc.model;

public class Task {
    private int id;
    private String deviceId;
    private String cronExpression;
    private String description;
    private String taskType;
    private String taskName;
    private boolean enabled;
    
    public Task() {}
    
    public Task(String deviceId, String cronExpression, String description, 
                String taskType, String taskName) {
        this.deviceId = deviceId;
        this.cronExpression = cronExpression;
        this.description = description;
        this.taskType = taskType;
        this.taskName = taskName;
        this.enabled = true;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", cronExpression='" + cronExpression + '\'' +
                ", description='" + description + '\'' +
                ", taskType='" + taskType + '\'' +
                ", taskName='" + taskName + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}