package com.iot.plc.service;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.BarcodeData;
import com.iot.plc.logger.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 * 串口服务类
 * 用于处理扫描枪的条码数据
 */
public class SerialPortService {
    // 数据监听器接口
    public interface DataListener {
        void onDataReceived(String port, String data);
    }
    
    // 数据监听器
    private DataListener dataListener;
    
    // 设置数据监听器
    public void setDataListener(DataListener listener) {
        this.dataListener = listener;
    }
    private static final Logger logger = LoggerFactory.getLogger(SerialPortService.class.getName());
    
    // 设备ID与串口映射
    private Map<String, SerialPort> devicePortMap = new HashMap<>();
    
    // 设备ID与条码数据缓存映射
    private Map<String, List<BarcodeData>> deviceBarcodeMap = new ConcurrentHashMap<>();
    
    // 单例模式
    private static SerialPortService instance;
    
    private SerialPortService() {
        // 私有构造函数
    }
    
    public static synchronized SerialPortService getInstance() {
        if (instance == null) {
            instance = new SerialPortService();
        }
        return instance;
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
        try {
            // 如果已经存在该设备的串口连接，先关闭
            if (devicePortMap.containsKey(deviceId)) {
                SerialPort existingPort = devicePortMap.get(deviceId);
                if (existingPort.isOpened()) {
                    existingPort.closePort();
                }
                devicePortMap.remove(deviceId);
            }
            
            // 创建新的串口连接
            SerialPort serialPort = new SerialPort(portName);
            serialPort.openPort();
            serialPort.setParams(baudRate, dataBits, stopBits, parity);
            
            // 添加事件监听器
            serialPort.addEventListener(new SerialPortEventListener() {
                private StringBuilder buffer = new StringBuilder();
                
                @Override
                public void serialEvent(SerialPortEvent event) {
                    if (event.isRXCHAR() && event.getEventValue() > 0) {
                        try {
                            byte[] data = serialPort.readBytes();
                            String receivedData = new String(data);
                            
                            // 将接收到的数据添加到缓冲区
                            buffer.append(receivedData);
                            
                            // 检查是否接收到完整的条码（通常以回车或换行结束）
                            if (receivedData.contains("\n") || receivedData.contains("\r")) {
                                String barcode = buffer.toString().trim();
                                buffer = new StringBuilder(); // 清空缓冲区
                                
                                // 处理条码数据
                                processBarcode(deviceId, barcode, portName);
                            }
                        } catch (SerialPortException ex) {
                            logger.severe("Error reading from serial port: " + ex.getMessage());
                        }
                    }
                }
            });
            
            // 保存串口连接
            devicePortMap.put(deviceId, serialPort);
            
            // 初始化条码数据缓存
            if (!deviceBarcodeMap.containsKey(deviceId)) {
                deviceBarcodeMap.put(deviceId, new ArrayList<>());
            }
            
            logger.info("Serial port initialized for device " + deviceId + " on port " + portName);
            return true;
        } catch (SerialPortException e) {
            logger.severe("Failed to initialize serial port for device " + deviceId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 处理条码数据
     * @param deviceId 设备ID
     * @param barcode 条码
     * @param portName 串口名称
     */
    private void processBarcode(String deviceId, String barcode, String portName) {
        logger.info("Received barcode for device " + deviceId + ": " + barcode);
        
        // 创建条码数据对象
        BarcodeData barcodeData = new BarcodeData(deviceId, barcode, portName);
        
        // 添加到缓存
        List<BarcodeData> barcodes = deviceBarcodeMap.get(deviceId);
        if (barcodes == null) {
            barcodes = new ArrayList<>();
            deviceBarcodeMap.put(deviceId, barcodes);
        }
        barcodes.add(barcodeData);
        
        // 通知数据监听器
        if (dataListener != null) {
            dataListener.onDataReceived(portName, barcode);
        }
        
        // 保存到数据库
        try {
            DatabaseManager.saveBarcodeData(deviceId, barcode, portName);
            logger.info("Barcode data saved to database");
        } catch (Exception e) {
            logger.severe("Failed to save barcode data to database: " + e.getMessage());
        }
    }
    
    /**
     * 获取设备的条码数据缓存
     * @param deviceId 设备ID
     * @return 条码数据列表
     */
    public List<BarcodeData> getDeviceBarcodes(String deviceId) {
        return deviceBarcodeMap.getOrDefault(deviceId, new ArrayList<>());
    }
    
    /**
     * 清空设备的条码数据缓存
     * @param deviceId 设备ID
     */
    public void clearDeviceBarcodes(String deviceId) {
        deviceBarcodeMap.put(deviceId, new ArrayList<>());
        logger.info("Cleared barcode cache for device " + deviceId);
    }
    
    /**
     * 获取当前条码数量
     * @param deviceId 设备ID
     * @return 条码数量
     */
    public int getBarcodeCount(String deviceId) {
        List<BarcodeData> barcodes = deviceBarcodeMap.get(deviceId);
        return barcodes == null ? 0 : barcodes.size();
    }
    
    /**
     * 关闭串口
     * @param deviceId 设备ID
     */
    public void closeSerialPort(String deviceId) {
        SerialPort serialPort = devicePortMap.get(deviceId);
        if (serialPort != null && serialPort.isOpened()) {
            try {
                serialPort.closePort();
                devicePortMap.remove(deviceId);
                logger.info("Serial port closed for device " + deviceId);
            } catch (SerialPortException e) {
                logger.severe("Failed to close serial port for device " + deviceId + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * 关闭所有串口
     */
    public void closeAllPorts() {
        for (Map.Entry<String, SerialPort> entry : devicePortMap.entrySet()) {
            String deviceId = entry.getKey();
            SerialPort serialPort = entry.getValue();
            
            if (serialPort != null && serialPort.isOpened()) {
                try {
                    serialPort.closePort();
                    logger.info("Serial port closed for device " + deviceId);
                } catch (SerialPortException e) {
                    logger.severe("Failed to close serial port for device " + deviceId + ": " + e.getMessage());
                }
            }
        }
        devicePortMap.clear();
    }
    
    /**
     * 获取可用串口列表
     * @return 串口名称数组
     */
    public String[] getAvailablePorts() {
        return SerialPortList.getPortNames();
    }
}