package com.iot.plc.ui.config;

import com.iot.plc.config.NetworkConfig;
import com.iot.plc.model.ConfigItem;
import com.iot.plc.service.ConfigService;
import com.iot.plc.ui.base.JavaFXBasePanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * 扫码机网络配置面板
 */
public class ScannerConfigPanel extends JavaFXBasePanel {
    private Stage primaryStage;
    
    private ComboBox<String> protocolComboBox;
    private TextField hostTextField;
    private TextField portTextField;
    private ComboBox<String> dataFormatComboBox;
    
    public ScannerConfigPanel(Stage stage) {
        this.primaryStage = stage;
        initComponents();
        loadData();
    }
    
    @Override
    protected void initComponents() {
        this.setPadding(new Insets(20));
        this.setSpacing(20);
        
        // 创建标题
        Label titleLabel = new Label("扫码机配置");
        titleLabel.setFont(Font.font(18));
        titleLabel.setAlignment(Pos.CENTER_LEFT);
        
        // 创建配置表单
        GridPane configGrid = new GridPane();
        configGrid.setHgap(15);
        configGrid.setVgap(20);
        configGrid.setPadding(new Insets(10));
        
        // 协议选择
        Label protocolLabel = new Label("协议类型：");
        protocolLabel.setFont(Font.font(14));
        protocolComboBox = new ComboBox<>();
        protocolComboBox.getItems().addAll("TCP服务端", "TCP客户端", "UDP");
        protocolComboBox.setPrefWidth(200);
        protocolComboBox.setPrefHeight(30);
        configGrid.add(protocolLabel, 0, 0);
        configGrid.add(protocolComboBox, 1, 0);
        
        // 主机地址
        Label hostLabel = new Label("主机地址：");
        hostLabel.setFont(Font.font(14));
        hostTextField = new TextField();
        hostTextField.setPromptText("请输入主机地址或IP");
        hostTextField.setPrefWidth(200);
        hostTextField.setPrefHeight(30);
        configGrid.add(hostLabel, 0, 1);
        configGrid.add(hostTextField, 1, 1);
        
        // 端口号
        Label portLabel = new Label("端口号：");
        portLabel.setFont(Font.font(14));
        portTextField = new TextField();
        portTextField.setPromptText("请输入端口号（1-65535）");
        portTextField.setPrefWidth(200);
        portTextField.setPrefHeight(30);
        configGrid.add(portLabel, 0, 2);
        configGrid.add(portTextField, 1, 2);
        
        // 数据格式
        Label dataFormatLabel = new Label("数据格式：");
        dataFormatLabel.setFont(Font.font(14));
        dataFormatComboBox = new ComboBox<>();
        dataFormatComboBox.getItems().addAll("ASCII", "HEX");
        dataFormatComboBox.setPrefWidth(200);
        dataFormatComboBox.setPrefHeight(30);
        configGrid.add(dataFormatLabel, 0, 3);
        configGrid.add(dataFormatComboBox, 1, 3);
        
        // 创建按钮面板
        HBox buttonPanel = new HBox(20);
        buttonPanel.setAlignment(Pos.CENTER);
        Button saveButton = new Button("保存");
        Button cancelButton = new Button("取消");
        saveButton.setPrefWidth(100);
        saveButton.setPrefHeight(35);
        cancelButton.setPrefWidth(100);
        cancelButton.setPrefHeight(35);
        
        saveButton.setOnAction(e -> saveConfig());
        cancelButton.setOnAction(e -> cancel());
        
        buttonPanel.getChildren().addAll(saveButton, cancelButton);
        
        // 添加所有组件到主面板
        this.getChildren().addAll(titleLabel, configGrid, buttonPanel);
        
        // 设置组件样式
        configGrid.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 4px; -fx-padding: 20px;");
        saveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
    }
    
