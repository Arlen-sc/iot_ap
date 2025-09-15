package com.iot.plc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 日志服务类
 * 负责系统日志的记录、存储和分发
 */
public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);
    private static LogService instance;
    private final CopyOnWriteArrayList<LogListener> logListeners;
    private boolean isLoggingEnabled;
    private final SimpleDateFormat dateFormat;

    /**
     * 日志监听器接口
     */
    public interface LogListener {
        void onLogAdded(String message);
    }

    /**
     * 单例模式私有构造函数
     */
    private LogService() {
        this.logListeners = new CopyOnWriteArrayList<>();
        this.isLoggingEnabled = true;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        logger.info("LogService 初始化成功");
    }

    /**
     * 获取单例实例
     * @return LogService实例
     */
    public static synchronized LogService getInstance() {
        if (instance == null) {
            instance = new LogService();
        }
        return instance;
    }

    /**
     * 记录信息日志
     * @param message 日志消息
     */
    public void info(String message) {
        String formattedMessage = formatLog("INFO", message);
        logger.info(formattedMessage);
        notifyLogListeners(formattedMessage);
    }

    /**
     * 记录警告日志
     * @param message 日志消息
     */
    public void warn(String message) {
        String formattedMessage = formatLog("WARN", message);
        logger.warn(formattedMessage);
        notifyLogListeners(formattedMessage);
    }

    /**
     * 记录错误日志
     * @param message 日志消息
     */
    public void error(String message) {
        String formattedMessage = formatLog("ERROR", message);
        logger.error(formattedMessage);
        notifyLogListeners(formattedMessage);
    }

    /**
     * 记录调试日志
     * @param message 日志消息
     */
    public void debug(String message) {
        String formattedMessage = formatLog("DEBUG", message);
        logger.debug(formattedMessage);
        notifyLogListeners(formattedMessage);
    }

    /**
     * 记录异常信息
     * @param message 错误消息
     * @param throwable 异常对象
     */
    public void error(String message, Throwable throwable) {
        String formattedMessage = formatLog("ERROR", message);
        logger.error(formattedMessage, throwable);
        notifyLogListeners(formattedMessage);
        // 可以选择是否将异常堆栈也发送给监听器
    }

    /**
     * 格式化日志消息
     * @param level 日志级别
     * @param message 日志内容
     * @return 格式化后的日志字符串
     */
    private String formatLog(String level, String message) {
        return String.format("[%s] [%s] %s", dateFormat.format(new Date()), level, message);
    }

    /**
     * 添加日志监听器
     * @param listener 日志监听器
     */
    public void addLogListener(LogListener listener) {
        if (listener != null && !logListeners.contains(listener)) {
            logListeners.add(listener);
            logger.info("已添加日志监听器");
        }
    }

    /**
     * 移除日志监听器
     * @param listener 日志监听器
     */
    public void removeLogListener(LogListener listener) {
        if (listener != null) {
            logListeners.remove(listener);
            logger.info("已移除日志监听器");
        }
    }

    /**
     * 通知所有日志监听器
     * @param message 日志消息
     */
    private void notifyLogListeners(String message) {
        if (isLoggingEnabled) {
            for (LogListener listener : logListeners) {
                try {
                    listener.onLogAdded(message);
                } catch (Exception e) {
                    logger.error("通知日志监听器失败", e);
                }
            }
        }
    }

    /**
     * 启用或禁用日志显示
     * @param enabled 是否启用
     */
    public void setLoggingEnabled(boolean enabled) {
        this.isLoggingEnabled = enabled;
        logger.info("日志显示已{}", enabled ? "启用" : "禁用");
    }

    /**
     * 检查日志显示是否启用
     * @return 是否启用
     */
    public boolean isLoggingEnabled() {
        return isLoggingEnabled;
    }

    /**
     * 清空所有日志监听器
     */
    public void clearLogListeners() {
        logListeners.clear();
        logger.info("已清空所有日志监听器");
    }
}