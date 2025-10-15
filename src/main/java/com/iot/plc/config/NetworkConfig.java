package com.iot.plc.config;

import com.iot.plc.model.DataMode;
import com.iot.plc.enumx.ProtocolType;
import com.iot.plc.enumx.ServiceType;

/**
 * 网络服务配置类
 * 存储网络服务的配置参数
 */
public class NetworkConfig {
    private ServiceType serviceType;
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
    public NetworkConfig(ServiceType serviceType, String host, int port) {
        this.serviceType = serviceType;
        this.host = host;
        this.port = port;
        this.dataMode = DataMode.ASCII; // 默认使用ASCII模式
        this.protocolType = ProtocolType.TCP_SERVER; // 默认使用TCP服务端模式
    }
    
    /**
     * 获取服务类型
     */
    public ServiceType getServiceType() {
        return serviceType;
    }
    
    /**
     * 设置服务类型
     */
    public void setServiceType(ServiceType serviceType) {
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