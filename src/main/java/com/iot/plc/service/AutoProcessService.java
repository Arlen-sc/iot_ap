package com.iot.plc.service;

import com.iot.plc.logger.LoggerFactory;
import com.iot.plc.model.BarcodeData;
import com.iot.plc.model.DeviceResult;
import com.iot.plc.model.ProgramResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * 自动处理流程服务
 * 实现自动逻辑流程：
 * 1. 监听串口读取扫描枪条码数据，自动缓存数据，绑定设备号+扫描内容（条码）
 * 2. 对比PLC传输的产品个数与缓存的条码个数，若相等则OK，反之给PLC发送异常指令
 * 3. 接收PLC开始指令后，自动给上位机传送烧录指令和多条条码信息
 * 4. 接收上位机返回的条码信息+烧录结果
 * 5. 保存结果并回传给EMS
 */
public class AutoProcessService {
    private static final Logger logger = LoggerFactory.getLogger(AutoProcessService.class.getName());
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static AutoProcessService instance;
    
    // 服务实例
    private final SerialPortService serialPortService;
    private final PlcService plcService;
    private final UpperComputerService upperComputerService;
    private final EmsService emsService;
    
    // 状态管理
    private String currentStatus = "空闲";
    private String serialPortStatus = "未连接";
    private String plcStatus = "未连接";
    private String upperComputerStatus = "未连接";
    private String emsStatus = "未连接";
    private int expectedBarcodeCount = 0;
    private int actualBarcodeCount = 0;
    
    // 数据管理
    private final List<String> currentBarcodes = new ArrayList<>();
    private final String deviceId = "PLC_DEVICE_001";
    
    // 流程控制标志
    private AtomicBoolean processStarted = new AtomicBoolean(false);
    private AtomicBoolean barcodeVerified = new AtomicBoolean(false);
    private AtomicBoolean waitingForStartCommand = new AtomicBoolean(false);
    private AtomicBoolean programCommandSent = new AtomicBoolean(false);
    private AtomicBoolean waitingForProgramResult = new AtomicBoolean(false);
    
    // 单例模式
    private AutoProcessService() {
        this.serialPortService = SerialPortService.getInstance();
        this.plcService = PlcService.getInstance();
        this.upperComputerService = UpperComputerService.getInstance();
        this.emsService = EmsService.getInstance();
        
        initProcess();
    }
    
    public static synchronized AutoProcessService getInstance() {
        if (instance == null) {
            instance = new AutoProcessService();
        }
        return instance;
    }
    
