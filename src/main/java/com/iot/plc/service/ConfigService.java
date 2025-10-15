package com.iot.plc.service;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.ConfigItem;
import com.iot.plc.logger.Logger;

import java.sql.SQLException;
import java.util.Collections;
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
            // 利用缓存机制获取配置项
            List<ConfigItem> configItems = getConfigItemsWithCache();
            if (configItems == null || configItems.isEmpty()) {
                logger.warn("当前没有配置项");
                return Collections.emptyList();
            }
            logger.debug("加载配置项成功，共加载 " + configItems.size() + " 个配置项");
            return configItems;
        } catch (SQLException e) {
            logger.error("加载配置项失败: " + e.getMessage(), e);
            throw new RuntimeException("加载配置项失败", e);
        }
    }
    /**
     * 根据配置键获取配置值，若未找到则返回默认值
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值，如果未找到则返回默认值
     */
    public String getConfigValueByKeyOrDefault(String configKey, String defaultValue) {
        String value = getConfigValueByKey(configKey);
        return value != null ? value : defaultValue;
    }
    // 配置项缓存，提高性能
    private volatile List<ConfigItem> cachedConfigItems = null;
    private static final long CACHE_EXPIRY_TIME = 5 * 60 * 1000; // 5分钟缓存过期时间
    private volatile long lastCacheUpdateTime = 0;
    
    /**
     * 根据配置键获取配置值
     * @param configKey 配置键
     * @return 配置值，如果未找到则返回null
     * @throws IllegalArgumentException 当配置键为空时抛出
     */
    public String getConfigValueByKey(String configKey) {
        // 参数校验
        if (configKey == null || configKey.trim().isEmpty()) {
            logger.warn("配置键不能为空");
            throw new IllegalArgumentException("配置键不能为空");
        }
        
        try {
            // 获取配置项列表（带缓存机制）
            List<ConfigItem> configItems = getConfigItemsWithCache();
            if (configItems == null || configItems.isEmpty()) {
                logger.warn("当前没有配置项"+configKey);
                return null;
            }
            
            // 查找配置项
            for (ConfigItem item : configItems) {
                if (item != null && configKey.equals(item.getConfigKey())) {
                    logger.debug("获取配置值成功，配置键: " + configKey+",配置值: "+item.getConfigValue());
                    return item.getConfigValue();
                }
            }
            
            logger.warn("未找到配置项，配置键: " + configKey);
            return null;
        } catch (SQLException e) {
            logger.error("获取配置项失败，配置键: " + configKey + ", 错误: " + e.getMessage(), e);
            throw new RuntimeException("获取配置项失败", e);
        }
    }
    
    /**
     * 获取配置项列表（带缓存机制）
     * @return 配置项列表
     * @throws SQLException SQL异常
     */
    private List<ConfigItem> getConfigItemsWithCache() throws SQLException {
        long currentTime = System.currentTimeMillis();
        
        // 检查缓存是否有效
        if (cachedConfigItems == null || 
            currentTime - lastCacheUpdateTime > CACHE_EXPIRY_TIME) {
            synchronized (this) {
                // 双重检查锁定模式，避免多线程问题
                if (cachedConfigItems == null || 
                    currentTime - lastCacheUpdateTime > CACHE_EXPIRY_TIME) {
                    logger.debug("配置项缓存过期或不存在，开始更新缓存");
                    cachedConfigItems = DatabaseManager.getAllConfigItems();
                    lastCacheUpdateTime = currentTime;
                    logger.debug("配置项缓存已更新，共 " + cachedConfigItems.size() + " 个配置项");
                }
            }
        }
        
        return cachedConfigItems;
    }
    
    /**
     * 刷新配置缓存
     */
    public synchronized void refreshCache() {
        try {
            logger.debug("开始刷新配置缓存");
            cachedConfigItems = DatabaseManager.getAllConfigItems();
            lastCacheUpdateTime = System.currentTimeMillis();
            logger.info("配置缓存已手动刷新，共 " + cachedConfigItems.size() + " 个配置项");
            logger.debug("配置缓存刷新完成");
        } catch (SQLException e) {
            logger.error("刷新配置缓存失败: " + e.getMessage(), e);
            // 抛出异常以便调用者知道刷新失败
            throw new RuntimeException("配置缓存刷新失败", e);
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
                logger.debug("获取配置项成功，配置ID: " + configId);
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
     * 添加一个只新增不更新的配置项
     * @param configItem 配置项对象
     * @return 是否成功添加（如果配置项已存在则返回false）
     */
    public boolean addConfigItem(ConfigItem configItem) {
        try {
            validateConfigItem(configItem);
            
            // 查找是否已存在相同config_key的配置项
            ConfigItem existingItem = findConfigItemByKey(configItem.getConfigKey());
            
            // 只添加不存在的配置项，不更新已存在的
            if (existingItem == null) {
                logger.debug("未找到已存在的配置项，将执行插入操作，配置键: " + configItem.getConfigKey());
                DatabaseManager.saveConfigItem(configItem);
                logger.info("配置项添加成功，配置键: " + configItem.getConfigKey());
                // 刷新缓存
                refreshCache();
                return true;
            } else {
                logger.debug("配置项已存在，不执行操作，配置键: " + configItem.getConfigKey() + ", ID: " + existingItem.getId());
                return false;
            }
        } catch (SQLException e) {
            logger.error("配置项添加失败: " + e.getMessage(), e);
            throw new RuntimeException("配置项添加失败", e);
        }
    }
    
    /**
     * 重载方法：直接通过键和值添加配置项
     * @param key 配置项键名
     * @param value 配置项值
     * @return 是否成功添加（如果配置项已存在则返回false）
     */
    public boolean addConfigItem(String key, String value) {
        return addConfigItem(key, value, "String");
    }

    /**
     * 重载方法：直接通过键、值和数据类型添加配置项
     * @param key 配置项键名
     * @param value 配置项值
     * @param dataType 数据类型
     * @return 是否成功添加（如果配置项已存在则返回false）
     */
    public boolean addConfigItem(String key, String value, String dataType) {
        ConfigItem configItem = new ConfigItem();
        configItem.setConfigKey(key);
        configItem.setConfigValue(value);
        configItem.setDataType(dataType);
        return this.addConfigItem(configItem);
    }
    
    /**
     * 根据配置键查找配置项
     * @param configKey 配置键
     * @return 找到的配置项，如果未找到则返回null
     */
    private ConfigItem findConfigItemByKey(String configKey) {
        try {
            // 利用缓存机制查找配置项，避免直接查询数据库
            List<ConfigItem> allItems = getConfigItemsWithCache();
            for (ConfigItem item : allItems) {
                if (item != null && configKey.equals(item.getConfigKey())) {
                    return item;
                }
            }
        } catch (SQLException e) {
            logger.error("查询配置项时发生异常: " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * 保存配置项（存在则更新，不存在则新增）
     * @param configItem 配置项对象
     */
    public void saveConfigItem(ConfigItem configItem) {
        try {
            validateConfigItem(configItem);
            
            // 查找是否已存在相同config_key的配置项
            ConfigItem existingItem = findConfigItemByKey(configItem.getConfigKey());
            
            // 如果找到已存在的配置项，则更新它
            if (existingItem != null) {
                configItem.setId(existingItem.getId());
                logger.debug("找到已存在的配置项，将执行更新操作，配置键: " + configItem.getConfigKey() + ", ID: " + existingItem.getId());
            } else {
                logger.debug("未找到已存在的配置项，将执行插入操作，配置键: " + configItem.getConfigKey());
            }
            
            DatabaseManager.saveConfigItem(configItem);
            logger.info("配置项保存成功，配置键: " + configItem.getConfigKey());
            // 保存后刷新缓存
            refreshCache();
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
            // 先检查配置项是否存在
            ConfigItem existingItem = DatabaseManager.getConfigItemById(configId);
            if (existingItem != null) {
                DatabaseManager.deleteConfigItem(configId);
                logger.info("配置项删除成功，配置ID: " + configId + ", 配置键: " + existingItem.getConfigKey());
                // 删除后刷新缓存
                refreshCache();
            } else {
                logger.warn("配置项不存在，无法删除，配置ID: " + configId);
            }
        } catch (SQLException e) {
            logger.error("配置项删除失败，配置ID: " + configId + ", 错误: " + e.getMessage(), e);
            throw new RuntimeException("配置项删除失败", e);
        }
    }

    /**
     * 验证配置项
     * @param configItem 配置项对象
     * @throws IllegalArgumentException 当必填字段为空时抛出
     * @throws NullPointerException 当配置项对象为null时抛出
     */
    private void validateConfigItem(ConfigItem configItem) {
        // 验证对象不为null
        if (configItem == null) {
            throw new NullPointerException("配置项对象不能为空");
        }
        // 验证配置键不为空
        if (configItem.getConfigKey() == null || configItem.getConfigKey().trim().isEmpty()) {
            throw new IllegalArgumentException("配置键为必填项");
        }
        // 验证数据类型不为空
        if (configItem.getDataType() == null || configItem.getDataType().trim().isEmpty()) {
            throw new IllegalArgumentException("数据类型为必填项");
        }
    }

    /**
     * 异步执行任务
     * @param runnable 要执行的任务
     */
    public void executeAsync(Runnable runnable) {
        if (runnable == null) {
            logger.warn("尝试异步执行null任务，忽略操作");
            return;
        }
        executorService.submit(runnable);
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        try {
            executorService.shutdown();
            logger.info("ConfigService 已关闭");
        } catch (Exception e) {
            logger.error("关闭ConfigService时发生异常: " + e.getMessage(), e);
        }
    }

  
    /**
     * 重载方法：通过键和值保存配置项（存在则更新，不存在则新增）
     * @param key 配置项键
     * @param value 配置项值
     */
    public void saveConfigItem(String key, String value) {
        saveConfigItem(key, value, "String");
    }

    /**
     * 重载方法：通过键、值和数据类型保存配置项（存在则更新，不存在则新增）
     * @param key 配置项键
     * @param value 配置项值
     * @param dataType 数据类型
     */
    public void saveConfigItem(String key, String value, String dataType) {
        ConfigItem configItem = new ConfigItem();
        configItem.setConfigKey(key);
        configItem.setConfigValue(value);
        configItem.setDataType(dataType);
        this.saveConfigItem(configItem);
    }
}