package com.iot.plc.service;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.iot.plc.config.NetworkConfig;
import com.iot.plc.enumx.ProtocolType;
import com.iot.plc.enumx.ServiceType;
import com.iot.plc.listener.NetworkListener;
import com.iot.plc.logger.Logger;
import com.iot.plc.model.DataMode;

/**
 * 网络服务类
 * 支持TCP服务端、TCP客户端和UDP协议
 * 支持ASCII和HEX格式的数据接收
 * 支持烧录机和扫码机两种服务类型
 */
public class NetworkService {
    private static final Logger logger = Logger.getInstance();
    private static final NetworkService instance = new NetworkService();
    private ExecutorService executorService;
    private List<NetworkListener> listeners = new ArrayList<>();
    private NetworkListener listener; // 保留旧的监听器引用，为了兼容
    
    // 使用Map管理不同服务类型的服务实例
    private Map<ServiceType, ServiceInstance> serviceInstances = new HashMap<>();
    
    /**
     * 获取指定服务类型的连接客户端数量
     */
    public int getConnectedClientCount(ServiceType serviceType) {
        ServiceInstance instance = serviceInstances.get(serviceType);
        return instance != null ? instance.getConnectedClientCount() : 0;
    }
    
    /**
     * 获取指定服务类型的配置
     */
    public NetworkConfig getConfig(ServiceType serviceType) {
        ServiceInstance instance = serviceInstances.get(serviceType);
        return instance != null ? instance.getConfig() : null;
    }
    
    /**
     * 获取当前配置（默认返回BURNER服务类型的配置，为了兼容旧代码）
     */
    public NetworkConfig getConfig() {
        return getConfig(ServiceType.BURNER);
    }
    
    /**
     * 获取指定服务类型的运行状态
     */
    public boolean isServiceRunning(ServiceType serviceType) {
        ServiceInstance instance = serviceInstances.get(serviceType);
        return instance != null && instance.isRunning();
    }
    
    /**
     * 判断默认服务类型（BURNER）是否正在运行
     * 为了兼容旧代码
     */
    public boolean isRunning() {
        return isServiceRunning(ServiceType.BURNER);
    }
    
