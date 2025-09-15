package com.iot.plc.service;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.ConfigItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 配置服务类
 * 负责系统配置项的管理、查询和维护
 */
public class ConfigService {
    private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);
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
            logger.info("加载配置项成功，共加载 {} 个配置项", configItems.size());
            return configItems;
        } catch (SQLException e) {
            logger.error("加载配置项失败: {}", e.getMessage(), e);
            throw new RuntimeException("加载配置项失败", e);
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
                logger.info("获取配置项成功，配置ID: {}", configId);
            } else {
                logger.warn("未找到配置项，配置ID: {}", configId);
            }
            return configItem;
        } catch (SQLException e) {
            logger.error("获取配置项失败，配置ID: {}, 错误: {}", configId, e.getMessage(), e);
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
            DatabaseManager.saveConfigItem(configItem);
            logger.info("配置项保存成功，配置键: {}", configItem.getConfigKey());
        } catch (SQLException e) {
            logger.error("配置项保存失败: {}", e.getMessage(), e);
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
            logger.info("配置项删除成功，配置ID: {}", configId);
        } catch (SQLException e) {
            logger.error("配置项删除失败，配置ID: {}, 错误: {}", configId, e.getMessage(), e);
            throw new RuntimeException("配置项删除失败", e);
        }
    }

    /**
     * 根据配置键获取配置值
     * @param configKey 配置键
     * @return 配置值
     */
    public String getConfigValueByKey(String configKey) {
        try {
            List<ConfigItem> configItems = DatabaseManager.getAllConfigItems();
            for (ConfigItem item : configItems) {
                if (item.getConfigKey().equals(configKey)) {
                    logger.info("获取配置值成功，配置键: {}", configKey);
                    return item.getConfigValue();
                }
            }
            logger.warn("未找到配置项，配置键: {}", configKey);
            return null;
        } catch (SQLException e) {
            logger.error("获取配置值失败，配置键: {}, 错误: {}", configKey, e.getMessage(), e);
            throw new RuntimeException("获取配置值失败", e);
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