package com.iot.plc.logger;

import com.iot.plc.ui.JavaFXLogPanel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringJoiner;

public class Logger {
    private static volatile Logger instance;
    private JavaFXLogPanel javaFXLogPanel; // JavaFX版本的日志面板
    private final DateTimeFormatter formatter; // 日期格式化器
    // 使用自定义日志记录方式，不再依赖java.util.logging.Logger
    
    private Logger() {
        // 私有构造函数，实现单例模式
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 文件日志记录现在通过LogService实现，无需在此初始化
    }
    
    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }
    
    // 支持JavaFX版本的JavaFXLogPanel
    public void setLogPanel(JavaFXLogPanel panel) {
        this.javaFXLogPanel = panel;
    }
    
    public void info(String message) {
        String timestamp = getCurrentTimestamp();
        String logMessage = timestamp + " [INFO] " + message;
        System.out.println(logMessage);
        updateJavaFXLogPanel(logMessage);
        // 文件日志记录通过LogService实现
    }
    
    public void error(String message) {
        String timestamp = getCurrentTimestamp();
        String logMessage = timestamp + " [ERROR] " + message;
        System.err.println(logMessage);
        updateJavaFXLogPanel(logMessage);
        // 文件日志记录通过LogService实现
    }
    
    /**
     * 记录错误信息并附带异常堆栈
     * @param message 错误消息
     * @param throwable 异常对象
     */
    public void error(String message, Throwable throwable) {
        String timestamp = getCurrentTimestamp();
        String errorMessage = message + "\n" + getStackTrace(throwable);
        String logMessage = timestamp + " [ERROR] " + errorMessage;
        System.err.println(logMessage);
        updateJavaFXLogPanel(logMessage);
        // 文件日志记录通过LogService实现
    }
    
    public void debug(String message) {
        String timestamp = getCurrentTimestamp();
        String logMessage = timestamp + " [DEBUG] " + message;
        System.out.println(logMessage);
        updateJavaFXLogPanel(logMessage);
        // 文件日志记录通过LogService实现
    }
    
    public void warn(String message) {
        String timestamp = getCurrentTimestamp();
        String logMessage = timestamp + " [WARN] " + message;
        System.out.println(logMessage);
        updateJavaFXLogPanel(logMessage);
        // 文件日志记录通过LogService实现
    }
    
    // 更新JavaFX版本的日志面板
    private void updateJavaFXLogPanel(String message) {
        if (javaFXLogPanel != null) {
            javafx.application.Platform.runLater(() -> {
                javaFXLogPanel.appendLog(message);
            });
        }
    }
    
    // 获取当前时间戳
    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(formatter);
    }
    
    /**
     * 获取异常堆栈信息
     * @param throwable 异常对象
     * @return 格式化的堆栈信息字符串
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("Exception: " + throwable.toString());
        
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        for (StackTraceElement element : stackTrace) {
            joiner.add("    at " + element.toString());
        }
        
        return joiner.toString();
    }
}