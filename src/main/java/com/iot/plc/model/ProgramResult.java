package com.iot.plc.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 烧录结果类
 * 用于存储上位机返回的烧录结果
 */
public class ProgramResult {
    private String status;         // 状态: success/failure
    private List<DeviceResult> results; // 设备结果列表
    private String batchId;        // 批次ID
    private LocalDateTime completeTime; // 完成时间
    
    public ProgramResult() {
        this.completeTime = LocalDateTime.now();
        this.results = new ArrayList<>();
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<DeviceResult> getResults() {
        return results;
    }
    
    public void setResults(List<DeviceResult> results) {
        this.results = results;
    }
    
    public String getBatchId() {
        return batchId;
    }
    
    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
    
    public LocalDateTime getCompleteTime() {
        return completeTime;
    }
    
    public void setCompleteTime(LocalDateTime completeTime) {
        this.completeTime = completeTime;
    }
    
    // 添加timestamp字段的getter和setter方法
    private String timestamp; // 字符串格式的时间戳
    
    public String getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    
    /**
     * 添加设备结果
     * @param deviceResult 设备结果
     */
    public void addDeviceResult(DeviceResult deviceResult) {
        if (this.results == null) {
            this.results = new ArrayList<>();
        }
        this.results.add(deviceResult);
    }
    
    /**
     * 计算成功率
     * @return 成功率百分比
     */
    public double getSuccessRate() {
        if (results == null || results.isEmpty()) {
            return 0.0;
        }
        
        long successCount = results.stream()
                .filter(DeviceResult::isSuccess)
                .count();
        
        return (double) successCount / results.size() * 100.0;
    }
    
    /**
     * 获取成功数量
     * @return 成功数量
     */
    public int getSuccessCount() {
        if (results == null) {
            return 0;
        }
        
        return (int) results.stream()
                .filter(DeviceResult::isSuccess)
                .count();
    }
    
    /**
     * 获取失败数量
     * @return 失败数量
     */
    public int getFailureCount() {
        if (results == null) {
            return 0;
        }
        
        return (int) results.stream()
                .filter(result -> !result.isSuccess())
                .count();
    }
    
    @Override
    public String toString() {
        return "ProgramResult{" +
                "status='" + status + '\'' +
                ", results=" + results +
                ", batchId='" + batchId + '\'' +
                ", completeTime=" + completeTime +
                '}';
    }
}