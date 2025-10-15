package com.iot.plc.enumx;

/**
 * 服务类型枚举
 */
public enum TcpServiceEnum {
    BURNER("burner", "烧录机"),  // BURNER
    SCANNER("scanner", "扫码机"),
    PLC("plc", "PLC设备");
    
    private String code;
    private String description;
    
    TcpServiceEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}