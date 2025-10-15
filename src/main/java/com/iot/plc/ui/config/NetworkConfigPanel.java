package com.iot.plc.ui.config;

import com.iot.plc.enumx.TcpServiceEnum;
import com.iot.plc.model.ConfigItem;
import com.iot.plc.service.ConfigService;
import com.iot.plc.ui.base.JavaFXBasePanel;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;


/**
 * 网络配置面板
 * 用于配置TCP服务端/客户端和UDP协议参数
 */
public class NetworkConfigPanel extends JavaFXBasePanel {
    private TcpServiceEnum selectedConfigType = TcpServiceEnum.BURNER;
    private ComboBox<String> protocolComboBox;
    private TextField hostTextField;
    private TextField portTextField;
    private ComboBox<String> dataFormatComboBox;

    public NetworkConfigPanel() {
        // 默认使用BURNER类型
        this.selectedConfigType = TcpServiceEnum.BURNER;
        initComponents();
        loadData();
    }
    
    public NetworkConfigPanel(TcpServiceEnum configType) {
        this.selectedConfigType = configType;
        initComponents();
        loadData();
    }
    
    /**
     * 显示配置面板
     */
    public void show() {
        // 此方法用于兼容现有代码，实际显示逻辑可能在调用方处理
        // 可以在这里添加其他必要的初始化逻辑
    }

    @Override
    protected void initComponents() {
        this.setPadding(new Insets(20));
        this.setSpacing(20);

        // 创建标题
        Label titleLabel = new Label("网络配置");
        titleLabel.setFont(Font.font(18));
        titleLabel.setAlignment(Pos.CENTER_LEFT);

        // 创建配置类型选择
        HBox configTypeBox = new HBox(10);
        Label configTypeLabel = new Label("TCP服务：");
        ComboBox<TcpServiceEnum> configTypeComboBox = new ComboBox<>();
        configTypeComboBox.getItems().addAll(TcpServiceEnum.values());
        configTypeComboBox.setValue(selectedConfigType);
        
        // 设置下拉框显示description
        configTypeComboBox.setCellFactory(lv -> new ListCell<TcpServiceEnum>() {
            @Override
            protected void updateItem(TcpServiceEnum item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getDescription());
            }
        });
        
