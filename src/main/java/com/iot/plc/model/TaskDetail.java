package com.iot.plc.model;

import java.util.Date;

public class TaskDetail {
    private int id;
    private int taskId;
    private String fieldName;
    private String fieldValue;
    private String dataType;
    private boolean required;
    private String description;
    private Date createdAt;
    
    public TaskDetail() {}
    
    public TaskDetail(int taskId, String fieldName, String fieldValue, String dataType, boolean required, String description) {
        this.taskId = taskId;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        this.dataType = dataType;
        this.required = required;
        this.description = description;
        this.createdAt = new Date();
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    
    public String getFieldValue() { return fieldValue; }
    public void setFieldValue(String fieldValue) { this.fieldValue = fieldValue; }
    
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "TaskDetail{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", fieldName='" + fieldName + '\'' +
                ", fieldValue='" + fieldValue + '\'' +
                ", dataType='" + dataType + '\'' +
                ", required=" + required +
                ", description='" + description + '\'' +
                '}';
    }
}