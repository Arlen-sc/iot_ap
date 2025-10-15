package com.iot.plc.service;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

import com.iot.plc.config.NetworkConfig;

import java.net.DatagramSocket;

/**
 * 网络服务实例类
 * 管理每种服务类型的具体实例信息
 */
public class ServiceInstance {
    private NetworkConfig config;
    private boolean running;
    private int connectedClientCount;
    private Thread thread;
    private ServerSocket tcpServerSocket;
    private Socket tcpClientSocket;
    private DatagramSocket udpSocket;
    private Set<Socket> connectedClients; // 存储连接的客户端Socket
    
    /**
     * 构造函数
     * @param config 服务配置
     */
    public ServiceInstance(NetworkConfig config) {
        this.config = config;
        this.running = false;
        this.connectedClientCount = 0;
        this.connectedClients = new HashSet<>();
        this.thread = null;
        this.tcpServerSocket = null;
        this.tcpClientSocket = null;
        this.udpSocket = null;
    }
    
    /**
     * 获取已连接的客户端集合
     */
    public Set<Socket> getConnectedClients() {
        return connectedClients;
    }
    
    /**
     * 添加客户端连接
     */
    public void addConnectedClient(Socket socket) {
        connectedClients.add(socket);
    }
    
    /**
     * 移除客户端连接
     */
    public void removeConnectedClient(Socket socket) {
        connectedClients.remove(socket);
    }
    
    /**
     * 获取服务配置
     */
    public NetworkConfig getConfig() {
        return config;
    }
    
    /**
     * 设置服务配置
     */
    public void setConfig(NetworkConfig config) {
        this.config = config;
    }
    
    /**
     * 获取服务运行状态
     */
    public boolean isRunning() {
        return running;
    }
    
    /**
     * 设置服务运行状态
     */
    public void setRunning(boolean running) {
        this.running = running;
    }
    
    /**
     * 获取已连接客户端计数
     */
    public int getConnectedClientCount() {
        return connectedClientCount;
    }
    
    /**
     * 设置已连接客户端计数
     */
    public void setConnectedClientCount(int connectedClientCount) {
        this.connectedClientCount = connectedClientCount;
    }
    
    /**
     * 获取服务线程
     */
    public Thread getThread() {
        return thread;
    }
    
    /**
     * 设置服务线程
     */
    public void setThread(Thread thread) {
        this.thread = thread;
    }
    
    /**
     * 获取TCP服务器套接字
     */
    public ServerSocket getTcpServerSocket() {
        return tcpServerSocket;
    }
    
    /**
     * 设置TCP服务器套接字
     */
    public void setTcpServerSocket(ServerSocket tcpServerSocket) {
        this.tcpServerSocket = tcpServerSocket;
    }
    
    /**
     * 获取TCP客户端套接字
     */
    public Socket getTcpClientSocket() {
        return tcpClientSocket;
    }
    
    /**
     * 设置TCP客户端套接字
     */
    public void setTcpClientSocket(Socket tcpClientSocket) {
        this.tcpClientSocket = tcpClientSocket;
    }
    
    /**
     * 获取UDP套接字
     */
    public DatagramSocket getUdpSocket() {
        return udpSocket;
    }
    
    /**
     * 设置UDP套接字
     */
    public void setUdpSocket(DatagramSocket udpSocket) {
        this.udpSocket = udpSocket;
    }
}