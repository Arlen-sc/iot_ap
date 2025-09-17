package com.iot.plc.logger;

/**
 * LoggerFactory类已废弃，不再使用java.util.logging
 * 日志功能现在通过Logger类和LogService实现
 */
@Deprecated
public class LoggerFactory {
    /**
     * 已废弃的方法，不再返回java.util.logging.Logger实例
     * @param name Logger的名称
     * @return null
     */
    public static Object getLogger(String name) {
        System.err.println("LoggerFactory.getLogger() is deprecated. Use Logger.getInstance() instead.");
        return null;
    }
}