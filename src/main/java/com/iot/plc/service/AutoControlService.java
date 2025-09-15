package com.iot.plc.service;

import com.iot.plc.model.BarcodeData;
import com.iot.plc.model.ProgramResult;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutoControlService {
    private static final Logger logger = LoggerFactory.getLogger(AutoControlService.class);
    
    private enum State { IDLE, SCANNING, VALIDATING, WAITING_COMMAND, PROGRAMMING, REPORTING, ERROR }
    
    private State currentState = State.IDLE;
    private final Map<String, List<BarcodeData>> barcodeMap = new ConcurrentHashMap<>();
    private String currentDeviceId;
    private int expectedProductCount;
    
    // Services
    private final PlcService plcService;
    private final UpperComputerService upperService;
    private final EmsService emsService;
    
    private static volatile AutoControlService instance;
    
    private AutoControlService() {
        // 初始化服务，但捕获可能的类加载异常
        this.plcService = initPlcService();
        this.upperService = initUpperComputerService();
        this.emsService = initEmsService();
        
        initServices();
    }
    
    public static AutoControlService getInstance() {
        if (instance == null) {
            synchronized (AutoControlService.class) {
                if (instance == null) {
                    instance = new AutoControlService();
                }
            }
        }
        return instance;
    }
    
    // 初始化PlcService，处理可能的类加载异常
    private PlcService initPlcService() {
        try {
            return PlcService.getInstance();
        } catch (Throwable e) {
            logger.warn("无法初始化PlcService，将使用模拟服务: {}", e.getMessage());
            return null;
        }
    }
    
    // 初始化UpperComputerService，处理可能的类加载异常
    private UpperComputerService initUpperComputerService() {
        try {
            return UpperComputerService.getInstance();
        } catch (Throwable e) {
            logger.warn("无法初始化UpperComputerService，将使用模拟服务: {}", e.getMessage());
            return null;
        }
    }
    
    // 初始化EmsService，处理可能的类加载异常
    private EmsService initEmsService() {
        try {
            return EmsService.getInstance();
        } catch (Throwable e) {
            logger.warn("无法初始化EmsService，将使用模拟服务: {}", e.getMessage());
            return null;
        }
    }
    
    private void initServices() {
        // PLC Message Handler - 检查服务是否为null
        if (plcService != null) {
            plcService.addPlcMessageListener((messageType, message) -> {
                switch (currentState) {
                    case SCANNING:
                        validateProductCount(message);
                        break;
                    case WAITING_COMMAND:
                        checkStartCommand(message);
                        break;
                }
            });
        }
        
        // 由于缺少依赖，我们不初始化串口服务和编程结果监听器
        logger.info("自动控制服务已初始化，由于缺少依赖，部分功能可能无法正常工作");
    }
    
    public void processBarcode(String port, String barcode) {
        if (currentDeviceId == null) {
            currentDeviceId = "DEV_" + System.currentTimeMillis() % 1000;
            logger.info("自动分配设备号: {}", currentDeviceId);
        }
        
        BarcodeData data = new BarcodeData(currentDeviceId, barcode, port);
        barcodeMap.computeIfAbsent(currentDeviceId, k -> new ArrayList<>()).add(data);
        
        updateState(State.SCANNING);
        logger.info("收到条码: {}", barcode);
    }
    
    private void validateProductCount(String plcMessage) {
        if (plcService == null) {
            logger.warn("PlcService未初始化，无法验证产品数量");
            return;
        }
        
        try {
            JsonObject json = JsonParser.parseString(plcMessage).getAsJsonObject();
            if ("product_count".equals(json.get("type").getAsString())) {
                expectedProductCount = json.getAsJsonObject("data").get("count").getAsInt();
                int actualCount = barcodeMap.getOrDefault(currentDeviceId, new ArrayList<>()).size();
                
                if (expectedProductCount == actualCount) {
                    updateState(State.WAITING_COMMAND);
                    logger.info("验证通过，等待开始指令");
                } else {
                    resetProcess();
                    logger.info("条码数量不匹配，预期:{}, 实际:{}", expectedProductCount, actualCount);
                }
            }
        } catch (Exception e) {
            logger.error("PLC消息解析失败: {}", e.getMessage());
        }
    }
    
    private void checkStartCommand(String message) {
        if (upperService == null) {
            logger.warn("UpperComputerService未初始化，无法发送烧录命令");
            return;
        }
        
        if ("start_command".equals(message)) {
            List<String> barcodes = new ArrayList<>();
            for (BarcodeData data : barcodeMap.getOrDefault(currentDeviceId, new ArrayList<>())) {
                barcodes.add(data.getBarcode());
            }
            
            try {
                upperService.sendProgramCommand(currentDeviceId, barcodes);
                updateState(State.PROGRAMMING);
                logger.info("开始烧录，条码数量: {}", barcodes.size());
            } catch (Exception e) {
                logger.error("发送烧录命令失败: {}", e.getMessage());
            }
        }
    }
    
    public void reportResult(ProgramResult result) {
        if (emsService == null) {
            logger.warn("EmsService未初始化，无法上报结果");
            // 即使没有EMS服务，我们也重置流程
            resetProcess();
            return;
        }
        
        try {
            emsService.sendProgramResult(result);
            updateState(State.REPORTING);
            logger.info("结果已上报EMS: {}", result.getStatus());
        } catch (Exception e) {
            logger.error("上报结果失败: {}", e.getMessage());
        }
        
        // 完成后重置状态
        resetProcess();
    }
    
    public void resetProcess() {
        barcodeMap.clear();
        currentDeviceId = null;
        expectedProductCount = 0;
        updateState(State.IDLE);
        logger.info("流程已重置");
    }
    
    private void updateState(State newState) {
        currentState = newState;
        logger.info("当前状态: {}", newState.name());
    }
    
    // 获取当前状态
    public String getCurrentState() {
        return currentState.name();
    }
    
    // 获取当前设备ID
    public String getCurrentDeviceId() {
        return currentDeviceId;
    }
    
    // 获取条码数据
    public Map<String, List<BarcodeData>> getBarcodeMap() {
        return new ConcurrentHashMap<>(barcodeMap);
    }
}