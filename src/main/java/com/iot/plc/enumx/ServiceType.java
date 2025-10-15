package com.iot.plc.enumx;

/**
 * 服务类型枚举
 */
public enum ServiceType {
    BURNER("烧录机"),
    SCANNER("扫码机");
    
    private String description;
    
    ServiceType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}