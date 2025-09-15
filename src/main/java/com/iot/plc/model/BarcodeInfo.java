package com.iot.plc.model;

/**
 * 条码信息类
 * 用于烧录指令中的条码信息
 */
public class BarcodeInfo {
    private String deviceId;  // 设备ID
    private String barcode;   // 条码内容
    
    public BarcodeInfo(String deviceId, String barcode) {
        this.deviceId = deviceId;
        this.barcode = barcode;
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
    
    @Override
    public String toString() {
        return "BarcodeInfo{" +
                "deviceId='" + deviceId + '\'' +
                ", barcode='" + barcode + '\'' +
                '}';
    }
}