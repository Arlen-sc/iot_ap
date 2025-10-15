package com.iot.plc.model;

import java.util.Date;

public class ConfigItem {
    private int id;
    private String configKey;
    private String configValue;
    private String description;
    private String dataType;
    private boolean required;
    private Date createdAt;

    public ConfigItem() {}

    public ConfigItem(String configKey, String configValue, String description, String dataType, boolean required) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.description = description;
        this.dataType = dataType;
        this.required = required;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigDescription() {
        return description;
    }

    public void setConfigDescription(String description) {
        this.description = description;
    }
    
    // 添加getDescription()方法作为别名，保持兼容性
    public String getDescription() {
        return getConfigDescription();
    }
    
    // 添加setDescription()方法作为别名，保持兼容性
    public void setDescription(String description) {
        setConfigDescription(description);
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "ConfigItem{" +
                "id=" + id +
                ", configKey='" + configKey + '\'' +
                ", configValue='" + configValue + '\'' +
                ", description='" + description + '\'' +
                ", dataType='" + dataType + '\'' +
                ", required=" + required +
                ", createdAt=" + createdAt +
                '}';
    }

    public void setConfigType(String string) {
        
    }

}