    private void initProcess() {
        // 初始化PLC消息监听器
        plcService.addPlcMessageListener((messageType, message) -> {
            if ("product_count".equals(messageType)) {
                handleProductCountMessage(message);
            } else if ("start_command".equals(messageType)) {
                handleStartCommandMessage();
            }
        });
        
        // 初始化流程定时器
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(500);
                    if (processStarted.get()) {
                        checkProcessStatus();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
        
        // 初始化上位机结果监听器
        initUpperComputerResultListener();
    }
    
    private void handleProductCountMessage(String message) {
        try {
            // 解析产品数量消息
            JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
            if (jsonObject.has("type") && "product_count".equals(jsonObject.get("type").getAsString()) && jsonObject.has("data")) {
                JsonObject dataObject = jsonObject.get("data").getAsJsonObject();
                int count = dataObject.has("count") ? dataObject.get("count").getAsInt() : 0;
                expectedBarcodeCount = count;
                log("接收到PLC产品数量: " + count);
                
                // 验证条码数量
                if (processStarted.get() && !barcodeVerified.get()) {
                    int actualCount = serialPortService.getBarcodeCount(deviceId);
                    actualBarcodeCount = actualCount;
                    if (actualCount == count) {
                        barcodeVerified.set(true);
                        currentStatus = "验证通过";
                        log("条码数量验证通过: " + actualCount + " = " + count);
                        // 发送确认指令给PLC
                        plcService.sendToPlc(deviceId, "127.0.0.1", 502, "{\"type\":\"barcode_verified\",\"status\":\"ok\"}");
                        waitingForStartCommand.set(true);
                    } else {
                        log("错误: 条码数量不匹配! 实际: " + actualCount + " 预期: " + count);
                        // 发送异常指令给PLC
                        plcService.sendToPlc(deviceId, "127.0.0.1", 502, "{\"type\":\"barcode_verified\",\"status\":\"error\",\"message\":\"Barcode count mismatch\"}");
                        currentStatus = "异常";
                        resetProcess();
                    }
                }
            } else {
                log("警告: 接收到无效的产品数量消息: " + message);
            }
        } catch (Exception e) {
            log("错误: 无法解析产品数量消息: " + message + ", 错误: " + e.getMessage());
        }
    }
    
    private void handleStartCommandMessage() {
        if (processStarted.get() && barcodeVerified.get() && waitingForStartCommand.get() && !programCommandSent.get()) {
            log("接收到PLC开始指令，准备发送烧录指令给上位机...");
            currentStatus = "发送烧录指令";
            
            // 收集条码数据
            List<BarcodeData> barcodes = serialPortService.getDeviceBarcodes(deviceId);
            currentBarcodes.clear();
            for (BarcodeData barcode : barcodes) {
                currentBarcodes.add(barcode.getBarcode());
            }
            
            // 发送烧录指令
            String result = upperComputerService.sendProgramCommand(deviceId, currentBarcodes);
            log("发送烧录指令结果: " + result);
            
            programCommandSent.set(true);
            waitingForProgramResult.set(true);
            currentStatus = "等待烧录结果";
        }
    }
    
    private void checkProcessStatus() {
        // 这里可以检查流程状态并处理超时等异常情况
        if (processStarted.get()) {
            // 检查是否超过预设的处理时间
            // 如果需要实现超时逻辑，可以在这里添加
        }
    }
    
    /**
     * 初始化上位机结果监听器
     */
    private void initUpperComputerResultListener() {
        // 添加烧录结果监听器
        upperComputerService.addProgramResultListener(result -> {
            if (processStarted.get() && programCommandSent.get() && waitingForProgramResult.get()) {
                log("收到上位机烧录结果，批次ID: " + result.getBatchId());
                currentStatus = "处理结果";
                
                // 显示烧录结果
                StringBuilder resultSummary = new StringBuilder();
                resultSummary.append("烧录结果:\n");
                
                boolean allSuccess = true;
                for (DeviceResult deviceResult : result.getResults()) {
                    String status = deviceResult.isSuccess() ? "成功" : "失败";
                    resultSummary.append("  条码: ")
                              .append(deviceResult.getBarcode())
                              .append(", 状态: ")
                              .append(status);
                    
                    if (!deviceResult.isSuccess() && deviceResult.getErrorMessage() != null) {
                        resultSummary.append(", 错误: ")
                                  .append(deviceResult.getErrorMessage());
                        allSuccess = false;
                    }
                    resultSummary.append("\n");
                }
                
                log(resultSummary.toString());
                
                // EMS已在上位机服务中发送，这里只记录状态
                log("结果已回传给EMS");
                
                // 流程完成
                currentStatus = allSuccess ? "完成" : "部分失败";
                waitingForProgramResult.set(false);
                
                if (allSuccess) {
                    log("流程执行完成！");
                } else {
                    log("警告: 流程执行完成，但部分条码烧录失败！");
                }
                
                // 注意：根据用户需求，流程完成后需要保持当前状态，不需要自动重置
                // 用户要求："且这5个步骤需要按顺序执行，不然就需要清空，重新从1走。"
            }
        });
    }
    
    private void updateStatus() {
        // 更新连接状态
        boolean serialConnected = serialPortService.getAvailablePorts().length > 0;
        serialPortStatus = serialConnected ? "已连接" : "未连接";
        
        boolean plcConnected = plcService.isPlcConnected();
        plcStatus = plcConnected ? "已连接" : "未连接";
        
        boolean upperComputerConnected = upperComputerService.isConnected();
        upperComputerStatus = upperComputerConnected ? "已连接" : "未连接";
        
        // EMS状态检查
        try {
            String emsStatusJson = plcService.getEmsConnectionStatus();
            boolean emsConnected = emsStatusJson.contains("connected");
            emsStatus = emsConnected ? "已连接" : "未连接";
        } catch (Exception e) {
            emsStatus = "未连接";
        }
    }
    
    public void startProcess() {
        if (processStarted.get()) {
            log("流程已经启动，请先重置流程");
            return;
        }
        
        // 检查连接状态
        if (serialPortService.getAvailablePorts().length == 0) {
            log("错误: 没有可用的串口");
            return;
        }
        
        if (!plcService.isPlcConnected()) {
            log("错误: PLC未连接");
            return;
        }
        
        if (!upperComputerService.isConnected()) {
            log("错误: 上位机未连接");
            return;
        }
        
        // 检查EMS服务是否可用
        try {
            String emsStatusJson = plcService.getEmsConnectionStatus();
            if (!emsStatusJson.contains("connected")) {
                log("错误: EMS服务未连接");
                return;
            }
        } catch (Exception e) {
            log("错误: 无法检查EMS连接状态: " + e.getMessage());
            return;
        }
        
        // 清空之前的条码数据
        clearBarcodes();
        
        // 启动流程
        processStarted.set(true);
        barcodeVerified.set(false);
        waitingForStartCommand.set(false);
        programCommandSent.set(false);
        waitingForProgramResult.set(false);
        currentStatus = "运行中";
        log("流程已启动，请扫描条码...");
    }
    
    public void resetProcess() {
        processStarted.set(false);
        barcodeVerified.set(false);
        waitingForStartCommand.set(false);
        programCommandSent.set(false);
        waitingForProgramResult.set(false);
        currentStatus = "空闲";
        expectedBarcodeCount = 0;
        currentBarcodes.clear();
        log("流程已重置");
    }
    
    public void clearBarcodes() {
        serialPortService.clearDeviceBarcodes(deviceId);
        log("条码缓存已清空");
    }
    
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = "[" + timestamp + "] " + message;
        
        logger.info(logMessage);
    }
    
    // Getters for monitoring purposes
    public String getCurrentStatus() {
        return currentStatus;
    }
    
    public String getSerialPortStatus() {
        return serialPortStatus;
    }
    
    public String getPlcStatus() {
        return plcStatus;
    }
    
    public String getUpperComputerStatus() {
        return upperComputerStatus;
    }
    
    public String getEmsStatus() {
        return emsStatus;
    }
    
    public int getExpectedBarcodeCount() {
        return expectedBarcodeCount;
    }
    
    public int getActualBarcodeCount() {
        return actualBarcodeCount;
    }
    
    public boolean isProcessStarted() {
        return processStarted.get();
    }
}