package com.iot.plc.service;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.iot.plc.config.NetworkConfig;
import com.iot.plc.enumx.ProtocolType;
import com.iot.plc.enumx.TcpServiceEnum;
import com.iot.plc.listener.NetworkListener;
import com.iot.plc.logger.Logger;
import com.iot.plc.model.DataMode;
import com.iot.plc.util.HexUtils;

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
     * 通用启动TCP服务方法
     * @param serviceType 服务类型
     * @param host 主机地址
     * @param port 端口号
     * @param protocolType 协议类型
     * @param dataMode 数据模式
     * @param alias 别名
     */
    public void startTcpService(TcpServiceEnum serviceType, String host, int port, ProtocolType protocolType, DataMode dataMode, String alias) {
        NetworkConfig config = new NetworkConfig(serviceType, host, port);
        config.setProtocolType(protocolType);
        config.setDataMode(dataMode);
        if (alias != null && !alias.isEmpty()) {
            config.setAlias(alias);
        }
        startService(config);
    }
    
    /**
     * 通用启动TCP服务方法 - 从配置管理获取参数
     * @param serviceType 服务类型
     */
    public void startTcpService(TcpServiceEnum serviceType) {
        try {
            // 从配置管理中获取参数
            String configHost = ConfigService.getInstance().getConfigValueByKey(serviceType.getCode() + ".tcp.host");
            // 端口号默认值为8888
            String configPort = ConfigService.getInstance().getConfigValueByKey(serviceType.getCode() + ".tcp.port");
            // 协议类型默认值为TCP_SERVER
            String configProtocol = ConfigService.getInstance().getConfigValueByKey(serviceType.getCode() + ".tcp.protocol");
            // 数据模式默认值为ASCII
            String configDataMode = ConfigService.getInstance().getConfigValueByKey(serviceType.getCode() + ".tcp.datamodel");
            // 别名默认值为空字符串
            String configAlias = ConfigService.getInstance().getConfigValueByKey(serviceType.getCode() + ".tcp.alias");
            //如果host=0.0.0.0,就不需要启动服务
            if (configHost.equals("0.0.0.0")) {
                logger.info("主机地址为0.0.0.0，不启动服务");
                return;
            }
            // 处理协议类型转换，支持中文配置值
            ProtocolType protocolType = ProtocolType.TCP_SERVER; // 默认值
            if (configProtocol != null) {
                // 支持中文配置值映射
                if (configProtocol.equals("TCP服务端")) {
                    protocolType = ProtocolType.TCP_SERVER;
                } else if (configProtocol.equals("TCP客户端")) {
                    protocolType = ProtocolType.TCP_CLIENT;
                } else if (configProtocol.equals("UDP")) {
                    protocolType = ProtocolType.UDP;
                } else {
                    // 尝试直接转换（用于英文枚举值）
                    try {
                        protocolType = ProtocolType.valueOf(configProtocol);
                    } catch (IllegalArgumentException e) {
                        logger.warn("未知的协议类型配置: " + configProtocol + ", 使用默认值: TCP_SERVER");
                    }
                }
            }
            
            // 处理数据模式转换
            DataMode dataMode = DataMode.ASCII; // 默认值
            if (configDataMode != null) {
                try {
                    dataMode = DataMode.valueOf(configDataMode);
                } catch (IllegalArgumentException e) {
                    logger.warn("未知的数据模式配置: " + configDataMode + ", 使用默认值: ASCII");
                }
            }
            
            // 调用现有的startTcpService方法
            startTcpService(serviceType, configHost, Integer.parseInt(configPort), protocolType, dataMode, configAlias);
        } catch (Exception e) {
            logger.error("启动" + serviceType.getDescription() + "服务失败: " + e.getMessage());
            throw e; // 抛出异常以便上层处理
        }
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
     * 启动TCP服务端
     */
    private void startTcpServer(ServiceInstance serviceInstance) {
        NetworkConfig config = serviceInstance.getConfig();
        executorService.submit(() -> {
            try {
                // 获取别名信息
                String alias = config.getAlias() != null && !config.getAlias().isEmpty() ? "[别名: " + config.getAlias() + "] " : "";
                
                String logMsg = "[TCP服务端] " + alias + "开始创建ServerSocket，端口: " + config.getPort();
                logger.info(logMsg);
                notifyLogReceived(logMsg);
                
                ServerSocket serverSocket = new ServerSocket(config.getPort());
                serviceInstance.setTcpServerSocket(serverSocket);
                
                logMsg = "[TCP服务端] " + alias + "TCP服务端启动成功，端口: " + config.getPort();
                logger.info(logMsg);
                notifyLogReceived(logMsg);
                
                // 服务成功启动后立即通知状态为已连接
                logMsg = "[TCP服务端] " + alias + "通知连接状态为已连接";
                logger.info(logMsg);
                notifyLogReceived(logMsg);
                notifyConnectionStatus(true, config.getServiceType());
                
                while (serviceInstance.isRunning()) {
                    try {
                        logMsg = "[TCP服务端] " + alias + "等待客户端连接...";
                        logger.info(logMsg);
                        notifyLogReceived(logMsg); // 将等待客户端连接的日志传递给UI
                        
                        Socket clientSocket = serverSocket.accept();
                        logMsg = "[TCP服务端] " + alias + "客户端已连接: " + clientSocket.getInetAddress().getHostAddress();
                        logger.info(logMsg);
                        notifyLogReceived(logMsg);
                        
                        // 为每个客户端创建一个线程处理数据
                        executorService.submit(() -> handleTcpConnection(clientSocket, serviceInstance));
                    } catch (IOException e) {
                        if (serviceInstance.isRunning()) { // 只有在服务运行时才记录错误
                            logMsg = "[TCP服务端] " + alias + "接受TCP连接失败: " + e.getMessage();
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
                //把当前buffer有效数值直接回传
                byte[] copyBuffer = Arrays.copyOf(buffer, bytesRead);
                notifyDataReceived(copyBuffer, config.getServiceType());
                String receivedData = processReceivedData(copyBuffer, bytesRead, config);
                // logger.info("收到TCP数据: " + receivedData);
                notifyDataReceived(receivedData, config.getServiceType());
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
        String resultString="";
        logger.debug(config.toString());
        if (config.getDataMode() == DataMode.HEX) {
            // logger.debug("收到原始HEX数据长度: " + length);
            // logger.debug("原始HEX数据: " + HexUtils.bytesToHex(data, length));
            String hexStr = HexUtils.bytesToHex(data, length);
            // notifyDataReceived(hexStr, config.getServiceType());
            try {
                resultString=HexUtils.hexToString(hexStr, "UTF-8");
            } catch (Exception e) {
                logger.error("处理HEX数据错误: " + e.getMessage(), e);
            }
        } else {
            // ASCII模式：使用UTF-8编码，增加异常处理
            try {
                resultString = new String(data, 0, length, "UTF-8");
                logger.debug("格式化接收数据(ASCII): " + resultString);
            } catch (Exception e) {
                // 如果UTF-8解码失败，使用默认编码并记录警告
                logger.warn("UTF-8解码失败，使用默认编码: " + e.getMessage());
                resultString=new  String(data, 0, length);
            }
        }
        return resultString;
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
                bytes = HexUtils.hexToBytes(data);
            } else {
                bytes = data.getBytes();
            }
            
            switch (config.getProtocolType()) {
                case TCP_SERVER:
                    // 向所有连接的客户端广播数据
                    logger.info("TCP_SERVER向" + serviceType.getDescription() + "的所有连接客户端广播数据: " + data);
                    if (!instance.getConnectedClients().isEmpty()) {
                        int successCount = 0;
                        int failCount = 0;
                        // 遍历所有连接的客户端
                        for (Socket clientSocket : instance.getConnectedClients()) {
                            try {
                                if (clientSocket != null && !clientSocket.isClosed()) {
                                    clientSocket.getOutputStream().write(bytes);
                                    successCount++;
                                    logger.debug("成功发送数据到客户端: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                                }
                            } catch (IOException e) {
                                failCount++;
                                logger.warn("发送数据到客户端" + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort() + "失败: " + e.getMessage());
                                // 可以考虑从connectedClients中移除失败的连接
                            }
                        }
                        logger.info("已向" + serviceType.getDescription() + "的" + successCount + "个客户端成功发送数据，" + failCount + "个客户端发送失败");
                    } else {
                        logger.warn(serviceType.getDescription() + "没有连接的客户端，无法广播数据");
                    }
            break;
                case TCP_CLIENT:
                    logger.info("TCP_CLIENT发送TCP数据到" + serviceType.getDescription() + ": " + data);
                    if (instance.getTcpClientSocket() != null && instance.getTcpClientSocket().isConnected()) {
                        instance.getTcpClientSocket().getOutputStream().write(bytes);
                        logger.info("已发送TCP数据到" + serviceType.getDescription() + ": " + data);
                    } else {
                        logger.warn(serviceType.getDescription() + "的TCP客户端未连接");
                    }
                    break;
                case UDP:
                    logger.info("UDP发送数据到" + serviceType.getDescription() + ": " + data);

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
                 logger.warn("通知旧监听器连接数变化失败: " + e.getMessage());
             }
         }
         
         // 通知所有新的监听器（调用带服务类型的方法）
         for (NetworkListener networkListener : listeners) {
             try {
                 if (networkListener != listener) { // 避免重复通知
                     networkListener.onConnectionCountChanged(count, serviceType);
                 }
             } catch (Exception e) {
                 logger.warn("通知监听器连接数变化失败: " + e.getMessage());
             }
         }
     }

     /**
      * 清理资源
      */
    public void shutdown() {
        stopAllServices();
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
                bytes = HexUtils.hexToBytes(data);
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
            String logData = config.getDataMode() == DataMode.HEX ? HexUtils.bytesToHex(data) : 
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
            String logData = config.getDataMode() == DataMode.HEX ? HexUtils.bytesToHex(data) : 
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