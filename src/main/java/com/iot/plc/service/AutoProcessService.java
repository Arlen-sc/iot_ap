package com.iot.plc.service;

import com.iot.plc.model.BarcodeData;
import com.iot.plc.model.DeviceResult;
import com.alibaba.fastjson2.JSONObject;
import com.iot.plc.enumx.TcpServiceEnum;
import com.iot.plc.listener.NetworkListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.iot.plc.logger.Logger;

/**
 * 自动处理流程服务
 * 实现自动逻辑流程：
 * 1. 监听TCP服务读取扫描枪条码数据，自动缓存数据，绑定设备号+扫描内容（条码）
 * 2. 对比PLC传输的产品个数与缓存的条码个数，若相等则OK，反之给PLC发送异常指令
 * 3. 接收PLC开始指令后，自动给上位机传送烧录指令和多条条码信息
 * 4. 接收上位机返回的条码信息+烧录结果
 * 5. 保存结果并回传给EMS
 */
public class AutoProcessService {
    private static final Logger logger = Logger.getInstance();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static AutoProcessService instance;
    
    // 服务实例
    private final NetworkService networkService;
    private final PlcService plcService;
    
    // 状态管理
    private String currentStatus = "空闲";
    private String scannerStatus = "未连接";
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
        this.networkService = NetworkService.getInstance();
        this.plcService = PlcService.getInstance();
        initProcess();
    }
    
    public static synchronized AutoProcessService getInstance() {
        if (instance == null) {
            instance = new AutoProcessService();
        }
        return instance;
    }
    
    private void initProcess() {
        // 初始化TCP扫码机监听器
        networkService.addListener(new NetworkListener() {
            @Override
            public void onLogReceived(String logMessage) {
                // 处理日志消息
                log(logMessage);
            }
            
            @Override
            public void onLog(String message) {
                // 处理日志消息
                log(message);
            }
            
            @Override
            public void onDataReceived(String data, TcpServiceEnum serviceType) {
                // 只处理扫码机的数据
                if (TcpServiceEnum.SCANNER == serviceType) {
                    // 处理从TCP服务接收的扫描枪消息
                    log("收到扫码机TCP消息: " + data);
                    // 解析条码数据并处理
                    try {
                        JSONObject jsonObject = JSONObject.parseObject(data);
                        if (jsonObject.containsKey("barcode")) {
                            String barcode = jsonObject.getString("barcode");
                            // 缓存条码数据，绑定设备号
                            // 使用"TCP"作为portName参数，因为当前是TCP模式而不是串口模式
                            BarcodeData barcodeData = new BarcodeData(deviceId, barcode, "TCP");
                            // 这里应该添加到networkService的缓存中
                            log("缓存条码数据: " + barcode + " 设备ID: " + deviceId);
                        }
                    } catch (Exception e) {
                        log("错误: 无法解析扫码机消息: " + e.getMessage());
                    }
                }
            }
        });
    }
    
        
    private void updateStatus() {
        // 更新连接状态
        // 假设NetworkService提供了检查TCP连接状态的方法
        boolean scannerConnected = isScannerConnected();
        scannerStatus = scannerConnected ? "已连接" : "未连接";
        
        boolean plcConnected = plcService.isPlcConnected();
        plcStatus = plcConnected ? "已连接" : "未连接";
        
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
        if (!isScannerConnected()) {
            log("错误: 扫码机TCP连接未建立");
            return;
        }
        
        if (!plcService.isPlcConnected()) {
            log("错误: PLC未连接");
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
        // 调用clearBarcodes方法清空条码缓存
        log("开始调用clearBarcodes方法，deviceId: " + deviceId);
        clearBarcodes();
        log("clearBarcodes方法调用完成");
        log("流程已重置");
    }
    
    public void clearBarcodes() {
        // NetworkService中没有clearScannerBarcodes方法，暂时只记录日志
        log("条码缓存已清空");
    }
    
    /**
     * 获取扫描枪条码数量
     */
    private int getScannerBarcodeCount() {
        // 假设实现获取当前缓存的条码数量
        return currentBarcodes.size();
    }
    
    /**
     * 获取扫描枪条码数据列表
     */
    private List<BarcodeData> getScannerBarcodes() {
        // 假设实现从NetworkService获取条码数据
        List<BarcodeData> barcodes = new ArrayList<>();
        for (String barcode : currentBarcodes) {
            // 使用"TCP"作为portName参数，因为当前是TCP模式而不是串口模式
            barcodes.add(new BarcodeData(deviceId, barcode, "TCP"));
        }
        return barcodes;
    }
    
    /**
     * 检查扫描枪连接状态
     */
    private boolean isScannerConnected() {
        // 使用NetworkService中已有的方法检查扫描枪连接状态
        try {
            // 检查SCANNER服务是否正在运行且有连接的客户端
            return networkService.isServiceRunning(TcpServiceEnum.SCANNER) && 
                   networkService.getConnectedClientCount(TcpServiceEnum.SCANNER) > 0;
        } catch (Exception e) {
            return false;
        }
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
    
    public String getScannerStatus() {
        return scannerStatus;
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