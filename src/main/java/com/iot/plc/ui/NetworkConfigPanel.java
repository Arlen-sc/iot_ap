package com.iot.plc.ui;

import com.iot.plc.service.NetworkService;
import com.iot.plc.service.ConfigService;
import com.iot.plc.model.ConfigItem;
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
 * 网络接收模块配置面板
 * 用于配置TCP服务端、TCP客户端和UDP协议参数
 */
public class NetworkConfigPanel extends VBox {
    // 使用我们统一的Logger类
    private static final Logger logger = Logger.getInstance();
    private Stage stage;
    private NetworkService.Config config;
    
    // UI组件
    private ComboBox<String> protocolComboBox;
    private TextField hostTextField;
    private TextField portTextField;
    private ComboBox<String> dataModeComboBox;
    private Button saveButton;
    private Button cancelButton;
    private Label statusLabel;
    
    public NetworkConfigPanel() {
        initializeUI();
    }
    
    private void initializeUI() {
        stage = new Stage();
        stage.setTitle("网络接收模块配置");
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
        portTextField.setText("8888");
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
            NetworkService.ProtocolType protocolType;
            String selectedProtocol = protocolComboBox.getValue();
            switch (selectedProtocol) {
                case "TCP服务端":
                    protocolType = NetworkService.ProtocolType.TCP_SERVER;
                    break;
                case "TCP客户端":
                    protocolType = NetworkService.ProtocolType.TCP_CLIENT;
                    break;
                case "UDP":
                    protocolType = NetworkService.ProtocolType.UDP;
                    break;
                default:
                    showError("无效的协议类型");
                    return;
            }
            
            // 获取数据模式
            NetworkService.DataMode dataMode = dataModeComboBox.getValue().equals("ASCII")
                    ? NetworkService.DataMode.ASCII
                    : NetworkService.DataMode.HEX;
            
            // 创建配置对象
            config = new NetworkService.Config(protocolType, host, port, dataMode);
            
            // 保存配置到服务
            saveToService();
            
            // 显示成功消息
            statusLabel.setText("配置保存成功");
            statusLabel.setStyle("-fx-text-fill: green;");
            
            logger.info("Network configuration saved: " + protocolType + ", host: " + host + ", port: " + port + ", mode: " + dataMode);
            
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
            logger.error("Failed to save network configuration: " + e.getMessage());
        }
    }
    
    /**
     * 保存配置到NetworkService
     * @throws Exception 当保存到配置管理系统失败时抛出异常
     */
    private void saveToService() throws Exception {
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
            
            logger.debug("开始保存网络配置到配置管理系统");
            logger.debug("协议类型: " + config.getProtocolType().name());
            logger.debug("主机地址: " + config.getHost());
            logger.debug("端口号: " + config.getPort());
            
            // 先尝试查找是否已存在配置项
            try {
                String existingProtocol = configService.getConfigValueByKey("network.protocol");
                String existingHost = configService.getConfigValueByKey("network.host");
                String existingPort = configService.getConfigValueByKey("network.port");
                
                logger.debug("现有配置 - 协议: " + existingProtocol + ", 主机: " + existingHost + ", 端口: " + existingPort);
            } catch (Exception e) {
                logger.warn("查询现有配置时发生异常: " + e.getMessage());
            }
            
            try {
                // 保存协议类型
                ConfigItem protocolItem = new ConfigItem();
                protocolItem.setConfigKey("network.protocol");
                protocolItem.setConfigValue(config.getProtocolType().name());
                protocolItem.setDataType("STRING");
                protocolItem.setDescription("网络协议类型");
                protocolItem.setRequired(true);
                configService.saveConfigItem(protocolItem);
                logger.debug("成功保存协议类型配置");
                
                // 保存主机地址
                ConfigItem hostItem = new ConfigItem();
                hostItem.setConfigKey("network.host");
                hostItem.setConfigValue(config.getHost());
                hostItem.setDataType("STRING");
                hostItem.setDescription("主机地址");
                hostItem.setRequired(true);
                configService.saveConfigItem(hostItem);
                logger.debug("成功保存主机地址配置");
                
                // 保存端口号
                ConfigItem portItem = new ConfigItem();
                portItem.setConfigKey("network.port");
                portItem.setConfigValue(String.valueOf(config.getPort()));
                portItem.setDataType("INTEGER");
                portItem.setDescription("端口号");
                portItem.setRequired(true);
                configService.saveConfigItem(portItem);
                logger.debug("成功保存端口号配置");
                
                logger.info("网络配置保存到配置管理系统成功");
            } catch (Exception e) {
                logger.error("保存配置到配置管理系统失败: " + e.getMessage(), e);
                throw e;
            }
            
            // 保存数据模式
            ConfigItem dataModeItem = new ConfigItem();
            dataModeItem.setConfigKey("network.dataMode");
            dataModeItem.setConfigValue(config.getDataMode().name());
            dataModeItem.setDataType("STRING");
            dataModeItem.setDescription("数据格式");
            configService.saveConfigItem(dataModeItem);
            
            logger.info("网络配置已保存到配置管理系统");
        } catch (Exception e) {
            logger.error("保存配置到配置管理系统失败: " + e.getMessage());
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
            
            // 如果从配置管理系统加载失败，则从NetworkService加载
            if (!loadedFromConfigService) {
                NetworkService service = NetworkService.getInstance();
                NetworkService.Config currentConfig = service.getCurrentConfig();
                
                if (currentConfig != null) {
                    loadFromNetworkService(currentConfig);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load network configuration: " + e.getMessage());
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
            String protocol = configService.getConfigValueByKey("network.protocol");
            if (protocol == null) {
                return false;
            }
            
            // 加载主机地址
            String host = configService.getConfigValueByKey("network.host");
            if (host == null) {
                return false;
            }
            
            // 加载端口号
            String portStr = configService.getConfigValueByKey("network.port");
            if (portStr == null) {
                return false;
            }
            
            // 加载数据模式
            String dataMode = configService.getConfigValueByKey("network.dataMode");
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
            
            logger.info("从配置管理系统加载网络配置成功");
            return true;
        } catch (Exception e) {
            logger.warn("从配置管理系统加载网络配置失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 从NetworkService加载配置
     */
    private void loadFromNetworkService(NetworkService.Config currentConfig) {
        // 加载协议类型
        switch (currentConfig.getProtocolType()) {
            case TCP_SERVER:
                protocolComboBox.setValue("TCP服务端");
                break;
            case TCP_CLIENT:
                protocolComboBox.setValue("TCP客户端");
                break;
            case UDP:
                protocolComboBox.setValue("UDP");
                break;
        }
        
        // 加载主机地址和端口
        hostTextField.setText(currentConfig.getHost());
        portTextField.setText(String.valueOf(currentConfig.getPort()));
        
        // 加载数据模式
        dataModeComboBox.setValue(currentConfig.getDataMode() == NetworkService.DataMode.ASCII ? "ASCII" : "HEX");
        
        logger.info("从NetworkService加载网络配置成功");
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
    public NetworkService.Config getConfig() {
        return config;
    }
}