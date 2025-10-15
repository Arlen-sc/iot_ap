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
import com.iot.plc.enumx.TcpServiceEnum;
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
    private Map<TcpServiceEnum, ServiceInstance> serviceInstances = new HashMap<>();
    
    /**
     * 获取指定服务类型的连接客户端数量
     */
    public int getConnectedClientCount(TcpServiceEnum serviceType) {
        ServiceInstance instance = serviceInstances.get(serviceType);
        return instance != null ? instance.getConnectedClientCount() : 0;
    }
    
    /**
     * 获取指定服务类型的配置
     */
    public NetworkConfig getConfig(TcpServiceEnum serviceType) {
        ServiceInstance instance = serviceInstances.get(serviceType);
        return instance != null ? instance.getConfig() : null;
    }
    
    /**
     * 获取当前配置（默认返回BURNER服务类型的配置，为了兼容旧代码）
     */
    public NetworkConfig getConfig() {
        return getConfig(TcpServiceEnum.BURNER);
    }
    
    /**
     * 获取指定服务类型的运行状态
     */
    public boolean isServiceRunning(TcpServiceEnum serviceType) {
        ServiceInstance instance = serviceInstances.get(serviceType);
        return instance != null && instance.isRunning();
    }
    
    /**
     * 判断默认服务类型（BURNER）是否正在运行
     * 为了兼容旧代码
     */
    public boolean isServiceRunning() {
        return isServiceRunning(TcpServiceEnum.BURNER);
    }
    

    
    /**
     */
    public boolean isRunning() {
        return isServiceRunning(TcpServiceEnum.BURNER);
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
    
    /**
     * 私有构造函数，防止外部实例化
     */
    private NetworkService() {
        executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * 获取网络服务单例实例
     * @return 网络服务实例
     */
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
        NetworkConfig config = new NetworkConfig(TcpServiceEnum.SCANNER, host, port);
        // 暂时注释掉ProtocolType设置，等待ProtocolType类问题解决
        // config.setProtocolType(ProtocolType.valueOf(protocolType));
        config.setDataMode(dataMode);
        startService(config);
    }
    
    /**
     * 启动烧录机网络服务
     */
    public void startBurnerService(String protocolType, String host, int port, DataMode dataMode) {
        NetworkConfig config = new NetworkConfig(TcpServiceEnum.BURNER, host, port);
        // 暂时注释掉ProtocolType设置，等待ProtocolType类问题解决
        // config.setProtocolType(ProtocolType.valueOf(protocolType));
        config.setDataMode(dataMode);
        startService(config);
    }
    
    /**
     * 启动网络服务
     */
    public synchronized void startService(NetworkConfig config) {
        TcpServiceEnum serviceType = config.getServiceType();
        
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
    public synchronized void stopService(TcpServiceEnum serviceType) {
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
            logger.warn("关闭TCP服务端套接字失败，服务类型: " + serviceType.getDescription() + ", 错误: " + e.getMessage());
        }
        
        try {
            if (instance.getTcpClientSocket() != null && !instance.getTcpClientSocket().isClosed()) {
                instance.getTcpClientSocket().close();
            }
        } catch (IOException e) {
            logger.warn("关闭TCP客户端套接字失败，服务类型: " + serviceType.getDescription() + ", 错误: " + e.getMessage());
        }
        
        if (instance.getUdpSocket() != null && !instance.getUdpSocket().isClosed()) {
            instance.getUdpSocket().close();
        }
        
        logger.info(serviceType.getDescription() + " 网络服务已停止");
        notifyConnectionStatus(false, serviceType);
    }
    
    /**
     * 停止所有网络服务
     */
    public synchronized void stopAllServices() {
        for (TcpServiceEnum serviceType : TcpServiceEnum.values()) {
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
                String logMsg = "[TCP服务端] 开始创建ServerSocket，端口: " + config.getPort();
                logger.info(logMsg);
                notifyLogReceived(logMsg);
                
                ServerSocket serverSocket = new ServerSocket(config.getPort());
                serviceInstance.setTcpServerSocket(serverSocket);
                
                logMsg = "[TCP服务端] TCP服务端启动成功，端口: " + config.getPort();
                logger.info(logMsg);
                notifyLogReceived(logMsg);
                
                // 服务成功启动后立即通知状态为已连接
                logMsg = "[TCP服务端] 通知连接状态为已连接";
                logger.info(logMsg);
                notifyLogReceived(logMsg);
                notifyConnectionStatus(true, config.getServiceType());
                
                while (serviceInstance.isRunning()) {
                    try {
                        logMsg = "[TCP服务端] 等待客户端连接...";
                        logger.info(logMsg);
                        notifyLogReceived(logMsg); // 将等待客户端连接的日志传递给UI
                        
                        Socket clientSocket = serverSocket.accept();
                        logMsg = "[TCP服务端] 客户端已连接: " + clientSocket.getInetAddress().getHostAddress();
                        logger.info(logMsg);
                        notifyLogReceived(logMsg);
                        
                        // 为每个客户端创建一个线程处理数据
                        executorService.submit(() -> handleTcpConnection(clientSocket, serviceInstance));
                    } catch (IOException e) {
                        if (serviceInstance.isRunning()) { // 只有在服务运行时才记录错误
                            logMsg = "[TCP服务端] 接受TCP连接失败: " + e.getMessage();
                            logger.warn(logMsg);
                            notifyLogReceived(logMsg);
                            // 注意：不要在这里设置为未连接，因为服务器仍然在运行中
                        }
                    }
                }
            } catch (IOException e) {
                String logMsg = "[TCP服务端] 启动TCP服务端失败: " + e.getMessage();
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
            String serviceTypeDesc = config.getServiceType().getDescription();
            String logMsg = "[TCP客户端] 尝试连接到 " + serviceTypeDesc + ": " + config.getHost() + ":" + config.getPort();
            logger.info(logMsg);
            notifyLogReceived(logMsg);
            
            try {
                Socket clientSocket = new Socket(config.getHost(), config.getPort());
                serviceInstance.setTcpClientSocket(clientSocket);
                
                logMsg = "[TCP客户端] 成功连接到 " + serviceTypeDesc + ": " + config.getHost() + ":" + config.getPort();
                logger.info(logMsg);
                notifyLogReceived(logMsg);
                notifyConnectionStatus(true, config.getServiceType());
                handleTcpConnection(clientSocket, serviceInstance);
            } catch (IOException e) {
                logMsg = "[TCP客户端] 连接 " + serviceTypeDesc + " 失败: " + config.getHost() + ":" + config.getPort() + ", 错误: " + e.getMessage();
                logger.error(logMsg, e);
                notifyLogReceived(logMsg);
                
                // 通知连接状态为断开
                notifyConnectionStatus(false, config.getServiceType());
                
                // 如果是连接被拒绝错误，提供更具体的错误信息
                if (e instanceof java.net.ConnectException) {
                    String detailMsg = "[TCP客户端] 连接被拒绝，可能的原因: 1.目标主机未运行对应服务 2.主机地址或端口配置错误 3.网络防火墙阻止连接";
                    logger.error(detailMsg);
                    notifyLogReceived(detailMsg);
                }
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
                
                String serviceTypeDesc = config.getServiceType().getDescription();
                logger.info(serviceTypeDesc + " UDP服务已启动，端口: " + config.getPort());
                notifyConnectionStatus(true, config.getServiceType());
                
                byte[] buffer = new byte[1024];
                while (serviceInstance.isRunning()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        udpSocket.receive(packet);
                        
                        String receivedData = processReceivedData(buffer, packet.getLength(), config);
                        notifyDataReceived(receivedData, config.getServiceType());
                        String aliasInfo = config.getAlias() != null && !config.getAlias().isEmpty() ? "[别名: " + config.getAlias() + "] " : "";
                        logger.info("收到来自 " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + " 的UDP数据: " + aliasInfo + receivedData);
                    } catch (IOException e) {
                        if (serviceInstance.isRunning()) { // 只有在服务运行时才记录错误
                            logger.warn("接收UDP数据错误: " + e.getMessage());
                        }
                    }
                }
            } catch (SocketException e) {
                logger.error("启动UDP服务失败: " + e.getMessage(), e);

            }
        });
    }
    
    /**
     * 处理TCP连接
     */
    private void handleTcpConnection(Socket socket, ServiceInstance serviceInstance) {
        NetworkConfig config = serviceInstance.getConfig();
        // 增加连接数计数并添加到客户端集合
        synchronized(this) {
            int count = serviceInstance.getConnectedClientCount() + 1;
            serviceInstance.setConnectedClientCount(count);
            serviceInstance.addConnectedClient(socket); // 添加到客户端集合
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
                
                // 根据数据模式记录相应格式的日志
                if (config.getDataMode() == DataMode.HEX) {
                    // HEX模式：记录带空格的十六进制字符串，并添加原始无空格版本便于调试
                    String rawHexData = bytesToHex(buffer, bytesRead);
                    logger.info("收到TCP数据(HEX): " + aliasInfo + receivedData);
                    logger.debug("收到TCP原始HEX数据(无空格): " + aliasInfo + rawHexData);
                } else {
                    // ASCII模式：记录ASCII字符串，并同时记录十六进制形式以便调试
                    String hexData = bytesToHex(buffer, bytesRead);
                    logger.info("收到TCP数据(ASCII): " + aliasInfo + receivedData);
                    logger.debug("收到TCP数据(HEX): " + aliasInfo + hexData);
                }
            }
        } catch (IOException e) {
            if (serviceInstance.isRunning()) { // 只有在服务运行时才记录错误
                logger.warn("处理TCP连接错误: " + e.getMessage());
            }
        } finally {
            // 连接关闭时减少计数并从集合中移除
            synchronized(this) {
                int count = Math.max(0, serviceInstance.getConnectedClientCount() - 1);
                serviceInstance.setConnectedClientCount(count);
                serviceInstance.removeConnectedClient(socket); // 从客户端集合中移除
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
                logger.warn("关闭TCP套接字失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理接收到的数据
     */
    private String processReceivedData(byte[] data, int length, NetworkConfig config) {
        if (config.getDataMode() == DataMode.HEX) {
            // 返回格式化的十六进制字符串，添加空格分隔以便于阅读
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(String.format("%02X", data[i]));
            }
            return sb.toString();
        } else {
            // ASCII模式：使用UTF-8编码，增加异常处理
            try {
                return new String(data, 0, length, "UTF-8");
            } catch (Exception e) {
                // 如果UTF-8解码失败，使用默认编码并记录警告
                logger.warn("UTF-8解码失败，使用默认编码: " + e.getMessage());
                return new String(data, 0, length);
            }
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
    public void sendData(String data, TcpServiceEnum serviceType) {
        ServiceInstance instance = serviceInstances.get(serviceType);
        if (instance == null || !instance.isRunning()) {
            logger.warn("无法发送数据到" + serviceType.getDescription() + ": 网络服务未运行");
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
            logger.warn(serviceType.getDescription() + " TCP服务端不能直接发送数据，需要客户端信息");
            // 可以在这里添加获取第一个连接的客户端并发送数据的逻辑
            if (!instance.getConnectedClients().isEmpty()) {
                Socket clientSocket = instance.getConnectedClients().iterator().next();
                try {
                    clientSocket.getOutputStream().write(bytes);
                    logger.info("已发送TCP数据到" + serviceType.getDescription() + "的第一个连接客户端: " + data);
                } catch (IOException e) {
                    logger.warn("发送数据到TCP客户端失败: " + e.getMessage());
                }
            }
            break;
                case TCP_CLIENT:
                    if (instance.getTcpClientSocket() != null && instance.getTcpClientSocket().isConnected()) {
                        instance.getTcpClientSocket().getOutputStream().write(bytes);
                        logger.info("已发送TCP数据到" + serviceType.getDescription() + ": " + data);
                    } else {
                        logger.warn(serviceType.getDescription() + "的TCP客户端未连接");
                    }
                    break;
                case UDP:
                    if (instance.getUdpSocket() != null && !instance.getUdpSocket().isClosed()) {
                        InetAddress address = InetAddress.getByName(config.getHost());
                        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, config.getPort());
                        instance.getUdpSocket().send(packet);
                        logger.info("已发送UDP数据到" + serviceType.getDescription() + "，地址: " + config.getHost() + ":" + config.getPort() + ": " + data);
                    }
                break;
            }
        } catch (IOException e) {
            logger.warn("发送数据到" + serviceType.getDescription() + "失败: " + e.getMessage());
        }
    }
    
    
    /**
     * 将十六进制字符串转换为字节数组
     */
    private byte[] hexToBytes(String hex) {
        // 检查输入是否为空
        if (hex == null || hex.trim().isEmpty()) {
            logger.warn("尝试转换空的十六进制字符串");
            return new byte[0];
        }
        
        // 移除所有空格和分隔符
        hex = hex.replaceAll("\\s+", "");
        
        // 检查长度是否为偶数
        if (hex.length() % 2 != 0) {
            logger.warn("十六进制字符串长度不是偶数，自动补0: " + hex);
            hex = "0" + hex; // 在前面补0
        }
        
        int len = hex.length();
        byte[] data = new byte[len / 2];
        
        // 添加错误处理
        try {
            for (int i = 0; i < len; i += 2) {
                // 检查字符是否为有效十六进制字符
                if (!isHexChar(hex.charAt(i)) || !isHexChar(hex.charAt(i + 1))) {
                    throw new IllegalArgumentException("无效的十六进制字符: " + hex.substring(i, i + 2));
                }
                
                data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                     + Character.digit(hex.charAt(i + 1), 16));
            }
            return data;
        } catch (Exception e) {
            logger.error("十六进制字符串转换失败: " + hex, e);
            throw new IllegalArgumentException("无效的十六进制字符串: " + hex, e);
        }
    }
    
    /**
     * 检查字符是否为有效的十六进制字符
     */
    private boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }
    
    /**
     * 通知数据接收（字符串格式）
     */
    private void notifyDataReceived(String data, TcpServiceEnum serviceType) {
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
    private void notifyDataReceived(byte[] data, TcpServiceEnum serviceType) {
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
    private void notifyConnectionStatus(boolean connected, TcpServiceEnum serviceType) {
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
    private void notifyConnectionCountChanged(int count, TcpServiceEnum serviceType) {
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
    
    /**
     * 发送数据并等待响应
     * @param data 要发送的数据
     * @param serviceType 服务类型
     * @param timeoutMs 等待超时时间（毫秒）
     * @return 接收到的响应数据，如果超时或出错则返回null
     */
    public String sendDataAndWaitForResponse(String data, TcpServiceEnum serviceType, int timeoutMs) {
        ServiceInstance instance = serviceInstances.get(serviceType);
        if (instance == null || !instance.isRunning()) {
            logger.warn("无法发送数据到" + serviceType.getDescription() + ": 网络服务未运行");
            return null;
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
                    logger.warn(serviceType.getDescription() + " TCP服务端不能直接发送数据并等待响应，需要客户端信息");
                    return null;
                case TCP_CLIENT:
                    return sendTcpClientDataAndWaitResponse(instance, bytes, timeoutMs, config);
                case UDP:
                    return sendUdpDataAndWaitResponse(instance, bytes, timeoutMs, config);
                default:
                    logger.warn("不支持的协议类型: " + config.getProtocolType());
                    return null;
            }
        } catch (Exception e) {
            logger.warn("发送数据并等待" + serviceType.getDescription() + "响应失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * TCP客户端发送数据并等待响应
     */
    private String sendTcpClientDataAndWaitResponse(ServiceInstance instance, byte[] data, int timeoutMs, NetworkConfig config) {
        try {
            Socket socket = instance.getTcpClientSocket();
            if (socket == null || !socket.isConnected()) {
                logger.warn("" + config.getServiceType().getDescription() + "的TCP客户端未连接");
                return null;
            }
            
            // 设置读取超时
            socket.setSoTimeout(timeoutMs);
            
            // 发送数据
            socket.getOutputStream().write(data);
            socket.getOutputStream().flush();
            // 确保日志中使用正确的数据模式格式
            String logData = config.getDataMode() == DataMode.HEX ? bytesToHex(data, data.length) : 
                (data.length > 100 ? "[太长，截断显示] " + new String(data, 0, 100, "UTF-8") + "..." : new String(data, "UTF-8"));
            logger.info("已发送TCP数据到" + config.getServiceType().getDescription() + ": " + logData);
            
            // 等待并读取响应
            byte[] buffer = new byte[1024];
            int bytesRead = socket.getInputStream().read(buffer);
            if (bytesRead > 0) {
                String response = processReceivedData(buffer, bytesRead, config);
                logger.info("已收到来自" + config.getServiceType().getDescription() + "的TCP响应: " + response);
                return response;
            }
        } catch (SocketTimeoutException e) {
            logger.warn(config.getServiceType().getDescription() + "的TCP响应超时");
        } catch (Exception e) {
            logger.warn(config.getServiceType().getDescription() + " TCP客户端通信错误: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * UDP发送数据并等待响应
     */
    private String sendUdpDataAndWaitResponse(ServiceInstance instance, byte[] data, int timeoutMs, NetworkConfig config) {
        try {
            DatagramSocket udpSocket = instance.getUdpSocket();
            if (udpSocket == null || udpSocket.isClosed()) {
                logger.warn(config.getServiceType().getDescription() + "的UDP套接字不可用");
                return null;
            }
            
            // 设置接收超时
            udpSocket.setSoTimeout(timeoutMs);
            
            // 发送数据
            InetAddress address = InetAddress.getByName(config.getHost());
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, address, config.getPort());
            udpSocket.send(sendPacket);
            // 确保日志中使用正确的数据模式格式
            String logData = config.getDataMode() == DataMode.HEX ? bytesToHex(data, data.length) : 
                (data.length > 100 ? "[太长，截断显示] " + new String(data, 0, 100, "UTF-8") + "..." : new String(data, "UTF-8"));
            logger.info("已发送UDP数据到" + config.getServiceType().getDescription() + "地址" + config.getHost() + ":" + config.getPort() + ": " + logData);
            
            // 等待并读取响应
            byte[] buffer = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            udpSocket.receive(receivePacket);
            
            String response = processReceivedData(buffer, receivePacket.getLength(), config);
            logger.info("已收到来自" + receivePacket.getAddress().getHostAddress() + ":" + receivePacket.getPort() + "的UDP响应: " + response);
            return response;
        } catch (SocketTimeoutException e) {
            logger.warn(config.getServiceType().getDescription() + "的UDP响应超时");
        } catch (Exception e) {
            logger.warn(config.getServiceType().getDescription() + " UDP通信错误: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * 发送数据并等待响应（使用默认超时时间5000毫秒）
     * @param data 要发送的数据
     * @param serviceType 服务类型
     * @return 接收到的响应数据，如果超时或出错则返回null
     */
    public String sendDataAndWaitForResponse(String data, TcpServiceEnum serviceType) {
        return sendDataAndWaitForResponse(data, serviceType, 5000);
    }

}