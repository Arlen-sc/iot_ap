package com.iot.plc.service;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.enumx.TcpServiceEnum;
import com.iot.plc.model.ConfigItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 初始化配置服务类
 * 集中管理系统的数据库初始化和配置项初始化逻辑
 */
public class InitConfigService {
    private static final Logger logger = LoggerFactory.getLogger(InitConfigService.class);
    private static volatile InitConfigService instance;
    
    /**
     * 私有构造函数
     */
    private InitConfigService() {
        // 私有构造函数，防止外部实例化
    }
    
    /**
     * 获取单例实例
     * @return InitConfigService实例
     */
    public static synchronized InitConfigService getInstance() {
        if (instance == null) {
            instance = new InitConfigService();
        }
        return instance;
    }
    
    /**
     * 执行系统初始化
     * 包括数据库初始化和配置项初始化
     */
    public void initializeSystem() {
        logger.info("开始执行系统初始化...");
        
        // 初始化数据库
        initializeDatabase();
        
        // 初始化配置项
        initializeConfigItems();
        
        logger.info("系统初始化完成");
    }
    
    /**
     * 初始化数据库
     * 创建必要的数据表和索引
     */
    private void initializeDatabase() {
        logger.info("初始化数据库...");
        try {
            DatabaseManager.initializeDatabase();
            logger.info("数据库初始化成功");
        } catch (Exception e) {
            logger.error("数据库初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化配置项
     * 设置系统运行所需的默认配置值
     */
    private void initializeConfigItems() {
        logger.info("初始化配置项...");
        try {
            ConfigService configService = ConfigService.getInstance();
            
            // 初始化必要的配置项
            initializeDefaultConfig(configService);
            
            logger.info("配置项初始化成功");
        } catch (Exception e) {
            logger.error("配置项初始化失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 初始化默认配置项
     * @param configService 配置服务实例
     */
    private void initializeDefaultConfig(ConfigService configService) {
        // 系统基本配置
        addDefaultConfig(configService, "system.version", "1.0.0", "系统版本号", "string", false);
        addDefaultConfig(configService, "system.debug", "true", "调试模式开关", "boolean", false);
        
        // PLC连接配置
        addDefaultConfig(configService, "plc.ip", "127.0.0.1", "PLC设备IP地址", "string", false);
        addDefaultConfig(configService, "plc.port", "502", "PLC设备端口号", "string", false);
        
        // 网络服务配置
        addDefaultConfig(configService, "network.timeout", "30000", "网络连接超时时间(毫秒)", "string", false);
        addDefaultConfig(configService, "network.retry.count", "3", "网络连接重试次数", "string", false);
        
        // 任务调度配置
        addDefaultConfig(configService, "scheduler.pool.size", "5", "任务调度线程池大小", "string", false);
        
        // 日志配置
        addDefaultConfig(configService, "log.level", "INFO", "日志级别", "string", false);
        addDefaultConfig(configService, "log.max.files", "10", "最大日志文件数量", "string", false);
        
        // 网络配置面板相关配置项
        // 使用TcpServiceEnum遍历添加，提高扩展性
        for (TcpServiceEnum serviceEnum : TcpServiceEnum.values()) {
            String code = serviceEnum.getCode().toUpperCase();
            String desc = serviceEnum.getDescription();
            
            // 默认配置
            String defaultHost = "0.0.0.0";
            String defaultPort = "8888";
            
            // 为不同服务类型设置特定的默认值
            if (serviceEnum == TcpServiceEnum.SCANNER) {
                defaultHost = "127.0.0.1";
                defaultPort = "8889";
            } else if (serviceEnum == TcpServiceEnum.PLC) {
                defaultPort = "8083";
            }
            
            // 添加配置项
            addDefaultConfig(configService, code + ".tcp.protocol", "TCP服务端", desc + "协议类型", "string", false);
            addDefaultConfig(configService, code + ".tcp.host", defaultHost, desc + "主机地址", "string", false);
            addDefaultConfig(configService, code + ".tcp.port", defaultPort, desc + "端口号", "string", false);
            addDefaultConfig(configService, code + ".tcp.data_format", "ASCII", desc + "数据格式", "string", false);
        }
    }
    
    /**
     * 添加默认配置项（如果不存在）
     * @param configService 配置服务
     * @param key 配置键
     * @param value 配置值
     * @param description 配置描述
     * @param dataType 数据类型
     * @param required 是否必填
     */
    private void addDefaultConfig(ConfigService configService, String key, String value, String description, String dataType, boolean required) {
        try {
            // 检查配置项是否已存在
            String existingValue = configService.getConfigValueByKey(key);
            if (existingValue == null) {
                // 如果不存在，则添加默认配置
                ConfigItem configItem = new ConfigItem();
                configItem.setConfigKey(key);
                configItem.setConfigValue(value);
                configItem.setDescription(description);
                configItem.setDataType(dataType);
                configItem.setRequired(required);
                
                configService.addConfigItem(configItem);
                logger.info("添加默认配置项: {} = {}", key, value);
            }
        } catch (Exception e) {
            logger.error("添加默认配置项失败 [{}]: {}", key, e.getMessage());
        }
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        logger.info("关闭初始化配置服务");
        // 可以在这里添加清理资源的逻辑
    }
}