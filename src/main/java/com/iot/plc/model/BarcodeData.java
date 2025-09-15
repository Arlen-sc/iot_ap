package com.iot.plc.model;

import java.time.LocalDateTime;

/**
 * 条码数据类
 * 用于存储扫描枪扫描的条码信息
 */
public class BarcodeData {
    private String deviceId;    // 设备编号
    private String barcode;     // 条码内容
    private LocalDateTime scanTime; // 扫描时间
    private String portName;    // 串口名称
    
    public BarcodeData(String deviceId, String barcode, String portName) {
        this.deviceId = deviceId;
        this.barcode = barcode;
        this.scanTime = LocalDateTime.now();
        this.portName = portName;
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
    
    public LocalDateTime getScanTime() {
        return scanTime;
    }
    
    public void setScanTime(LocalDateTime scanTime) {
        this.scanTime = scanTime;
    }
    
    public String getPortName() {
        return portName;
    }
    
    public void setPortName(String portName) {
        this.portName = portName;
    }
    
    @Override
    public String toString() {
        return "BarcodeData{" +
                "deviceId='" + deviceId + '\'' +
                ", barcode='" + barcode + '\'' +
                ", scanTime=" + scanTime +
                ", portName='" + portName + '\'' +
                '}';
    }
}