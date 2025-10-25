package com.iot.plc.ui.logs;

import com.iot.plc.ui.base.JavaFXBasePanel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.Handler;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.ErrorManager;
import java.io.IOException;

/**
 * 日志面板
 * 用于系统日志展示与控制
 */
public class JavaFXLogPanel extends JavaFXBasePanel {
    private TextArea logTextArea;
    private boolean isLogEnabled = false;
    private LogHandler logHandler;
    private Logger logger;

    public JavaFXLogPanel() {
        initComponents();
        loadData();
        initLogger();
    }

    @Override
    protected void initComponents() {
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // 创建标题
        Label titleLabel = new Label("系统日志");
        HBox titleBox = new HBox(titleLabel);
        titleBox.setPadding(new Insets(5, 0, 10, 0));

        // 创建日志显示区域
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");

        // 创建滚动面板
        ScrollPane scrollPane = new ScrollPane(logTextArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        // 创建控制按钮
        Button enableButton = new Button("开启日志");
        Button disableButton = new Button("关闭日志");
        Button clearButton = new Button("清空日志");

        enableButton.setOnAction(e -> enableLog());
        disableButton.setOnAction(e -> disableLog());
        clearButton.setOnAction(e -> clearLog());

        // 创建按钮面板
        HBox buttonPanel = new HBox(10);
        buttonPanel.getChildren().addAll(enableButton, disableButton, clearButton);

        // 添加所有组件到主面板
        this.getChildren().addAll(titleBox, scrollPane, buttonPanel);

        // 设置布局权重
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
    }

    @Override
    protected void loadData() {
        // 初始化时可以加载一些欢迎信息
        // 注意：因为默认关闭日志，所以这些信息不会显示在日志区域
    }

    @Override
    public void refresh() {
        // 刷新日志面板
        appendLog("日志面板已刷新");
    }

    /**
     * 初始化日志记录器
     */
    private void initLogger() {
        logger = Logger.getLogger("com.iot.plc");
        logHandler = new LogHandler();
        logHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(logHandler);
        logger.setLevel(Level.ALL);
        
        // 移除控制台处理器，避免日志重复输出
        for (Handler handler : logger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                logger.removeHandler(handler);
            }
        }
    }

    /**
     * 追加日志信息
     */
    public void appendLog(String message) {
        if (!isLogEnabled) {
            return;
        }

        Platform.runLater(() -> {
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            logTextArea.appendText(timestamp + " - " + message + "\n");
            // 自动滚动到底部
            logTextArea.positionCaret(logTextArea.getLength());
        });
    }

    /**
     * 开启日志
     */
    private void enableLog() {
        isLogEnabled = true;
        appendLog("日志已开启");
    }

    /**
     * 关闭日志
     */
    private void disableLog() {
        isLogEnabled = false;
        appendLog("日志已关闭");
    }

    /**
     * 清空日志
     */
    private void clearLog() {
        Platform.runLater(() -> {
            logTextArea.clear();
            appendLog("日志已清空");
        });
    }

    /**
     * 自定义日志处理器
     */
    private class LogHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            if (!isLogEnabled || !isLoggable(record)) {
                return;
            }

            try {
                String formattedMessage = getFormatter().format(record);
                appendLog(formattedMessage.trim());
            } catch (Exception e) {
                reportError(null, e, ErrorManager.FORMAT_FAILURE);
            }
        }

        @Override
        public void flush() {
            // 不需要实现
        }

        @Override
        public void close() throws SecurityException {
            // 不需要实现
        }
    }

    /**
     * 获取日志文本区域
     */
    public TextArea getLogTextArea() {
        return logTextArea;
    }

    /**
     * 设置日志启用状态
     */
    public void setLogEnabled(boolean enabled) {
        isLogEnabled = enabled;
        appendLog("日志状态设置为: " + (enabled ? "开启" : "关闭"));
    }

    /**
     * 获取日志启用状态
     */
    public boolean isLogEnabled() {
        return isLogEnabled;
    }
}