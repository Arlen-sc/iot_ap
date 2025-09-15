package com.iot.plc.logger;

import com.iot.plc.ui.JavaFXLogPanel;

public class Logger {
    private static Logger instance;
    private JavaFXLogPanel javaFXLogPanel; // JavaFX版本的日志面板
    
    private Logger() {
        // 私有构造函数，实现单例模式
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
        String logMessage = "[INFO] " + message;
        System.out.println(logMessage);
        updateJavaFXLogPanel(logMessage);
    }
    
    public void error(String message) {
        String logMessage = "[ERROR] " + message;
        System.err.println(logMessage);
        updateJavaFXLogPanel(logMessage);
    }
    
    public void debug(String message) {
        String logMessage = "[DEBUG] " + message;
        System.out.println(logMessage);
        updateJavaFXLogPanel(logMessage);
    }
    
    public void warn(String message) {
        String logMessage = "[WARN] " + message;
        System.out.println(logMessage);
        updateJavaFXLogPanel(logMessage);
    }
    
    // 更新JavaFX版本的日志面板
    private void updateJavaFXLogPanel(String message) {
        if (javaFXLogPanel != null) {
            javafx.application.Platform.runLater(() -> {
                javaFXLogPanel.appendLog(message);
            });
        }
    }
}