    @Override
    protected void loadData() {
        // 加载扫码机配置
        String prefix = "SCANNER.tcp";
        
        String protocol = ConfigService.getInstance().getConfigValueByKeyOrDefault(prefix + ".protocol", "TCP服务端");
        protocolComboBox.setValue(protocol);
        
        String host = ConfigService.getInstance().getConfigValueByKeyOrDefault(prefix + ".host", "127.0.0.1");
        hostTextField.setText(host);
        
        String port = ConfigService.getInstance().getConfigValueByKeyOrDefault(prefix + ".port", "8889");
        portTextField.setText(port);
        
        String dataFormat = ConfigService.getInstance().getConfigValueByKeyOrDefault(prefix + ".data_format", "ASCII");
        dataFormatComboBox.setValue(dataFormat);
    }
    
    @Override
    public void refresh() {
        loadData();
    }
    
    /**
     * 保存配置
     */
    private void saveConfig() {
        // 验证输入
        if (!validateInput()) {
            return;
        }
        
        String protocol = protocolComboBox.getValue();
        String host = hostTextField.getText().trim();
        String port = portTextField.getText().trim();
        String dataFormat = dataFormatComboBox.getValue();
        
        try {
            // 创建ConfigItem对象并保存
            // 使用枚举.tcp.参数格式
            String prefix = "SCANNER.tcp";
            
            ConfigItem protocolItem = new ConfigItem();
            protocolItem.setConfigKey(prefix + ".protocol");
            protocolItem.setConfigValue(protocol);
            protocolItem.setConfigDescription("扫码机协议类型");
            protocolItem.setDataType("String");
            protocolItem.setRequired(true);
            ConfigService.getInstance().saveConfigItem(protocolItem);

            ConfigItem hostItem = new ConfigItem();
            hostItem.setConfigKey(prefix + ".host");
            hostItem.setConfigValue(host);
            hostItem.setConfigDescription("扫码机主机地址");
            hostItem.setDataType("String");
            hostItem.setRequired(true);
            ConfigService.getInstance().saveConfigItem(hostItem);

            ConfigItem portItem = new ConfigItem();
            portItem.setConfigKey(prefix + ".port");
            portItem.setConfigValue(port);
            portItem.setConfigDescription("扫码机端口号");
            portItem.setDataType("Integer");
            portItem.setRequired(true);
            ConfigService.getInstance().saveConfigItem(portItem);

            ConfigItem formatItem = new ConfigItem();
            formatItem.setConfigKey(prefix + ".data_format");
            formatItem.setConfigValue(dataFormat);
            formatItem.setConfigDescription("扫码机数据格式");
            formatItem.setDataType("String");
            formatItem.setRequired(true);
            ConfigService.getInstance().saveConfigItem(formatItem);
            
            showInfoDialog("成功", "扫码机配置保存成功！");
            
            // 如果是模态窗口，保存后关闭
            if (primaryStage != null && primaryStage.isShowing()) {
                primaryStage.close();
            }
        } catch (Exception e) {
            showErrorDialog("错误", "保存配置失败：" + e.getMessage());
        }
    }
    
    /**
     * 取消配置
     */
    private void cancel() {
        // 如果是模态窗口，直接关闭
        if (primaryStage != null && primaryStage.isShowing()) {
            primaryStage.close();
        } else {
            loadData(); // 否则重新加载数据
        }
    }
    
    /**
     * 验证输入
     */
    private boolean validateInput() {
        // 验证端口号
        String portText = portTextField.getText().trim();
        if (portText.isEmpty()) {
            showErrorDialog("错误", "请输入端口号");
            return false;
        }
        
        try {
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                showErrorDialog("错误", "端口号必须在1-65535之间");
                return false;
            }
        } catch (NumberFormatException e) {
            showErrorDialog("错误", "端口号必须是数字");
            return false;
        }
        
        // 验证主机地址
        String host = hostTextField.getText().trim();
        if (host.isEmpty()) {
            showErrorDialog("错误", "请输入主机地址");
            return false;
        }
        
        // 如果是TCP客户端模式，验证IP地址格式
        if ("TCP客户端".equals(protocolComboBox.getValue()) && !isValidIpAddress(host)) {
            showErrorDialog("错误", "请输入有效的IP地址");
            return false;
        }
        
        return true;
    }
    
    /**
     * 验证IP地址格式
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        
        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 显示错误对话框
     */
    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * 显示信息对话框
     */
    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}