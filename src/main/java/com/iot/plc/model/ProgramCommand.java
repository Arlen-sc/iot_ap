package com.iot.plc.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 烧录指令类
 * 用于向烧录上位机发送烧录指令和条码信息
 */
public class ProgramCommand {
    private String command = "program";  // 指令类型
    private List<BarcodeInfo> barcodes;  // 条码信息列表
    private String batchId;              // 批次ID
    private LocalDateTime sendTime;      // 发送时间
    
    public ProgramCommand(List<BarcodeInfo> barcodes, String batchId) {
        this.barcodes = barcodes;
        this.batchId = batchId;
        this.sendTime = LocalDateTime.now();
    }
    
    public String getCommand() {
        return command;
    }
    
    public void setCommand(String command) {
        this.command = command;
    }
    
    public List<BarcodeInfo> getBarcodes() {
        return barcodes;
    }
    
    public void setBarcodes(List<BarcodeInfo> barcodes) {
        this.barcodes = barcodes;
    }
    
    public String getBatchId() {
        return batchId;
    }
    
    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }
    
    public LocalDateTime getSendTime() {
        return sendTime;
    }
    
    public void setSendTime(LocalDateTime sendTime) {
        this.sendTime = sendTime;
    }
}