    // 添加网络监听器
    public void addListener(NetworkListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    // 移除网络监听器
    public void removeListener(NetworkListener listener) {
        listeners.remove(listener);
    }
    
    private NetworkService() {
        executorService = Executors.newCachedThreadPool();
    }
    
    public static NetworkService getInstance() {
        return instance;
    }
    
    /**
     * 设置网络监听器（旧版方法，为了兼容）
     */
    public void setNetworkListener(NetworkListener listener) {
        this.listener = listener;
        // 同时添加到监听器列表中
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * 启动扫码机网络服务
     */
    public void startScannerService(String protocolType, String host, int port, DataMode dataMode) {
        NetworkConfig config = new NetworkConfig(ServiceType.SCANNER, host, port);
        // 暂时注释掉ProtocolType设置，等待ProtocolType类问题解决
        // config.setProtocolType(ProtocolType.valueOf(protocolType));
        config.setDataMode(dataMode);
        startService(config);
    }
    
    /**
     * 启动烧录机网络服务
     */
    public void startBurnerService(String protocolType, String host, int port, DataMode dataMode) {
        NetworkConfig config = new NetworkConfig(ServiceType.BURNER, host, port);
        // 暂时注释掉ProtocolType设置，等待ProtocolType类问题解决
        // config.setProtocolType(ProtocolType.valueOf(protocolType));
        config.setDataMode(dataMode);
        startService(config);
    }
    
    /**
     * 启动网络服务
     */
    public synchronized void startService(NetworkConfig config) {
        ServiceType serviceType = config.getServiceType();
        
        // 停止该服务类型的现有服务
        stopService(serviceType);
        
        // 创建新的服务实例
        ServiceInstance instance = new ServiceInstance(config);
        instance.setRunning(true);
        serviceInstances.put(serviceType, instance);
        
        // 日志中包含别名信息
        String aliasInfo = config.getAlias() != null && !config.getAlias().isEmpty() ? "[别名: " + config.getAlias() + "] " : "";
        logger.info("启动网络服务，类型: " + config.getServiceType().getDescription() + aliasInfo + ", 协议: " + config.getProtocolType() + ", 地址: " + config.getHost() + ", 端口: " + config.getPort() + ", 数据模式: " + config.getDataMode());
        
        switch (config.getProtocolType()) {
            case TCP_SERVER:
                startTcpServer(instance);
                break;
            case TCP_CLIENT:
                startTcpClient(instance);
                break;
            case UDP:
                startUdpServer(instance);
                break;
        }
    }
    
    /**
     * 停止指定类型的网络服务
     */
    public synchronized void stopService(ServiceType serviceType) {
        ServiceInstance instance = serviceInstances.get(serviceType);
        if (instance == null || !instance.isRunning()) {
            return;
        }
        
        instance.setRunning(false);
        
        try {
            if (instance.getTcpServerSocket() != null && !instance.getTcpServerSocket().isClosed()) {
                instance.getTcpServerSocket().close();
            }
        } catch (IOException e) {
            logger.warn("Failed to close TCP server socket for " + serviceType.getDescription() + ": " + e.getMessage());
        }
        
        try {
            if (instance.getTcpClientSocket() != null && !instance.getTcpClientSocket().isClosed()) {
                instance.getTcpClientSocket().close();
            }
        } catch (IOException e) {
            logger.warn("Failed to close TCP client socket for " + serviceType.getDescription() + ": " + e.getMessage());
        }
        
        if (instance.getUdpSocket() != null && !instance.getUdpSocket().isClosed()) {
            instance.getUdpSocket().close();
        }
        
        logger.info(serviceType.getDescription() + " network service stopped");
        notifyConnectionStatus(false, serviceType);
    }
    
    /**
     * 停止所有网络服务
     */
    public synchronized void stopAllServices() {
        for (ServiceType serviceType : ServiceType.values()) {
            stopService(serviceType);
        }
    }
    
    /**
     * 停止网络服务（旧版方法，为了兼容）
     */
    public synchronized void stopService() {
        stopAllServices();
    }
    
    /**
     * 启动TCP服务端
     */
    private void startTcpServer(ServiceInstance serviceInstance) {
        NetworkConfig config = serviceInstance.getConfig();
        executorService.submit(() -> {
            try {
                String logMsg = "[TCP Server] 开始创建ServerSocket，端口: " + config.getPort();
                logger.info(logMsg);
                notifyLogReceived(logMsg);
                
                ServerSocket serverSocket = new ServerSocket(config.getPort());
                serviceInstance.setTcpServerSocket(serverSocket);
                
                logMsg = "[TCP Server] TCP server started successfully on port: " + config.getPort();
                logger.info(logMsg);
                notifyLogReceived(logMsg);
                
                // 服务成功启动后立即通知状态为已连接
                logMsg = "[TCP Server] 通知连接状态为已连接";
                logger.info(logMsg);
                notifyLogReceived(logMsg);
                notifyConnectionStatus(true, config.getServiceType());
                
                while (serviceInstance.isRunning()) {
                    try {
                        logMsg = "[TCP Server] 等待客户端连接...";
                        logger.info(logMsg);
                        notifyLogReceived(logMsg); // 将等待客户端连接的日志传递给UI
                        
                        Socket clientSocket = serverSocket.accept();
                        logMsg = "[TCP Server] Client connected: " + clientSocket.getInetAddress().getHostAddress();
                        logger.info(logMsg);
                        notifyLogReceived(logMsg);
                        
                        // 为每个客户端创建一个线程处理数据
                        executorService.submit(() -> handleTcpConnection(clientSocket, serviceInstance));
                    } catch (IOException e) {
                        if (serviceInstance.isRunning()) { // 只有在服务运行时才记录错误
                            logMsg = "[TCP Server] Error accepting TCP connection: " + e.getMessage();
                            logger.warn(logMsg);
                            notifyLogReceived(logMsg);
                            // 注意：不要在这里设置为未连接，因为服务器仍然在运行中
                        }
                    }
                }
            } catch (IOException e) {
                String logMsg = "[TCP Server] Failed to start TCP server: " + e.getMessage();
                logger.error(logMsg, e);
                notifyLogReceived(logMsg);

            }
        });
    }
    
    /**
     * 启动TCP客户端
     */
    private void startTcpClient(ServiceInstance serviceInstance) {
        NetworkConfig config = serviceInstance.getConfig();
        executorService.submit(() -> {
            try {
                Socket clientSocket = new Socket(config.getHost(), config.getPort());
                serviceInstance.setTcpClientSocket(clientSocket);
                
                logger.info("TCP client connected to: " + config.getHost() + ":" + config.getPort());
                notifyConnectionStatus(true, config.getServiceType());
                handleTcpConnection(clientSocket, serviceInstance);
            } catch (IOException e) {
                logger.error("Failed to connect to TCP server: " + e.getMessage(), e);

            }
        });
    }
    
    /**
     * 启动UDP服务
     */
    private void startUdpServer(ServiceInstance serviceInstance) {
        NetworkConfig config = serviceInstance.getConfig();
        executorService.submit(() -> {
            try {
                DatagramSocket udpSocket = new DatagramSocket(config.getPort());
                serviceInstance.setUdpSocket(udpSocket);
                
                logger.info("UDP server started on port: " + config.getPort());
                notifyConnectionStatus(true, config.getServiceType());
                
                byte[] buffer = new byte[1024];
                while (serviceInstance.isRunning()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        udpSocket.receive(packet);
                        
                        String receivedData = processReceivedData(buffer, packet.getLength(), config);
                        notifyDataReceived(receivedData, config.getServiceType());
                        String aliasInfo = config.getAlias() != null && !config.getAlias().isEmpty() ? "[别名: " + config.getAlias() + "] " : "";
                        logger.info("Received UDP data from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + ": " + aliasInfo + receivedData);
                    } catch (IOException e) {
                        if (serviceInstance.isRunning()) { // 只有在服务运行时才记录错误
                            logger.warn("Error receiving UDP data: " + e.getMessage());
                        }
                    }
                }
            } catch (SocketException e) {
                logger.error("Failed to start UDP server: " + e.getMessage(), e);

            }
        });
    }
    
    /**
     * 处理TCP连接
     */
    private void handleTcpConnection(Socket socket, ServiceInstance serviceInstance) {
        NetworkConfig config = serviceInstance.getConfig();
        // 增加连接数计数
        synchronized(this) {
            int count = serviceInstance.getConnectedClientCount() + 1;
            serviceInstance.setConnectedClientCount(count);
            String logMsg = "[TCP Connection] " + config.getServiceType().getDescription() + " 当前连接数: " + count;
            logger.info(logMsg);
            notifyLogReceived(logMsg);
            notifyConnectionCountChanged(count, config.getServiceType()); // 通知连接数变化
        }
        
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            
            while (serviceInstance.isRunning() && (bytesRead = socket.getInputStream().read(buffer)) != -1) {
                String receivedData = processReceivedData(buffer, bytesRead, config);
                notifyDataReceived(receivedData, config.getServiceType());
                String aliasInfo = config.getAlias() != null && !config.getAlias().isEmpty() ? "[别名: " + config.getAlias() + "] " : "";
                logger.info("Received TCP data: " + aliasInfo + receivedData);
            }
        } catch (IOException e) {
            if (serviceInstance.isRunning()) { // 只有在服务运行时才记录错误
                logger.warn("Error handling TCP connection: " + e.getMessage());
            }
        } finally {
            // 连接关闭时减少计数
            synchronized(this) {
                int count = Math.max(0, serviceInstance.getConnectedClientCount() - 1);
                serviceInstance.setConnectedClientCount(count);
                String logMsg = "[TCP Connection] 客户端断开连接，" + config.getServiceType().getDescription() + " 当前连接数: " + count;
                logger.info(logMsg);
                notifyLogReceived(logMsg);
                notifyConnectionCountChanged(count, config.getServiceType()); // 通知连接数变化
            }
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                logger.warn("Failed to close TCP socket: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理接收到的数据
     */
    private String processReceivedData(byte[] data, int length, NetworkConfig config) {
        if (config.getDataMode() == DataMode.HEX) {
            return bytesToHex(data, length);
        } else {
            // ASCII模式
            return new String(data, 0, length);
        }
    }
    
    /**
     * 将字节数组转换为十六进制字符串
     */
    private String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X", bytes[i]));
        }
        return sb.toString();
    }
    
    /**
     * 发送数据到指定服务类型
     */
    public void sendData(String data, ServiceType serviceType) {
        ServiceInstance instance = serviceInstances.get(serviceType);
        if (instance == null || !instance.isRunning()) {
            logger.warn("Cannot send data to " + serviceType.getDescription() + ": network service not running");
            return;
        }
        
        NetworkConfig config = instance.getConfig();
        try {
            byte[] bytes;
            if (config.getDataMode() == DataMode.HEX) {
                bytes = hexToBytes(data);
            } else {
                bytes = data.getBytes();
            }
            
            switch (config.getProtocolType()) {
                case TCP_SERVER:
                    // TCP服务端需要知道目标客户端
                    logger.warn("TCP server cannot send data directly, need client information");
                    break;
                case TCP_CLIENT:
                    if (instance.getTcpClientSocket() != null && instance.getTcpClientSocket().isConnected()) {
                        instance.getTcpClientSocket().getOutputStream().write(bytes);
                        logger.info("Sent TCP data to " + serviceType.getDescription() + ": " + data);
                    } else {
                        logger.warn("TCP client for " + serviceType.getDescription() + " not connected");
                    }
                    break;
                case UDP:
                    if (instance.getUdpSocket() != null && !instance.getUdpSocket().isClosed()) {
                        InetAddress address = InetAddress.getByName(config.getHost());
                        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, config.getPort());
                        instance.getUdpSocket().send(packet);
                        logger.info("Sent UDP data to " + serviceType.getDescription() + " at " + config.getHost() + ":" + config.getPort() + ": " + data);
                    }
                    break;
            }
        } catch (Exception e) {
            logger.warn("Failed to send data to " + serviceType.getDescription() + ": " + e.getMessage());
        }
    }
    
    /**
     * 将十六进制字符串转换为字节数组
     */
    private byte[] hexToBytes(String hex) {
        hex = hex.replaceAll("\\s+", ""); // 移除所有空格
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                 + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
    
    /**
     * 通知数据接收（字符串格式）
     */
    private void notifyDataReceived(String data, ServiceType serviceType) {
        // 通知所有监听器（只使用新版带服务类型的方法）
        for (NetworkListener networkListener : listeners) {
            try {
                networkListener.onDataReceived(data, serviceType);
            } catch (Exception e) {
                logger.warn("通知数据接收失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 通知数据接收（字节数组格式）
     */
    private void notifyDataReceived(byte[] data, ServiceType serviceType) {
        // 通知所有监听器（只使用新版带服务类型的方法）
        for (NetworkListener networkListener : listeners) {
            try {
                networkListener.onDataReceived(data, serviceType);
            } catch (Exception e) {
                logger.warn("通知字节数组数据接收失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 通知连接状态变化
     */
    private void notifyConnectionStatus(boolean connected, ServiceType serviceType) {
        // 通知所有监听器（包括旧的监听器，但避免重复通知）
        for (NetworkListener networkListener : listeners) {
            try {
                networkListener.onConnectionStatusChanged(connected, serviceType);
            } catch (Exception e) {
                logger.warn("通知连接状态变化失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 通知日志消息
     */
    private void notifyLogReceived(String logMessage) {
        // 通知旧的监听器
        if (listener != null) {
            try {
                listener.onLogReceived(logMessage);
            } catch (Exception e) {
                // 尝试使用onLog方法
                try {
                    listener.onLog(logMessage);
                } catch (Exception ex) {
                    logger.warn("通知日志消息失败: " + ex.getMessage());
                }
            }
        }
        
        // 通知所有监听器
        for (NetworkListener networkListener : listeners) {
            try {
                if (networkListener != listener) { // 避免重复通知
                    networkListener.onLogReceived(logMessage);
                }
            } catch (Exception e) {
                // 尝试使用onLog方法
                try {
                    if (networkListener != listener) {
                        networkListener.onLog(logMessage);
                    }
                } catch (Exception ex) {
                    logger.warn("通知日志消息失败: " + ex.getMessage());
                }
            }
        }
    }
    
    /**
     * 通知连接数变化
     */
    private void notifyConnectionCountChanged(int count, ServiceType serviceType) {
        // 通知旧的监听器（调用旧方法，保持兼容性）
        if (listener != null) {
            try {
                listener.onConnectionCountChanged(count);
            } catch (Exception e) {
                logger.warn("通知连接数变化失败: " + e.getMessage());
            }
        }
        
        // 通知所有监听器
        for (NetworkListener networkListener : listeners) {
            try {
                if (networkListener != listener) { // 避免重复通知
                    networkListener.onConnectionCountChanged(count, serviceType);
                }
            } catch (Exception e) {
                logger.warn("通知连接数变化失败: " + e.getMessage());
            }
        }
    }
    /* 
         * 清理资源
     */
    public void shutdown() {
        stopService();
        executorService.shutdown();
    }
    

}