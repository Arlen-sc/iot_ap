package com.iot.plc.config;

import com.iot.plc.model.DataMode;
import com.iot.plc.enumx.ProtocolType;
import com.iot.plc.enumx.TcpServiceEnum;

/**
 * 网络服务配置类
 * 存储网络服务的配置参数
 */
public class NetworkConfig {
    private TcpServiceEnum serviceType;
    private String host;
    private int port;
    private String alias;
    private DataMode dataMode;
    private ProtocolType protocolType;
    
    /**
     * 构造函数
     * @param serviceType 服务类型
     * @param host 主机地址
     * @param port 端口号
     */
    public NetworkConfig(TcpServiceEnum serviceType, String host, int port) {
        this.serviceType = serviceType;
        this.host = host;
        this.port = port;
        this.dataMode = DataMode.ASCII; // 默认使用ASCII模式
        this.protocolType = ProtocolType.TCP_SERVER; // 默认使用TCP服务端模式
    }
    
    /**
     * 构造函数（仅支持TcpServiceEnum）
     * @param serviceType 服务类型
     */
    public NetworkConfig(TcpServiceEnum serviceType) {
        this.serviceType = serviceType;
        this.dataMode = DataMode.ASCII; // 默认使用ASCII模式
        this.protocolType = ProtocolType.TCP_SERVER; // 默认使用TCP服务端模式
        // 根据服务类型设置默认端口
        switch (serviceType) {
            case BURNER:
                this.port = 8888;
                break;
            case SCANNER:
                this.port = 8889;
                break;
            case PLC:
                this.port = 8890;
                break;
            default:
                this.port = 8888; // 默认端口
        }
    }
    
    /**
     * 获取服务类型
     */
    public TcpServiceEnum getServiceType() {
        return serviceType;
    }
    
    /**
     * 设置服务类型
     */
    public void setServiceType(TcpServiceEnum serviceType) {
        this.serviceType = serviceType;
    }
    
    /**
     * 获取主机地址
     */
    public String getHost() {
        return host;
    }
    
    /**
     * 设置主机地址
     */
    public void setHost(String host) {
        this.host = host;
    }
    
    /**
     * 获取端口号
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 设置端口号
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    /**
     * 获取别名
     */
    public String getAlias() {
        return alias;
    }
    
    /**
     * 设置别名
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }
    
    /**
     * 获取数据模式
     */
    public DataMode getDataMode() {
        return dataMode;
    }
    
    /**
     * 设置数据模式
     */
    public void setDataMode(DataMode dataMode) {
        this.dataMode = dataMode;
    }
    
    /**
     * 获取协议类型
     */
    public ProtocolType getProtocolType() {
        return protocolType;
    }
    
    /**
     * 设置协议类型
     */
    public void setProtocolType(ProtocolType protocolType) {
        this.protocolType = protocolType;
    }
}