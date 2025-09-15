package com.iot.plc.model;

import java.time.LocalDateTime;

/**
 * 产品数量数据类
 * 用于存储PLC设备传输的产品数量信息
 */
public class ProductCountData {
    private int productCount;     // 产品数量
    private LocalDateTime receiveTime; // 接收时间
    private String batchId;        // 批次ID
    
    public ProductCountData() {
        this.receiveTime = LocalDateTime.now();
    }
    
    public ProductCountData(int productCount, String batchId) {
        this.productCount = productCount;
        this.batchId = batchId;
        this.receiveTime = LocalDateTime.now();
    }
    
    public int getProductCount() {
        return productCount;
    }
    
    public void setProductCount(int productCount) {
        this.productCount = productCount;
    }
    
    public LocalDateTime getReceiveTime() {
        return receiveTime;
    }
    
    public void setReceiveTime(LocalDateTime receiveTime) {
        this.receiveTime = receiveTime;
    }
    
    public String getBatchId() {
        return batchId;
    }
    
    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
    
    @Override
    public String toString() {
        return "ProductCountData{" +
                "productCount=" + productCount +
                ", receiveTime=" + receiveTime +
                ", batchId='" + batchId + '\'' +
                '}';
    }
}