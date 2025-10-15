package com.iot.plc.ui;

import com.iot.plc.service.NetworkService;
import com.iot.plc.service.ConfigService;
import com.iot.plc.model.ConfigItem;
import com.iot.plc.model.DataMode;
import com.iot.plc.enumx.ProtocolType;
import com.iot.plc.enumx.ServiceType;
import com.iot.plc.config.NetworkConfig;
import com.iot.plc.logger.Logger;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * 扫码机网络配置面板
 * 用于配置扫码机的TCP服务端参数
 */
public class ScannerConfigPanel extends VBox {
    // 使用统一的Logger类
    private static final Logger logger = Logger.getInstance();
    private Stage stage;
    private NetworkConfig config;
    
    // UI组件
    private ComboBox<String> protocolComboBox;
    private TextField hostTextField;
    private TextField portTextField;
    private ComboBox<String> dataModeComboBox;
    private Button saveButton;
    private Button cancelButton;
    private Label statusLabel;
    
    public ScannerConfigPanel() {
        initializeUI();
    }
    
    private void initializeUI() {
        stage = new Stage();
        stage.setTitle("扫码机网络配置");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setWidth(400);
        stage.setHeight(300);
        
        // 创建主布局
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        
        // 创建配置表单
        GridPane configGrid = new GridPane();
        configGrid.setHgap(10);
        configGrid.setVgap(15);
        configGrid.setAlignment(Pos.CENTER);
        
        // 协议类型
        Label protocolLabel = new Label("协议类型：");
        protocolComboBox = new ComboBox<>();
        protocolComboBox.getItems().addAll("TCP服务端", "TCP客户端", "UDP");
        protocolComboBox.setValue("TCP服务端");
        configGrid.add(protocolLabel, 0, 0);
        configGrid.add(protocolComboBox, 1, 0);
        
        // 主机地址
        Label hostLabel = new Label("主机地址：");
        hostTextField = new TextField();
        hostTextField.setPromptText("输入主机地址");
        hostTextField.setText("127.0.0.1");
        configGrid.add(hostLabel, 0, 1);
        configGrid.add(hostTextField, 1, 1);
        
        // 主机端口
        Label portLabel = new Label("主机端口：");
        portTextField = new TextField();
        portTextField.setPromptText("输入端口号");
        portTextField.setText("8889"); // 默认使用不同于烧录机的端口
        portTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            // 只允许输入数字
            if (!newValue.matches("\\d*")) {
                portTextField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        configGrid.add(portLabel, 0, 2);
        configGrid.add(portTextField, 1, 2);
        
        // 数据格式
        Label dataModeLabel = new Label("数据格式：");
        dataModeComboBox = new ComboBox<>();
        dataModeComboBox.getItems().addAll("ASCII", "HEX");
        dataModeComboBox.setValue("ASCII");
        configGrid.add(dataModeLabel, 0, 3);
        configGrid.add(dataModeComboBox, 1, 3);
        
        // 状态标签
        statusLabel = new Label("");
        statusLabel.setStyle("-fx-text-fill: green;");
        
        // 按钮区域
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        saveButton = new Button("保存配置");
        saveButton.setOnAction(e -> saveConfig());
        
        cancelButton = new Button("取消");
        cancelButton.setOnAction(e -> stage.close());
        
        buttonBox.getChildren().addAll(saveButton, cancelButton);
        
        // 添加到主布局
        root.getChildren().addAll(configGrid, statusLabel, buttonBox);
        
        // 创建场景并显示
        Scene scene = new Scene(root);
        stage.setScene(scene);
        
        // 加载已保存的配置
        loadConfig();
    }
    
    /**
     * 显示配置面板
     */
    public void show() {
        // 每次显示前重新加载配置
        loadConfig();
        stage.show();
    }
    
    /**
     * 保存配置
     */
    private void saveConfig() {
        try {
            // 验证端口
            String portText = portTextField.getText();
            if (portText.isEmpty()) {
                showError("端口号不能为空");
                return;
            }
            
            int port = Integer.parseInt(portText);
            if (port < 1 || port > 65535) {
                showError("端口号必须在1-65535之间");
                return;
            }
            
            // 验证主机地址
            String host = hostTextField.getText();
            if (host.isEmpty()) {
                showError("主机地址不能为空");
                return;
            }
            
            // 获取协议类型
            ProtocolType protocolType;
            String selectedProtocol = protocolComboBox.getValue();
            switch (selectedProtocol) {
                case "TCP服务端":
                    protocolType = ProtocolType.TCP_SERVER;
                    break;
                case "TCP客户端":
                    protocolType = ProtocolType.TCP_CLIENT;
                    break;
                case "UDP":
                    protocolType = ProtocolType.UDP;
                    break;
                default:
                    showError("无效的协议类型");
                    return;
            }
            
            // 获取数据模式
            DataMode dataMode = dataModeComboBox.getValue().equals("ASCII")
                    ? DataMode.ASCII
                    : DataMode.HEX;
            
            // 创建配置对象，设置服务类型为扫码机
            config = new NetworkConfig(ServiceType.SCANNER, host, port);
            config.setProtocolType(protocolType);
            config.setDataMode(dataMode);
            
            // 保存配置到服务
            saveToService();
            
            // 显示成功消息
            statusLabel.setText("配置保存成功");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            logger.info("Scanner network configuration saved: " + protocolType + ", host: " + host + ", port: " + port + ", mode: " + dataMode);
            
            // 3秒后自动关闭窗口
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(() -> stage.close());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } catch (NumberFormatException e) {
            showError("端口号必须是数字");
        } catch (Exception e) {
            showError("保存配置失败：" + e.getMessage());
            logger.error("Failed to save scanner network configuration: " + e.getMessage());
        }
    }
    
    /**
     * 保存配置到服务
     * @throws Exception 当保存到配置管理系统失败时抛出异常
     */
    private void saveToService() throws Exception {
        // 启动网络服务
        NetworkService.getInstance().startService(config);
        
        // 同时保存到配置管理系统
        saveToConfigService();
    }
    
    /**
     * 保存配置到配置管理系统
     * @throws Exception 当保存失败时抛出异常
     */
    private void saveToConfigService() throws Exception {
        try {
            ConfigService configService = ConfigService.getInstance();
            
            // 验证配置对象
            if (config == null) {
                throw new IllegalStateException("配置对象不能为空");
            }
            if (config.getProtocolType() == null) {
                throw new IllegalStateException("协议类型不能为空");
            }
            if (config.getHost() == null || config.getHost().trim().isEmpty()) {
                throw new IllegalStateException("主机地址不能为空");
            }
            
            logger.debug("开始保存扫码机网络配置到配置管理系统");
            logger.debug("协议类型: " + config.getProtocolType().name());
            logger.debug("主机地址: " + config.getHost());
            logger.debug("端口号: " + config.getPort());
            
            // 保存协议类型
            ConfigItem protocolItem = new ConfigItem();
            protocolItem.setConfigKey("scanner.network.protocol");
            protocolItem.setConfigValue(config.getProtocolType().name());
            protocolItem.setDataType("STRING");
            protocolItem.setDescription("扫码机网络协议类型");
            protocolItem.setRequired(true);
            configService.saveConfigItem(protocolItem);
            logger.debug("成功保存扫码机协议类型配置");
            
            // 保存主机地址
            ConfigItem hostItem = new ConfigItem();
            hostItem.setConfigKey("scanner.network.host");
            hostItem.setConfigValue(config.getHost());
            hostItem.setDataType("STRING");
            hostItem.setDescription("扫码机主机地址");
            hostItem.setRequired(true);
            configService.saveConfigItem(hostItem);
            logger.debug("成功保存扫码机主机地址配置");
            
            // 保存端口号
            ConfigItem portItem = new ConfigItem();
            portItem.setConfigKey("scanner.network.port");
            portItem.setConfigValue(String.valueOf(config.getPort()));
            portItem.setDataType("INTEGER");
            portItem.setDescription("扫码机端口号");
            portItem.setRequired(true);
            configService.saveConfigItem(portItem);
            logger.debug("成功保存扫码机端口号配置");
            
            // 保存数据模式
            ConfigItem dataModeItem = new ConfigItem();
            dataModeItem.setConfigKey("scanner.network.dataMode");
            dataModeItem.setConfigValue(config.getDataMode().name());
            dataModeItem.setDataType("STRING");
            dataModeItem.setDescription("扫码机数据格式");
            configService.saveConfigItem(dataModeItem);
            
            logger.info("扫码机网络配置已保存到配置管理系统");
        } catch (Exception e) {
            logger.error("保存扫码机配置到配置管理系统失败: " + e.getMessage());
            throw e; // 重新抛出异常，让上层知道保存失败
        }
    }
    
    /**
     * 加载配置
     */
    private void loadConfig() {
        try {
            // 优先从配置管理系统加载配置
            boolean loadedFromConfigService = loadFromConfigService();
            
            // 如果从配置管理系统加载失败，则使用默认配置
            if (!loadedFromConfigService) {
                // 使用默认配置
                protocolComboBox.setValue("TCP服务端");
                hostTextField.setText("127.0.0.1");
                portTextField.setText("8889");
                dataModeComboBox.setValue("ASCII");
            }
        } catch (Exception e) {
            logger.warn("Failed to load scanner network configuration: " + e.getMessage());
        }
    }
    
    /**
     * 从配置管理系统加载配置
     * @return 是否成功加载
     */
    private boolean loadFromConfigService() {
        try {
            ConfigService configService = ConfigService.getInstance();
            
            // 加载协议类型
            String protocol = configService.getConfigValueByKey("scanner.network.protocol");
            if (protocol == null) {
                return false;
            }
            
            // 加载主机地址
            String host = configService.getConfigValueByKey("scanner.network.host");
            if (host == null) {
                return false;
            }
            
            // 加载端口号
            String portStr = configService.getConfigValueByKey("scanner.network.port");
            if (portStr == null) {
                return false;
            }
            
            // 加载数据模式
            String dataMode = configService.getConfigValueByKey("scanner.network.dataMode");
            if (dataMode == null) {
                return false;
            }
            
            // 设置UI组件值
            if (protocol.equals("TCP_SERVER")) {
                protocolComboBox.setValue("TCP服务端");
            } else if (protocol.equals("TCP_CLIENT")) {
                protocolComboBox.setValue("TCP客户端");
            } else if (protocol.equals("UDP")) {
                protocolComboBox.setValue("UDP");
            }
            
            hostTextField.setText(host);
            portTextField.setText(portStr);
            dataModeComboBox.setValue(dataMode.equals("ASCII") ? "ASCII" : "HEX");
            
            // 创建配置对象
            int port = Integer.parseInt(portStr);
            ProtocolType protocolType = ProtocolType.valueOf(protocol);
            DataMode dataModeType = DataMode.valueOf(dataMode);
            config = new NetworkConfig(ServiceType.SCANNER, host, port);
            config.setProtocolType(protocolType);
            config.setDataMode(dataModeType);
            
            logger.info("从配置管理系统加载扫码机网络配置成功");
            return true;
        } catch (Exception e) {
            logger.warn("从配置管理系统加载扫码机网络配置失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 显示错误消息
     */
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: red;");
    }
    
    /**
     * 获取配置
     */
    public NetworkConfig getConfig() {
        return config;
    }
}