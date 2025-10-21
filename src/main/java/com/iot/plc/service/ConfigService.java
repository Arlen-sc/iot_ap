package com.iot.plc.service;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.ConfigItem;
import com.iot.plc.logger.Logger;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 配置管理服务类
 * 负责系统配置项的CRUD操作和缓存管理
 */
public class ConfigService {
    private static final Logger logger = Logger.getInstance();
    private static volatile ConfigService instance;
    
    // 配置项缓存
    private final Map<String, ConfigItem> configItemCache = new ConcurrentHashMap<>();
    private long lastCacheUpdateTime = 0;
    private static final long CACHE_EXPIRATION_MS = 5 * 60 * 1000; // 5分钟缓存过期
    
    // 线程池用于缓存刷新
    private final ScheduledExecutorService scheduledExecutorService;
    
    // 私有构造函数
    private ConfigService() {
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        // 启动定期刷新缓存的任务
        scheduledExecutorService.scheduleAtFixedRate(
                this::refreshCache,
                CACHE_EXPIRATION_MS,
                CACHE_EXPIRATION_MS,
                TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized ConfigService getInstance() {
        if (instance == null) {
            instance = new ConfigService();
        }
        return instance;
    }
    
    /**
     * 获取配置项缓存（如果缓存过期则重新加载）
     */
    private Map<String, ConfigItem> getConfigItemsWithCache() {
        long currentTime = System.currentTimeMillis();
        
        // 双重检查锁定模式
        if (configItemCache.isEmpty() || (currentTime - lastCacheUpdateTime) > CACHE_EXPIRATION_MS) {
            synchronized (this) {
                if (configItemCache.isEmpty() || (currentTime - lastCacheUpdateTime) > CACHE_EXPIRATION_MS) {
                    try {
                        List<ConfigItem> configItems = DatabaseManager.getAllConfigItems();
                        configItemCache.clear();
                        
                        for (ConfigItem item : configItems) {
                            configItemCache.put(item.getConfigKey(), item);
                        }
                        
                        lastCacheUpdateTime = currentTime;
                        logger.info("配置缓存已更新，加载了 " + configItems.size() + " 个配置项");
                    } catch (SQLException e) {
                        logger.error("更新配置缓存失败: " + e.getMessage(), e);
                    }
                }
            }
        }
        
        return configItemCache;
    }
    
    /**
     * 刷新缓存
     */
    public void refreshCache() {
        try {
            List<ConfigItem> configItems = DatabaseManager.getAllConfigItems();
            configItemCache.clear();
            
            for (ConfigItem item : configItems) {
                configItemCache.put(item.getConfigKey(), item);
            }
            
            lastCacheUpdateTime = System.currentTimeMillis();
            logger.info("手动刷新配置缓存成功，共加载 " + configItems.size() + " 个配置项");
        } catch (SQLException e) {
            logger.error("手动刷新配置缓存失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据ID获取配置项
     */
    public ConfigItem getConfigItemById(int id) {
        try {
            return DatabaseManager.getConfigItemById(id);
        } catch (SQLException e) {
            logger.error("根据ID获取配置项失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 根据键获取配置值
     */
    public String getConfigValueByKey(String key) {
        ConfigItem item = getConfigItemsWithCache().get(key);
        return item != null ? item.getConfigValue() : null;
    }
    
    /**
     * 根据键获取配置值，如果不存在则返回默认值
     */
    public String getConfigValueByKeyOrDefault(String key, String defaultValue) {
        String value = getConfigValueByKey(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * 添加配置项
     */
    public boolean addConfigItem(ConfigItem configItem) {
        try {
            // 检查键是否已存在
            if (getConfigItemsWithCache().containsKey(configItem.getConfigKey())) {
                logger.warn("配置项键已存在: " + configItem.getConfigKey());
                return false;
            }
            
            DatabaseManager.saveConfigItem(configItem);
            // 刷新缓存
            refreshCache();
            return true;
        } catch (SQLException e) {
            logger.error("添加配置项失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 保存配置项（新增或更新）
     */
    public boolean saveConfigItem(ConfigItem configItem) {
        try {
            ConfigItem existingItem = getConfigItemsWithCache().get(configItem.getConfigKey());
            
            if (existingItem != null) {
                // 更新现有配置项
                configItem.setId(existingItem.getId());
            }
            
            // 使用saveConfigItem方法处理新增和更新操作
            DatabaseManager.saveConfigItem(configItem);
            // 刷新缓存
            refreshCache();
            return true;
        } catch (SQLException e) {
            logger.error("保存配置项失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 保存配置项（带描述和数据类型）
     */
    public boolean saveConfigItem(String configKey, String configValue, String description, String dataType) {
        ConfigItem configItem = new ConfigItem(
            configKey,
            configValue,
            description,
            dataType,
            false // 默认为非必填
        );
        return saveConfigItem(configItem);
    }
    
    /**
     * 保存配置项（带描述、数据类型和必填标志）
     */
    public boolean saveConfigItem(String configKey, String configValue, String description, String dataType, boolean required) {
        ConfigItem configItem = new ConfigItem(
            configKey,
            configValue,
            description,
            dataType,
            required
        );
        return saveConfigItem(configItem);
    }
    
    /**
     * 通用配置保存方法（供UI层调用）
     */
    public boolean saveConfig(String key, String value, String description, String dataType) {
        try {
            return saveConfigItem(key, value, description, dataType);
        } catch (Exception e) {
            logger.error("保存配置时发生错误: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 删除配置项
     */
    public boolean deleteConfigItem(int id) {
        try {
            boolean result = DatabaseManager.deleteConfigItem(id);
            if (result) {
                refreshCache();
            }
            return result;
        } catch (Exception e) {
            logger.error("删除配置项失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
            try {
                if (!scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}