        // 设置选中项显示description
        configTypeComboBox.setButtonCell(new ListCell<TcpServiceEnum>() {
            @Override
            protected void updateItem(TcpServiceEnum item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getDescription());
            }
        });
        
        configTypeComboBox.setOnAction(e -> {
            selectedConfigType = configTypeComboBox.getValue();
            // 切换配置类型时，重新加载数据
            loadData();
        });
        configTypeBox.getChildren().addAll(configTypeLabel, configTypeComboBox);

        // 创建配置表单
        GridPane configGrid = new GridPane();
        configGrid.setHgap(15);
        configGrid.setVgap(15);
        configGrid.setPadding(new Insets(10));

        // 协议选择
        Label protocolLabel = new Label("协议类型：");
        protocolComboBox = new ComboBox<>();
        protocolComboBox.getItems().addAll("TCP服务端", "TCP客户端", "UDP");
        protocolComboBox.setPrefWidth(200);
        configGrid.add(protocolLabel, 0, 0);
        configGrid.add(protocolComboBox, 1, 0);

        // 主机地址
        Label hostLabel = new Label("主机地址：");
        hostTextField = new TextField();
        hostTextField.setPromptText("请输入主机地址或IP");
        hostTextField.setPrefWidth(200);
        configGrid.add(hostLabel, 0, 1);
        configGrid.add(hostTextField, 1, 1);

        // 端口号
        Label portLabel = new Label("端口号：");
        portTextField = new TextField();
        portTextField.setPromptText("请输入端口号（1-65535）");
        portTextField.setPrefWidth(200);
        configGrid.add(portLabel, 0, 2);
        configGrid.add(portTextField, 1, 2);

        // 数据格式
        Label dataFormatLabel = new Label("数据格式：");
        dataFormatComboBox = new ComboBox<>();
        dataFormatComboBox.getItems().addAll("ASCII", "HEX");
        dataFormatComboBox.setPrefWidth(200);
        configGrid.add(dataFormatLabel, 0, 3);
        configGrid.add(dataFormatComboBox, 1, 3);

        // 创建按钮面板
        HBox buttonPanel = new HBox(15);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);
        Button saveButton = new Button("保存配置");
        Button cancelButton = new Button("取消");
        saveButton.setPrefWidth(100);
        cancelButton.setPrefWidth(100);

        saveButton.setOnAction(e -> saveConfig());
        cancelButton.setOnAction(e -> loadData()); // 取消时重新加载数据

        buttonPanel.getChildren().addAll(saveButton, cancelButton);

        // 添加所有组件到主面板
        this.getChildren().addAll(titleLabel, configTypeBox, configGrid, buttonPanel);

        // 添加分隔线
        Separator separator1 = new Separator();
        Separator separator2 = new Separator();
        this.getChildren().add(1, separator1);
        this.getChildren().add(3, separator2);

        // 设置组件样式
        configGrid.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 4px; -fx-padding: 20px;");
        saveButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        cancelButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold;");
    }

    @Override
    protected void loadData() {
        // 加载协议类型
        String protocol = ConfigService.getInstance().getConfigValueByKeyOrDefault(selectedConfigType.getCode() + ".tcp.protocol", "TCP服务端");
        protocolComboBox.setValue(protocol);
        
        // 加载主机地址
        String host= ConfigService.getInstance().getConfigValueByKeyOrDefault(selectedConfigType.getCode() + ".tcp.host", "0.0.0.0");
        String port=ConfigService.getInstance().getConfigValueByKeyOrDefault(selectedConfigType.getCode() + ".tcp.port", "8888");
        hostTextField.setText(host);
        portTextField.setText(port);
        
        // 加载数据格式
        String dataFormat = ConfigService.getInstance().getConfigValueByKeyOrDefault(selectedConfigType.getCode() + ".tcp.data_format", "ASCII");
        dataFormatComboBox.setValue(dataFormat);
    }

    @Override
    public void refresh() {
        loadData();
    }

    /**
     * 保存网络配置
     */
    private void saveConfig() {
        // 验证输入
        if (!validateInput()) {
            return;
        }

        String prefix = selectedConfigType.getCode() + ".tcp";
        String protocol = protocolComboBox.getValue();
        String host = hostTextField.getText().trim();
        String port = portTextField.getText().trim();
        String dataFormat = dataFormatComboBox.getValue();

        // 保存配置
        try {
            // 创建ConfigItem对象并保存
            ConfigItem protocolItem = new ConfigItem();
            protocolItem.setConfigKey(prefix + ".protocol");
            protocolItem.setConfigValue(protocol);
            protocolItem.setConfigDescription("协议类型");
            protocolItem.setDataType("String");
            protocolItem.setRequired(true);
            ConfigService.getInstance().saveConfigItem(protocolItem);

            ConfigItem hostItem = new ConfigItem();
            hostItem.setConfigKey(prefix + ".host");
            hostItem.setConfigValue(host);
            hostItem.setConfigDescription("主机地址");
            hostItem.setDataType("String");
            hostItem.setRequired(true);
            ConfigService.getInstance().saveConfigItem(hostItem);

            ConfigItem portItem = new ConfigItem();
            portItem.setConfigKey(prefix + ".port");
            portItem.setConfigValue(port);
            portItem.setConfigDescription("端口号");
            portItem.setDataType("Integer");
            portItem.setRequired(true);
            ConfigService.getInstance().saveConfigItem(portItem);

            ConfigItem formatItem = new ConfigItem();
            formatItem.setConfigKey(prefix + ".data_format");
            formatItem.setConfigValue(dataFormat);
            formatItem.setConfigDescription("数据格式");
            formatItem.setDataType("String");
            formatItem.setRequired(true);
            ConfigService.getInstance().saveConfigItem(formatItem);

            showInfoDialog("成功", "网络配置保存成功！");
        } catch (Exception e) {
            showErrorDialog("错误", "保存配置失败：" + e.getMessage());
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