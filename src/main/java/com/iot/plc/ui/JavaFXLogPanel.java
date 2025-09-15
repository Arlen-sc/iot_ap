package com.iot.plc.ui;

import com.iot.plc.logger.Logger;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class JavaFXLogPanel extends BorderPane {
    private TextArea logArea;
    private Button toggleButton;
    private boolean isLoggingEnabled = true;

    public JavaFXLogPanel() {
        // 日志显示区域
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setWrapText(true);
        ScrollPane scrollPane = new ScrollPane(logArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        setCenter(scrollPane);

        // 控制按钮
        toggleButton = new Button("关闭日志显示");
        toggleButton.setOnAction(e -> toggleLogging());

        HBox buttonBox = new HBox(toggleButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setStyle("-fx-padding: 10px;");
        setBottom(buttonBox);

        // 确保Logger能够正确使用这个面板
        Logger.getInstance().setLogPanel(this);
    }

    public void appendLog(String message) {
        if (isLoggingEnabled) {
            Platform.runLater(() -> {
                logArea.appendText(message + "\n");
                // 自动滚动到底部
                logArea.setScrollTop(Double.MAX_VALUE);
            });
        }
    }

    private void toggleLogging() {
        isLoggingEnabled = !isLoggingEnabled;
        toggleButton.setText(isLoggingEnabled ? "关闭日志显示" : "开启日志显示");
    }
}