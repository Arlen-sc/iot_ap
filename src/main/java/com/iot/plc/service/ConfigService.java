package com.iot.plc.service;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.ConfigItem;
import com.iot.plc.logger.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 配置服务类
 * 负责系统配置项的管理、查询和维护
 */
public class ConfigService {
    private static final Logger logger = Logger.getInstance();
    private static ConfigService instance;
    private final ExecutorService executorService;

    /**
     * 单例模式私有构造函数
     */
    private ConfigService() {
        this.executorService = Executors.newSingleThreadExecutor();
        logger.info("ConfigService 初始化成功");
    }

    /**
     * 获取单例实例
     * @return ConfigService实例
     */
    public static synchronized ConfigService getInstance() {
        if (instance == null) {
            instance = new ConfigService();
        }
        return instance;
    }

    /**
     * 加载所有配置项
     * @return 配置项列表
     */
    public List<ConfigItem> getAllConfigItems() {
        try {
            List<ConfigItem> configItems = DatabaseManager.getAllConfigItems();
            logger.info("加载配置项成功，共加载 " + configItems.size() + " 个配置项");
            return configItems;
        } catch (SQLException e) {
            logger.error("加载配置项失败: " + e.getMessage(), e);
            throw new RuntimeException("加载配置项失败", e);
        }
    }
    
    /**
     * 根据配置键获取配置值
     * @param configKey 配置键
     * @return 配置值，如果未找到则返回null
     */
    public String getConfigValueByKey(String configKey) {
        try {
            List<ConfigItem> configItems = DatabaseManager.getAllConfigItems();
            for (ConfigItem item : configItems) {
                if (item.getConfigKey().equals(configKey)) {
                    return item.getConfigValue();
                }
            }
            logger.warn("未找到配置项，配置键: " + configKey);
            return null;
        } catch (SQLException e) {
            logger.error("获取配置项失败，配置键: " + configKey + ", 错误: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * 根据ID获取配置项
     * @param configId 配置项ID
     * @return 配置项对象
     */
    public ConfigItem getConfigItemById(int configId) {
        try {
            ConfigItem configItem = DatabaseManager.getConfigItemById(configId);
            if (configItem != null) {
                logger.info("获取配置项成功，配置ID: " + configId);
            } else {
                logger.warn("未找到配置项，配置ID: " + configId);
            }
            return configItem;
        } catch (SQLException e) {
            logger.error("获取配置项失败，配置ID: " + configId + ", 错误: " + e.getMessage(), e);
            throw new RuntimeException("获取配置项失败", e);
        }
    }

    /**
     * 保存配置项
     * @param configItem 配置项对象
     */
    public void saveConfigItem(ConfigItem configItem) {
        try {
            validateConfigItem(configItem);
            
            // 先尝试查找是否已存在相同config_key的配置项
            ConfigItem existingItem = null;
            try {
                List<ConfigItem> allItems = DatabaseManager.getAllConfigItems();
                for (ConfigItem item : allItems) {
                    if (item.getConfigKey().equals(configItem.getConfigKey())) {
                        existingItem = item;
                        break;
                    }
                }
            } catch (SQLException e) {
                logger.warn("查询配置项时发生异常: " + e.getMessage());
            }
            
            // 如果找到已存在的配置项，则更新它
            if (existingItem != null) {
                configItem.setId(existingItem.getId());
                logger.debug("找到已存在的配置项，将执行更新操作，配置键: " + configItem.getConfigKey() + ", ID: " + existingItem.getId());
            } else {
                logger.debug("未找到已存在的配置项，将执行插入操作，配置键: " + configItem.getConfigKey());
            }
            
            DatabaseManager.saveConfigItem(configItem);
            logger.info("配置项保存成功，配置键: " + configItem.getConfigKey());
        } catch (SQLException e) {
            logger.error("配置项保存失败: " + e.getMessage(), e);
            throw new RuntimeException("配置项保存失败", e);
        }
    }

    /**
     * 删除配置项
     * @param configId 配置项ID
     */
    public void deleteConfigItem(int configId) {
        try {
            DatabaseManager.deleteConfigItem(configId);
            logger.info("配置项删除成功，配置ID: " + configId);
        } catch (SQLException e) {
            logger.error("配置项删除失败，配置ID: " + configId + ", 错误: " + e.getMessage(), e);
            throw new RuntimeException("配置项删除失败", e);
        }
    }

    /**
     * 验证配置项
     * @param configItem 配置项对象
     * @throws IllegalArgumentException 当必填字段为空时抛出
     */
    
    
    private void validateConfigItem(ConfigItem configItem) {
        if (configItem.getConfigKey() == null || configItem.getConfigKey().trim().isEmpty()) {
            throw new IllegalArgumentException("配置项为必填项");
        }
        if (configItem.getConfigValue() == null || configItem.getConfigValue().trim().isEmpty()) {
            throw new IllegalArgumentException("配置值为必填项");
        }
        if (configItem.getDataType() == null || configItem.getDataType().trim().isEmpty()) {
            throw new IllegalArgumentException("数据类型为必填项");
        }
    }

    /**
     * 异步执行任务
     * @param runnable 要执行的任务
     */
    public void executeAsync(Runnable runnable) {
        executorService.submit(runnable);
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        executorService.shutdown();
        logger.info("ConfigService 已关闭");
    }

    public void saveConfigItem(String logRetentionPeriodKey, String defaultDays, String string) {
        ConfigItem configItem = new ConfigItem();
        configItem.setConfigKey(logRetentionPeriodKey);
        configItem.setConfigValue(defaultDays);
        // 假设第三个参数是数据类型，根据实际情况调整
        configItem.setDataType(string);
        this.saveConfigItem(configItem);
    }
}