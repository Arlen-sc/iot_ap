package com.iot.plc.ui;

import javax.swing.*;
import java.awt.*;

public class LogPanel extends JPanel {
    private JTextArea logArea;
    private JButton toggleButton;
    private boolean isLoggingEnabled = true;

    public LogPanel() {
        setLayout(new BorderLayout());
        
        // 日志显示区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);
        
        // 控制按钮
        toggleButton = new JButton("关闭日志显示");
        toggleButton.addActionListener(e -> toggleLogging());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(toggleButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    public void appendLog(String message) {
        if (isLoggingEnabled) {
            logArea.append(message + "\n");
        }
    }
    
    private void toggleLogging() {
        isLoggingEnabled = !isLoggingEnabled;
        toggleButton.setText(isLoggingEnabled ? "关闭日志显示" : "开启日志显示");
    }
}