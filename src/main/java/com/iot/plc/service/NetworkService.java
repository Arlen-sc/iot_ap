package com.iot.plc.service;

import com.iot.plc.logger.LoggerFactory;
import java.io.IOException;
import java.net.*;
import java.util.logging.Logger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 网络接收服务类
 * 支持TCP服务端、TCP客户端和UDP协议
 * 支持ASCII和HEX格式的数据接收
 */
public class NetworkService {
    private static final Logger logger = LoggerFactory.getLogger(NetworkService.class.getName());
    private static final NetworkService instance = new NetworkService();
    private ExecutorService executorService;
    private boolean isRunning = false;
    private NetworkListener listener;
    
    // 当前配置
    private Config currentConfig;
    
    // 连接对象
    private ServerSocket tcpServerSocket;
    private Socket tcpClientSocket;
    private DatagramSocket udpSocket;
    
    // 数据解析模式
    public enum DataMode { ASCII, HEX }
    
    // 协议类型
    public enum ProtocolType { TCP_SERVER, TCP_CLIENT, UDP }
    
    // 配置类
    public static class Config {
        private ProtocolType protocolType;
        private String host;
        private int port;
        private DataMode dataMode;
        
        public Config(ProtocolType protocolType, String host, int port, DataMode dataMode) {
            this.protocolType = protocolType;
            this.host = host;
            this.port = port;
            this.dataMode = dataMode;
        }
        
        // Getters and setters
        public ProtocolType getProtocolType() { return protocolType; }
        public void setProtocolType(ProtocolType protocolType) { this.protocolType = protocolType; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public DataMode getDataMode() { return dataMode; }
        public void setDataMode(DataMode dataMode) { this.dataMode = dataMode; }
    }
    
    // 数据监听器接口
    public interface NetworkListener {
        void onDataReceived(String data);
        void onConnectionStatusChanged(boolean connected);
    }
    
    private NetworkService() {
        executorService = Executors.newCachedThreadPool();
    }
    
    public static NetworkService getInstance() {
        return instance;
    }
    
    public void setNetworkListener(NetworkListener listener) {
        this.listener = listener;
    }
    
    /**
     * 启动网络服务
     */
    public synchronized void startService(Config config) {
        stopService(); // 先停止之前的服务
        currentConfig = config;
        isRunning = true;
        
        logger.info("Starting network service: " + config.protocolType + ", host: " + config.host + ", port: " + config.port + ", mode: " + config.dataMode);
        
        switch (config.protocolType) {
            case TCP_SERVER:
                startTcpServer();
                break;
            case TCP_CLIENT:
                startTcpClient();
                break;
            case UDP:
                startUdpServer();
                break;
        }
    }
    
    /**
     * 停止网络服务
     */
    public synchronized void stopService() {
        if (!isRunning) return;
        
        isRunning = false;
        
        try {
            if (tcpServerSocket != null && !tcpServerSocket.isClosed()) {
                tcpServerSocket.close();
            }
        } catch (IOException e) {
            logger.warning("Failed to close TCP server socket: " + e.getMessage());
        }
        
        try {
            if (tcpClientSocket != null && !tcpClientSocket.isClosed()) {
                tcpClientSocket.close();
            }
        } catch (IOException e) {
            logger.warning("Failed to close TCP client socket: " + e.getMessage());
        }
        
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        
        logger.info("Network service stopped");
        notifyConnectionStatus(false);
    }
    
