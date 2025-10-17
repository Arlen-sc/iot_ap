package com.iot.plc.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 烧录结果类
 * 用于存储烧录结果信息
 */
public class ProgramResult {
    private String code;         // 条码
    private String result;       // 烧录状态
    private Timestamp time;  // 时间
    private String rem;          // 备注
    private String deviceId;     // 设备ID
    
    // 日期时间格式化器
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public ProgramResult() {
        this.time = new Timestamp(System.currentTimeMillis());
    }
    
    public ProgramResult(String code, String result, String deviceId) {
        this.code = code;
        this.result = result;
        this.deviceId = deviceId;
        this.time = new Timestamp(System.currentTimeMillis());
        this.rem = "";
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getResult() {
        return result;
    }
    
    public void setResult(String result) {
        this.result = result;
    }
    
    public Timestamp getTime() {
        return time;
    }
    
    public void setTime(Timestamp time) {
        this.time = time;
    }
    
    /**
     * 获取格式化后的时间字符串
     * @return 格式化的时间字符串
     */
    public String getFormattedTime() {
        return time != null ? FORMATTER.format(time.toLocalDateTime()) : "";
    }
    
    public String getRem() {
        return rem;
    }
    
    public void setRem(String rem) {
        this.rem = rem;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    @Override
    public String toString() {
        return "ProgramResult{" +
                "code='" + code + '\'' +
                ", result='" + result + '\'' +
                ", time=" + getFormattedTime() +
                ", rem='" + rem + '\'' +
                ", deviceId='" + deviceId + '\'' +
                '}';
    }
}