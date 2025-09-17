package com.iot.plc.service;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.*;
import com.iot.plc.logger.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.BiConsumer;

/**
 * PLC通信服务类
 * 负责与PLC设备、上位机和EMS系统进行通信，实现整体工作流程
 */
public class PlcService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlcService.class.getName());
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    private static final int RECONNECT_DELAY = 5000; // 重连延迟时间(毫秒)
    
    // 单例模式
    private static volatile PlcService instance;
    
    // PLC设备连接
    private String plcHost;
    private int plcPort;
    private Socket plcSocket;
    private final AtomicBoolean isPlcConnected = new AtomicBoolean(false);
    
    // PLC消息监听器集合
    private final Set<BiConsumer<String, String>> plcMessageListeners = new CopyOnWriteArraySet<>();
    
    // 同步和队列
    private CountDownLatch startCommandLatch = new CountDownLatch(1);
    private final BlockingQueue<ProductCountData> productCountQueue = new LinkedBlockingQueue<>();
    
    // 条码缓存
    private final Map<String, BarcodeData> barcodeCache = new ConcurrentHashMap<>();
    private final AtomicInteger barcodeCount = new AtomicInteger(0);
    
    // 线程池
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledExecutorService;
    
    // 串口服务
    private SerialPortService serialPortService;
    
    // 上位机服务
    private UpperComputerService upperComputerService;
    
    // EMS服务
    private EmsService emsService;
    
    // 配置键常量定义
    private static final String CONFIG_KEY_PLC_IP = "plc.default.ip";
    private static final String CONFIG_KEY_PLC_PORT = "plc.default.port";
    private static final String CONFIG_KEY_CONNECTION_TIMEOUT = "connection.timeout";
    private static final String CONFIG_KEY_READ_TIMEOUT = "read.timeout";
    
    // 配置默认值
    private static final String DEFAULT_PLC_IP = "127.0.0.1";
    private static final int DEFAULT_PLC_PORT = 502;
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;
    private static final int DEFAULT_READ_TIMEOUT = 3000;
    
    // 私有构造函数
    private PlcService() {
        // 从配置管理系统获取PLC配置
        loadConfigFromConfigService();
        
        // 初始化服务
        this.serialPortService = SerialPortService.getInstance();
        this.upperComputerService = UpperComputerService.getInstance();
        this.emsService = EmsService.getInstance();
        
        // 创建线程池
        this.executorService = Executors.newFixedThreadPool(8);
        this.scheduledExecutorService = Executors.newScheduledThreadPool(4);
    }
    
    /**
     * 从配置管理系统加载PLC相关配置
     */
    private void loadConfigFromConfigService() {
        try {
            ConfigService configService = ConfigService.getInstance();
            
            // 获取PLC IP配置
            String plcIp = configService.getConfigValueByKey(CONFIG_KEY_PLC_IP);
            if (plcIp == null || plcIp.trim().isEmpty()) {
                // 如果配置不存在，创建新配置项
                createDefaultConfigItem(CONFIG_KEY_PLC_IP, DEFAULT_PLC_IP, "PLC设备默认IP地址", "STRING");
                plcIp = DEFAULT_PLC_IP;
            }
            this.plcHost = plcIp;
            
            // 获取PLC端口配置
            String plcPortStr = configService.getConfigValueByKey(CONFIG_KEY_PLC_PORT);
            if (plcPortStr == null || plcPortStr.trim().isEmpty()) {
                // 如果配置不存在，创建新配置项
                createDefaultConfigItem(CONFIG_KEY_PLC_PORT, String.valueOf(DEFAULT_PLC_PORT), "PLC设备默认端口号", "INTEGER");
                this.plcPort = DEFAULT_PLC_PORT;
            } else {
                try {
                    this.plcPort = Integer.parseInt(plcPortStr);
                } catch (NumberFormatException e) {
                    LOGGER.log(Level.WARNING, "PLC端口配置格式错误，使用默认值: {}", e.getMessage());
                    this.plcPort = DEFAULT_PLC_PORT;
                }
            }
            
            // 记录配置加载结果
            LOGGER.info(String.format("成功从配置管理系统加载PLC配置: IP=%s, 端口=%d", this.plcHost, this.plcPort));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "从配置管理系统加载PLC配置失败: {}", e.getMessage());
            // 使用默认值
            this.plcHost = DEFAULT_PLC_IP;
            this.plcPort = DEFAULT_PLC_PORT;
        }
    }
    
    /**
     * 创建默认配置项
     */
    private void createDefaultConfigItem(String configKey, String configValue, String description, String dataType) {
        try {
            ConfigService configService = ConfigService.getInstance();
            ConfigItem configItem = new ConfigItem(
                configKey,
                configValue,
                description,
                dataType,
                false // 设置为非必填配置项
            );
            configService.saveConfigItem(configItem);
            LOGGER.info(String.format("已创建默认配置项: %s", configKey));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "创建默认配置项失败: {}", e.getMessage());
        }
    }
    
    // 获取单例实例
    public static synchronized PlcService getInstance() {
        if (instance == null) {
            instance = new PlcService();
        }
        return instance;
    }
        
    /**
     * 构造函数
     * @param plcHost PLC设备主机地址
     * @param plcPort PLC设备端口
     * @param programDeviceHost 烧录上位机主机地址
     * @param programDevicePort 烧录上位机端口
     * @param emsHost EMS系统主机地址
     * @param emsPort EMS系统端口
     */
    public PlcService(String plcHost, int plcPort, String programDeviceHost, int programDevicePort, 
                     String emsHost, int emsPort) {
        this.plcHost = plcHost;
        this.plcPort = plcPort;
        
        // 初始化服务
        this.upperComputerService.init(programDeviceHost, programDevicePort);
        this.emsService.init("http://" + emsHost + ":" + emsPort + "/api");
        
        // 初始化串口服务
        this.serialPortService = SerialPortService.getInstance();
        
        // 创建线程池
        this.executorService = Executors.newFixedThreadPool(8);
        this.scheduledExecutorService = Executors.newScheduledThreadPool(4);
        
        // 初始化服务
        this.serialPortService = SerialPortService.getInstance();
        this.upperComputerService = UpperComputerService.getInstance();
        this.emsService = EmsService.getInstance();
        
        // 初始化上位机服务
        this.upperComputerService.init(programDeviceHost, programDevicePort);
        
        // 初始化EMS服务
        this.emsService.init("http://" + emsHost + ":" + emsPort + "/api/results");
    }
    
    /**
     * 初始化所有连接
     * @return 是否全部连接成功
     */
    public boolean initializeConnections() {
        boolean plcConnected = connectToPLC();
        String connectResult = upperComputerService.connect();
        boolean programDeviceConnected = connectResult != null && connectResult.contains("success");
        
        // 启动监听线程
        if (plcConnected) {
            startPlcListener();
        }
        
        return plcConnected && programDeviceConnected;
    }
    
    /**
     * 连接到PLC设备
     * @return 连接结果
     */
    public boolean connectToPLC() {
        try {
            plcSocket = new Socket(plcHost, plcPort);
            plcSocket.setSoTimeout(5000);
            isPlcConnected.set(true);
            LOGGER.info("成功连接到PLC设备: " + plcHost + ":" + plcPort);
            return true;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "连接PLC设备失败: " + e.getMessage(), e);
            scheduleReconnectPLC();
            return false;
        }
    }
    
    /**
     * 计划重新连接PLC设备
     */
    private void scheduleReconnectPLC() {
        if (!isPlcConnected.get()) {
            scheduledExecutorService.schedule(() -> {
                LOGGER.info("尝试重新连接到PLC设备...");
                connectToPLC();
                if (isPlcConnected.get()) {
                    startPlcListener();
                }
            }, RECONNECT_DELAY, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * 关闭所有连接
     */
    public void disconnect() {
        try {
            if (plcSocket != null && !plcSocket.isClosed()) {
                plcSocket.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "关闭PLC连接时发生错误: " + e.getMessage(), e);
        }
        
        // 关闭上位机连接
        upperComputerService.shutdown();
        
        // 关闭EMS服务
        emsService.shutdown();
        
        // 关闭串口连接
        serialPortService.closeAllPorts();
        
        isPlcConnected.set(false);
        
        // 关闭线程池
        executorService.shutdown();
        scheduledExecutorService.shutdown();
    }
    
    /**
     * 启动PLC监听线程
     */
    private void startPlcListener() {
        executorService.submit(() -> {
            try {
                while (isPlcConnected.get() && plcSocket != null && !plcSocket.isClosed()) {
                    try {
                        // 读取PLC消息
                        byte[] buffer = new byte[4096];
                        int bytesRead = plcSocket.getInputStream().read(buffer);
                        
                        if (bytesRead > 0) {
                            String message = new String(buffer, 0, bytesRead);
                            handlePlcMessage(message);
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "读取PLC消息时发生错误: " + e.getMessage(), e);
                        isPlcConnected.set(false);
                        scheduleReconnectPLC();
                        break;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "PLC监听线程异常: " + e.getMessage(), e);
                isPlcConnected.set(false);
                scheduleReconnectPLC();
            }
        });
    }
    
    /**
     * 处理PLC消息
     * @param message PLC消息
     */
    private void handlePlcMessage(String message) {
        try {
            LOGGER.info("收到PLC消息: " + message);
            
            // 解析消息
            JsonObject jsonObject = JsonParser.parseString(message).getAsJsonObject();
            String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : "unknown";
            
            // 通知所有监听器
            notifyPlcMessageListeners(type, message);
            
            if ("product_count".equals(type) && jsonObject.has("data")) {
                // 处理产品数量数据
                JsonObject dataObject = jsonObject.get("data").getAsJsonObject();
                int count = dataObject.has("count") ? dataObject.get("count").getAsInt() : 0;
                String batchId = dataObject.has("batch_id") ? dataObject.get("batch_id").getAsString() : "";
                
                ProductCountData productCountData = new ProductCountData(count, batchId);
                productCountQueue.offer(productCountData);
                
                LOGGER.info("收到产品数量数据: " + productCountData);
                
                // 验证条码数量与产品数量是否匹配
                validateBarcodeCount(productCountData);
                
                // 保存到数据库
                try {
                    DatabaseManager.savePlcData(batchId, message);
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "保存PLC数据到数据库失败: " + e.getMessage(), e);
                }
            } else if ("start_command".equals(type)) {
                // 处理开始指令
                LOGGER.info("收到开始指令");
                startCommandLatch.countDown();
                
                // 收到开始指令后，发送烧录指令给上位机
                sendProgramCommand();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "处理PLC消息时发生错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 验证条码数量与产品数量是否匹配
     * @param productCountData 产品数量数据
     */
    private void validateBarcodeCount(ProductCountData productCountData) {
        int expectedCount = productCountData.getProductCount();
        int actualCount = barcodeCount.get();
        
        boolean isValid = (expectedCount == actualCount);
        String message = isValid ? "OK" : "条码数量不匹配";
        
        ValidationResult validationResult = new ValidationResult(isValid, message, expectedCount, actualCount);
        
        // 发送验证结果给PLC
        sendValidationResult(validationResult);
    }
    
    /**
     * 发送验证结果到PLC
     * @param result 验证结果
     * @return 发送是否成功
     */
    public boolean sendValidationResult(ValidationResult result) {
        if (!isPlcConnected.get() || plcSocket == null || plcSocket.isClosed()) {
            LOGGER.warning("无法发送验证结果: 未连接到PLC设备");
            return false;
        }
        
        try {
            JsonObject responseObject = new JsonObject();
            responseObject.addProperty("type", "validation_result");
            
            JsonObject dataObject = new JsonObject();
            dataObject.addProperty("is_valid", result.isValid());
            dataObject.addProperty("message", result.getMessage());
            dataObject.addProperty("expected", result.getExpectedCount());
            dataObject.addProperty("actual", result.getActualCount());
            
            responseObject.add("data", dataObject);
            
            String jsonResponse = gson.toJson(responseObject);
            
            // 发送验证结果
            plcSocket.getOutputStream().write(jsonResponse.getBytes());
            plcSocket.getOutputStream().flush();
            
            LOGGER.info("验证结果已发送: " + jsonResponse);
            
            try {
                // 保存到数据库
                DatabaseManager.saveValidationResult(result.isValid(), result.getMessage(), 
                        result.getExpectedCount(), result.getActualCount());
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "保存验证结果到数据库失败: " + e.getMessage(), e);
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "发送验证结果时发生错误: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 发送烧录指令给上位机
     * @return 发送是否成功
     */
    public boolean sendProgramCommand() {
        try {
            // 准备条码列表
            List<String> barcodes = new ArrayList<>();
            for (Map.Entry<String, BarcodeData> entry : barcodeCache.entrySet()) {
                BarcodeData barcodeData = entry.getValue();
                barcodes.add(barcodeData.getBarcode());
            }
            
            // 使用上位机服务发送烧录指令
            String deviceId = "MAIN_DEVICE"; // 可以根据实际情况设置
            upperComputerService.sendProgramCommand(deviceId, barcodes);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "发送烧录指令时发生错误: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 接收产品数量数据
     * @param timeoutMs 超时时间(毫秒)
     * @return 产品数量数据
     * @throws InterruptedException 如果等待被中断
     * @throws TimeoutException 如果等待超时
     */
    public ProductCountData receiveProductCount(long timeoutMs) throws InterruptedException, TimeoutException {
        ProductCountData data = productCountQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (data == null) {
            throw new TimeoutException("等待产品数量数据超时");
        }
        return data;
    }
    
    /**
     * 等待开始指令
     * @param timeoutMs 超时时间(毫秒)
     * @return 是否收到开始指令
     * @throws InterruptedException 如果等待被中断
     */
    public boolean waitForStartCommand(long timeoutMs) throws InterruptedException {
        return startCommandLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 重置开始指令锁
     */
    public void resetStartCommandLatch() {
        startCommandLatch = new CountDownLatch(1);
    }
    
    /**
     * 添加条码数据
     * @param deviceId 设备ID
     * @param barcode 条码内容
     * @param portName 串口名称
     */
    public void addBarcodeData(String deviceId, String barcode, String portName) {
        BarcodeData barcodeData = new BarcodeData(deviceId, barcode, portName);
        barcodeCache.put(deviceId, barcodeData);
        barcodeCount.incrementAndGet();
        
        LOGGER.info("添加条码数据: " + barcodeData);
        
        try {
            // 保存到数据库
            DatabaseManager.saveBarcodeData(deviceId, barcode, portName);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "保存条码数据到数据库失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取条码数量
     * @return 条码数量
     */
    public int getBarcodeCount() {
        return barcodeCount.get();
    }
    
    /**
     * 清空条码缓存
     */
    public void clearBarcodeCache() {
        barcodeCache.clear();
        barcodeCount.set(0);
        LOGGER.info("条码缓存已清空");
    }
    
    /**
     * 测试PLC连接
     * @param deviceId 设备ID
     * @return 连接测试结果
     */
    public String testConnection(String deviceId) {
        if (isPlcConnected.get()) {
            return "PLC设备 " + deviceId + " 连接成功";
        } else {
            return "PLC设备 " + deviceId + " 未连接";
        }
    }
    
    /**
     * 解析PLC数据
     * @param rawData 原始数据
     * @return 解析后的JSON字符串
     */
    /**
     * 发送数据到PLC设备
     * @param deviceId 设备ID
     * @param host PLC主机地址
     * @param port PLC端口
     * @param data 要发送的数据(JSON格式)
     * @return 发送结果
     */
    public String sendToPlc(String deviceId, String host, int port, String data) {
        try {
            if (!isPlcConnected.get()) {
                return "{\"status\":\"error\",\"message\":\"未连接到PLC设备\"}";
            }
            
            // 发送数据
            plcSocket.getOutputStream().write(data.getBytes());
            plcSocket.getOutputStream().flush();
            
            return "{\"status\":\"success\",\"message\":\"数据发送成功\"}";
        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 从PLC读取数据
     * @param deviceId 设备ID
     * @param host PLC主机
     * @param port PLC端口
     * @return 读取到的数据
     */
    public String readPlcData(String deviceId, String host, int port) {
        try {
            if (!isPlcConnected.get()) {
                return "{\"status\":\"error\",\"message\":\"未连接到PLC设备\"}";
            }
            
            // 模拟读取数据
            return "{\"status\":\"success\",\"data\":\"sample data\"}";
        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }

    public String parseData(String rawData) {
        try {
            // 解析PLC数据
            JsonObject parsedData = new JsonObject();
            parsedData.addProperty("rawData", rawData);
            
            // 尝试解析为JSON
            try {
                JsonObject jsonObject = JsonParser.parseString(rawData).getAsJsonObject();
                String type = jsonObject.has("type") ? jsonObject.get("type").getAsString() : "";
                
                if ("product_count".equals(type) && jsonObject.has("data")) {
                    JsonObject dataObject = jsonObject.get("data").getAsJsonObject();
                    int count = dataObject.has("count") ? dataObject.get("count").getAsInt() : 0;
                    String timestamp = dataObject.has("timestamp") ? dataObject.get("timestamp").getAsString() : "";
                    
                    parsedData.addProperty("type", "product_count");
                    parsedData.addProperty("count", count);
                    parsedData.addProperty("timestamp", timestamp);
                    parsedData.addProperty("status", "parsed");
                } else if ("start_command".equals(type)) {
                    parsedData.addProperty("type", "start_command");
                    parsedData.addProperty("status", "parsed");
                } else {
                    parsedData.addProperty("type", "unknown");
                    parsedData.addProperty("status", "parsed");
                }
            } catch (Exception e) {
                // 非JSON格式，尝试其他解析方式
                parsedData.addProperty("parsedValue", parseRawData(rawData));
                parsedData.addProperty("status", "parsed");
            }
            
            parsedData.addProperty("timestamp", System.currentTimeMillis());
            return gson.toJson(parsedData);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "解析数据失败: " + e.getMessage(), e);
            return "解析失败: " + e.getMessage();
        }
    }
    
    /**
     * 解析原始数据
     * @param rawData 原始数据
     * @return 解析后的数值
     */
    private static double parseRawData(String rawData) {
        try {
            return Double.parseDouble(rawData) * 0.1;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * 获取PLC连接状态
     * @return PLC连接状态
     */
    public boolean isPlcConnected() {
        return isPlcConnected.get();
    }
    
    /**
     * 获取烧录上位机连接状态
     * @return 烧录上位机连接状态
     */
    /**
     * 检查上位机连接状态
     * @return 连接状态JSON字符串
     */
    public String getProgramDeviceConnectionStatus() {
        boolean isConnected = upperComputerService != null && upperComputerService.isConnected();
        return isConnected ? 
            "{\"status\":\"connected\"}" : 
            "{\"status\":\"disconnected\"}";
    }
    
    /**
     * 获取EMS服务连接状态
     * @return 连接状态字符串
     */
    public String getEmsConnectionStatus() {
        return emsService != null ? "{\"status\":\"connected\"}" : "{\"status\":\"disconnected\"}";
    }
    
    /**
     * 初始化串口
     * @param deviceId 设备ID
     * @param portName 串口名称
     * @param baudRate 波特率
     * @param dataBits 数据位
     * @param stopBits 停止位
     * @param parity 校验位
     * @return 是否成功
     */
    public boolean initSerialPort(String deviceId, String portName, int baudRate, int dataBits, int stopBits, int parity) {
        return serialPortService.initSerialPort(deviceId, portName, baudRate, dataBits, stopBits, parity);
    }
    
    /**
     * 获取可用串口列表
     * @return 串口名称数组
     */
    public String[] getAvailablePorts() {
        return serialPortService.getAvailablePorts();
    }
    
    /**
     * 关闭串口
     * @param deviceId 设备ID
     */
    public void closeSerialPort(String deviceId) {
        serialPortService.closeSerialPort(deviceId);
    }
    
    /**
     * 添加PLC消息监听器
     * @param listener 消息监听器，接收消息类型和消息内容两个参数
     */
    public void addPlcMessageListener(BiConsumer<String, String> listener) {
        if (listener != null) {
            plcMessageListeners.add(listener);
        }
    }
    
    /**
     * 移除PLC消息监听器
     * @param listener 要移除的消息监听器
     */
    public void removePlcMessageListener(BiConsumer<String, String> listener) {
        if (listener != null) {
            plcMessageListeners.remove(listener);
        }
    }
    
    /**
     * 通知所有PLC消息监听器
     * @param messageType 消息类型
     * @param message 消息内容
     */
    private void notifyPlcMessageListeners(String messageType, String message) {
        for (BiConsumer<String, String> listener : plcMessageListeners) {
            try {
                listener.accept(messageType, message);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error notifying PLC message listener: " + e.getMessage(), e);
            }
        }
    }
}