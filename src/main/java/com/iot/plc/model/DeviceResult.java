package com.iot.plc.model;

/**
 * 设备结果类
 * 用于存储单个设备的烧录结果
 */
public class DeviceResult {
    private String deviceId;      // 设备ID
    private String barcode;       // 条码内容
    private boolean success;      // 烧录结果
    private String errorMessage;  // 错误信息
    
    public DeviceResult(String deviceId, String barcode, boolean success, String errorMessage) {
        this.deviceId = deviceId;
        this.barcode = barcode;
        this.success = success;
        this.errorMessage = errorMessage;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getBarcode() {
        return barcode;
    }
    
    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String toString() {
        return "DeviceResult{" +
                "deviceId='" + deviceId + '\'' +
                ", barcode='" + barcode + '\'' +
                ", success=" + success +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}