    /**
     * 启动TCP服务端
     */
    private void startTcpServer() {
        executorService.submit(() -> {
            try {
                tcpServerSocket = new ServerSocket(currentConfig.getPort());
                logger.info("TCP server started on port: " + currentConfig.getPort());
                
                while (isRunning) {
                    try {
                        Socket clientSocket = tcpServerSocket.accept();
                        logger.info("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                        notifyConnectionStatus(true);
                        
                        // 为每个客户端创建一个线程处理数据
                        executorService.submit(() -> handleTcpConnection(clientSocket));
                    } catch (IOException e) {
                        if (isRunning) { // 只有在服务运行时才记录错误
                            logger.warning("Error accepting TCP connection: " + e.getMessage());
                            notifyConnectionStatus(false);
                        }
                    }
                }
            } catch (IOException e) {
                logger.severe("Failed to start TCP server: " + e.getMessage());
                notifyConnectionStatus(false);
            }
        });
    }
    
    /**
     * 启动TCP客户端
     */
    private void startTcpClient() {
        executorService.submit(() -> {
            try {
                tcpClientSocket = new Socket(currentConfig.getHost(), currentConfig.getPort());
                logger.info("TCP client connected to: " + currentConfig.getHost() + ":" + currentConfig.getPort());
                notifyConnectionStatus(true);
                handleTcpConnection(tcpClientSocket);
            } catch (IOException e) {
                logger.severe("Failed to connect to TCP server: " + e.getMessage());
                notifyConnectionStatus(false);
            }
        });
    }
    
    /**
     * 启动UDP服务
     */
    private void startUdpServer() {
        executorService.submit(() -> {
            try {
                udpSocket = new DatagramSocket(currentConfig.getPort());
                logger.info("UDP server started on port: " + currentConfig.getPort());
                notifyConnectionStatus(true);
                
                byte[] buffer = new byte[1024];
                while (isRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        udpSocket.receive(packet);
                        
                        String receivedData = processReceivedData(buffer, packet.getLength());
                        notifyDataReceived(receivedData);
                        logger.info("Received UDP data from " + packet.getAddress().getHostAddress() + ":" + packet.getPort() + ": " + receivedData);
                    } catch (IOException e) {
                        if (isRunning) { // 只有在服务运行时才记录错误
                            logger.warning("Error receiving UDP data: " + e.getMessage());
                        }
                    }
                }
            } catch (SocketException e) {
                logger.severe("Failed to start UDP server: " + e.getMessage());
                notifyConnectionStatus(false);
            }
        });
    }
    
    /**
     * 处理TCP连接
     */
    private void handleTcpConnection(Socket socket) {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead;
            
            while (isRunning && (bytesRead = socket.getInputStream().read(buffer)) != -1) {
                String receivedData = processReceivedData(buffer, bytesRead);
                notifyDataReceived(receivedData);
                logger.info("Received TCP data: " + receivedData);
            }
        } catch (IOException e) {
            if (isRunning) { // 只有在服务运行时才记录错误
                logger.warning("Error handling TCP connection: " + e.getMessage());
            }
        } finally {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                logger.warning("Failed to close TCP socket: " + e.getMessage());
            }
            notifyConnectionStatus(false);
        }
    }
    
    /**
     * 处理接收到的数据
     */
    private String processReceivedData(byte[] data, int length) {
        if (currentConfig.getDataMode() == DataMode.HEX) {
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
     * 发送数据
     */
    public void sendData(String data) {
        if (!isRunning || currentConfig == null) {
            logger.warning("Cannot send data: network service not running");
            return;
        }
        
        try {
            byte[] bytes;
            if (currentConfig.getDataMode() == DataMode.HEX) {
                bytes = hexToBytes(data);
            } else {
                bytes = data.getBytes();
            }
            
            switch (currentConfig.getProtocolType()) {
                case TCP_SERVER:
                    // TCP服务端需要知道目标客户端
                    logger.warning("TCP server cannot send data directly, need client information");
                    break;
                case TCP_CLIENT:
                    if (tcpClientSocket != null && tcpClientSocket.isConnected()) {
                        tcpClientSocket.getOutputStream().write(bytes);
                        logger.info("Sent TCP data: " + data);
                    } else {
                        logger.warning("TCP client not connected");
                    }
                    break;
                case UDP:
                    if (udpSocket != null && !udpSocket.isClosed()) {
                        InetAddress address = InetAddress.getByName(currentConfig.getHost());
                        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, currentConfig.getPort());
                        udpSocket.send(packet);
                        logger.info("Sent UDP data to " + currentConfig.getHost() + ":" + currentConfig.getPort() + ": " + data);
                    }
                    break;
            }
        } catch (Exception e) {
            logger.warning("Failed to send data: " + e.getMessage());
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
     * 通知数据接收
     */
    private void notifyDataReceived(String data) {
        if (listener != null) {
            listener.onDataReceived(data);
        }
    }
    
    /**
     * 通知连接状态变化
     */
    private void notifyConnectionStatus(boolean connected) {
        if (listener != null) {
            listener.onConnectionStatusChanged(connected);
        }
    }
    
    /**
     * 获取当前运行状态
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * 获取当前配置
     */
    public Config getCurrentConfig() {
        return currentConfig;
    }
    
    /**
     * 清理资源
     */
    public void shutdown() {
        stopService();
        executorService.shutdown();
    }
}