package com.iot.plc.ui;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import com.iot.plc.enumx.TcpServiceEnum;
import java.sql.SQLException;
import java.util.UUID;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.GridPane;

import com.alibaba.fastjson.JSONObject;
import com.iot.plc.config.NetworkConfig;
import com.iot.plc.database.DatabaseManager;
import com.iot.plc.listener.NetworkListener;
import com.iot.plc.service.EmsService;
import com.iot.plc.service.ConfigService;
import com.iot.plc.service.SerialPortService;
import com.iot.plc.service.NetworkService;
import com.iot.plc.service.LogService;
import com.iot.plc.model.BarcodeData;
import com.iot.plc.model.BurnResultData;
import com.iot.plc.model.ConfigItem;
import com.iot.plc.model.DeviceResult;
import com.iot.plc.model.ProgramResult;
import com.iot.plc.logger.Logger;
import com.iot.plc.ui.config.NetworkConfigPanel;

/**
 * 自动处理面板
 * 实现用户需求的五个步骤：
 * 1. 监听串口读取扫描枪条码数据，自动缓存数据，绑定设备号+扫描内容（条码）
 * 2. 对比PLC传输的产品个数与缓存的条码个数，若相等则OK，反之给PLC发送异常指令
 * 3. 接收PLC开始指令后，自动给上位机传送烧录指令和多条条码信息
 * 4. 接收上位机返回的条码信息+烧录结果
 * 5. 保存结果并回传给EMS
 * 
 * 注意：此面板使用模拟数据，不依赖于真实的Netty服务
 */
public class AutoProcessPanel extends BorderPane {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger logger = Logger.getInstance();

    // 服务实例
    private NetworkService networkService;
    private TextArea networkDataArea; // 右侧网络数据显示区域
    private LinkedList<String> networkDataBuffer = new LinkedList<>(); // 网络数据缓冲区
    private static final int MAX_NETWORK_DATA_LINES = 100;
    private final SerialPortService serialPortService;

    // 状态管理
    private StringProperty currentStatus = new SimpleStringProperty("空闲");
    private StringProperty serialPortStatus = new SimpleStringProperty("未连接");
    private StringProperty plcStatus = new SimpleStringProperty("未连接");
    private StringProperty expectedBarcodeCount = new SimpleStringProperty("6");
    private StringProperty actualBarcodeCount = new SimpleStringProperty("0");
    private StringProperty connectionStatus = new SimpleStringProperty("0个连接"); // 连接状态属性
    private StringProperty scannerStatus = new SimpleStringProperty("未连接"); // 扫码机状态属性
    private StringProperty scannerConnectionStatus = new SimpleStringProperty("0个连接"); // 扫码机连接状态属性
    private StringProperty plcConnectionStatus = new SimpleStringProperty("0个连接"); // PLC连接状态属性
    private static final String CONFIG_KEY_EXPECTED_BARCODE_COUNT = "expected_barcode_count";

    // 数据管理
    private ObservableList<BarcodeData> barcodeDataList = FXCollections.observableArrayList();
    // 烧录结果数据列表
    private ObservableList<BurnResultData> burnResultDataList = FXCollections.observableArrayList();
    // 当前条码列表
    private final List<String> currentBarcodes = new ArrayList<>();
    // 设备ID
    private final String deviceId;

    // UI组件
    private TableView<BarcodeData> barcodeTable;
    private TableView<BurnResultData> burnResultTable;
    private TextArea logArea;
    private Button resetProcessButton;
    private Button clearBarcodesButton;
    private Button simulateScanButton;
    private Button simulatePlcCountButton;
    private Button simulatePlcStartButton;
    private TextField barcodeInputField;
    private Button confirmBarcodeButton;
    private ComboBox<String> comPortComboBox;
    private Button startComMonitorButton;
    private Button stopComMonitorButton;
    private boolean isMonitoringComPort = false;
    private TextField expectedBarcodeCountInput;
    private Button applyExpectedCountButton;

    // 流程控制标志
    private AtomicBoolean processStarted = new AtomicBoolean(false);
    private AtomicBoolean barcodeVerified = new AtomicBoolean(false);
    private AtomicBoolean waitingForStartCommand = new AtomicBoolean(false);
    private AtomicBoolean programCommandSent = new AtomicBoolean(false);
    private AtomicBoolean waitingForProgramResult = new AtomicBoolean(false);
    private AtomicBoolean processCompleted = new AtomicBoolean(false);

    // 随机数生成器
    private Random random = new Random();

    /**
     * 自动处理面板构造函数
     */
    public AutoProcessPanel() {
        // 从配置管理中获取设备ID
        ConfigService configService = ConfigService.getInstance();
        String configDeviceId = configService.getConfigValueByKey("device.id");
        this.deviceId = configDeviceId != null ? configDeviceId : "PLC_DEVICE_001"; // 默认值作为后备
        
        // 初始化服务实例
        this.serialPortService = SerialPortService.getInstance();

        initUI();
        startStatusUpdateThread();
        loadExpectedBarcodeCountFromConfig();
        
        // 添加LogService监听器，确保Logger的日志能够显示在UI中
        setupLogListener();
    }
    
    /**
     * 设置日志监听器，将Logger的日志转发到UI的logArea
     */
    private void setupLogListener() {
        LogService logService = LogService.getInstance();
        logService.addLogListener(message -> {
            // 使用Platform.runLater确保在JavaFX线程中更新UI
            Platform.runLater(() -> {
                logArea.appendText(message + "\n");
                // 自动滚动到底部
                logArea.setScrollTop(Double.MAX_VALUE);
            });
        });
    }

    /**
     * 从配置服务加载预期条码数量
     */
    private void loadExpectedBarcodeCountFromConfig() {
        try {
            String configValue = ConfigService.getInstance().getConfigValueByKey(CONFIG_KEY_EXPECTED_BARCODE_COUNT);
            if (configValue != null && !configValue.trim().isEmpty()) {
                try {
                    int count = Integer.parseInt(configValue.trim());
                    if (count > 0) {
                        expectedBarcodeCount.set(configValue);
                        expectedBarcodeCountInput.setText(configValue);
                        log("[配置加载] 从配置服务成功加载预期条码数量: " + configValue);
                    } else {
                        // 如果配置值无效（小于等于0），使用默认值并更新配置
                        log("[配置检查] 配置服务中的预期条码数量值无效: " + configValue + ", 将使用默认值: " + expectedBarcodeCount.get());
                        saveDefaultExpectedBarcodeCount();
                    }
                } catch (NumberFormatException e) {
                    // 如果配置值格式错误，使用默认值并更新配置
                    log("[配置检查] 配置服务中的预期条码数量格式错误: " + configValue + ", 将使用默认值: " + expectedBarcodeCount.get());
                    saveDefaultExpectedBarcodeCount();
                }
            } else {
                // 如果配置不存在，设置默认值并保存到配置服务
                saveDefaultExpectedBarcodeCount();
            }
        } catch (Exception e) {
            log("[配置加载错误] 从配置服务加载预期条码数量失败: " + e.getMessage());
            // 如果加载失败，使用当前的默认值
            saveDefaultExpectedBarcodeCount();
        }
    }

    /**
     * 保存默认的预期条码数量到配置服务
     */
    private void saveDefaultExpectedBarcodeCount() {
        try {
            // 确保配置值有效
            String configValue = expectedBarcodeCount.get().trim();
            if (configValue.isEmpty()) {
                configValue = "6"; // 使用默认值6
            }

            // 先检查配置项是否已存在
            String existingValue = ConfigService.getInstance().getConfigValueByKey(CONFIG_KEY_EXPECTED_BARCODE_COUNT);
            ConfigItem configItem = null;

            if (existingValue != null) {
                // 如果已存在，获取现有配置项
                try {
                    List<ConfigItem> allConfigs = DatabaseManager.getAllConfigItems();
                    for (ConfigItem item : allConfigs) {
                        if (item.getConfigKey().equals(CONFIG_KEY_EXPECTED_BARCODE_COUNT)) {
                            configItem = item;
                            configItem.setConfigValue(configValue); // 更新值
                            break;
                        }
                    }
                } catch (SQLException e) {
                    // 如果获取现有配置项失败，创建新的配置项
                    log("[配置检查] 获取现有配置项失败: " + e.getMessage() + ", 将创建新配置项");
                    configItem = new ConfigItem(
                            CONFIG_KEY_EXPECTED_BARCODE_COUNT,
                            configValue,
                            "自动处理流程中预期的条码数量",
                            "INTEGER",
                            false // 设置为非必填配置项
                    );
                }
            } else {
                // 如果不存在，创建新配置项
                configItem = new ConfigItem(
                        CONFIG_KEY_EXPECTED_BARCODE_COUNT,
                        configValue,
                        "自动处理流程中预期的条码数量",
                        "INTEGER",
                        false // 设置为非必填配置项
                );
            }

            ConfigService.getInstance().saveConfigItem(configItem);
            log("[配置保存] 已保存默认预期条码数量到配置服务: " + configValue);
        } catch (Exception e) {
            log("[配置保存错误] 保存默认预期条码数量到配置服务失败: " + e.getMessage());
            // 记录详细异常信息以便调试
            e.printStackTrace();
        }
    }

    private void initUI() {
        // 创建菜单栏
        MenuBar menuBar = createMenuBar();
        setTop(menuBar);

        // 顶部状态栏
        VBox statusBox = createStatusBox();

        // 扫描框区域
        VBox scanBox = createScanBox();
        statusBox.getChildren().add(scanBox); // 添加到状态栏下方

        // 中部表格区域 - 将条码数据和烧录结果合并到一个界面
        VBox dataPanel = new VBox(10);
        dataPanel.setPrefWidth(Region.USE_COMPUTED_SIZE);

        // 条码表格
        barcodeTable = createBarcodeTable();
        Label barcodeLabel = new Label("条码数据");
        barcodeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        /**
         * 条码表格滚动区域
         */
        ScrollPane barcodeScrollPane = new ScrollPane(barcodeTable);
        barcodeScrollPane.setFitToWidth(true);
        barcodeScrollPane.setFitToHeight(true);
        VBox.setVgrow(barcodeScrollPane, Priority.ALWAYS);

        // 烧录结果表格
        burnResultTable = createBurnResultTable();
        Label resultLabel = new Label("烧录结果");
        resultLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        /**
         * 烧录结果表格滚动区域
         */
        ScrollPane resultScrollPane = new ScrollPane(burnResultTable);
        resultScrollPane.setFitToWidth(true);
        resultScrollPane.setFitToHeight(true);
        VBox.setVgrow(resultScrollPane, Priority.ALWAYS);

        dataPanel.getChildren().addAll(
                barcodeLabel,
                barcodeScrollPane,
                resultLabel,
                resultScrollPane);

        // 底部日志和操作按钮
        VBox bottomBox = new VBox(10);
        bottomBox.setPrefWidth(Region.USE_COMPUTED_SIZE);

        // 操作按钮 - 设置为可换行
        FlowPane buttonFlowPane = new FlowPane();
        buttonFlowPane.setHgap(10);
        buttonFlowPane.setVgap(5);
        buttonFlowPane.setPrefWrapLength(Region.USE_COMPUTED_SIZE); // 自动计算换行宽度
        buttonFlowPane.setPrefWidth(Region.USE_COMPUTED_SIZE);

        resetProcessButton = new Button("重置流程");
        clearBarcodesButton = new Button("清空条码");
        simulateScanButton = new Button("模拟扫描");
        simulatePlcCountButton = new Button("模拟PLC数量");
        simulatePlcStartButton = new Button("模拟PLC开始");
        Button simulateComDataButton = new Button("模拟COM口数据");

        resetProcessButton.setOnAction(e -> resetProcess());
        clearBarcodesButton.setOnAction(e -> clearBarcodes());
        simulateScanButton.setOnAction(e -> simulateBarcodeScan());
        simulatePlcCountButton.setOnAction(e -> simulatePlcProductCount());
        simulatePlcStartButton.setOnAction(e -> simulatePlcStartCommand());
        simulateComDataButton.setOnAction(e -> showComDataInputDialog());

        buttonFlowPane.getChildren().addAll(
                resetProcessButton, clearBarcodesButton,
                simulateScanButton, simulatePlcCountButton, simulatePlcStartButton,
                simulateComDataButton);

        // 日志区域（左侧）
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(Region.USE_COMPUTED_SIZE);
        ScrollPane logScrollPane = new ScrollPane(logArea);
        logScrollPane.setFitToWidth(true);
        logScrollPane.setFitToHeight(true);

        // 网络数据显示区域（右侧）
        networkDataArea = new TextArea();
        networkDataArea.setEditable(false);
        networkDataArea.setPrefHeight(Region.USE_COMPUTED_SIZE);
        networkDataArea.setPromptText("网络服务接收数据显示区域");
        ScrollPane networkDataScrollPane = new ScrollPane(networkDataArea);
        networkDataScrollPane.setFitToWidth(true);
        networkDataScrollPane.setFitToHeight(true);

        // 创建左右布局的HBox，确保在小屏幕上能够适当缩放
        HBox logContainer = new HBox(10);
        logContainer.getChildren().addAll(logScrollPane, networkDataScrollPane);
        logContainer.setFillHeight(true);
        VBox.setVgrow(logContainer, Priority.ALWAYS);

        // 为左右日志区域设置权重，确保均匀分配空间
        HBox.setHgrow(logScrollPane, Priority.ALWAYS);
        HBox.setHgrow(networkDataScrollPane, Priority.ALWAYS);

        bottomBox.getChildren().addAll(buttonFlowPane, logContainer);

        // 创建主内容区域，包含状态栏、数据面板和底部区域
        VBox mainContent = new VBox(10);
        mainContent.getChildren().addAll(statusBox, dataPanel, bottomBox);
        setCenter(mainContent);

        setPadding(new Insets(10));

        // 设置面板自适应父容器大小
        setPrefWidth(Region.USE_COMPUTED_SIZE);
        setPrefHeight(Region.USE_COMPUTED_SIZE);

        // 添加窗口大小变化监听器，实现真正的自适应布局
        widthProperty().addListener((obs, oldWidth, newWidth) -> {
            adjustLayoutForWindowSize();
        });

        heightProperty().addListener((obs, oldHeight, newHeight) -> {
            adjustLayoutForWindowSize();
        });

        // 初始化网络服务
        initializeNetworkService();
    }

    /**
     * 根据窗口大小调整布局
     */
    private void adjustLayoutForWindowSize() {
        double width = getWidth();
        double height = getHeight();

        // 根据窗口宽度调整组件布局
        if (width > 0) {
            // 调整日志区域的高度，使其随窗口高度变化
            double logAreaHeight = Math.max(250, height * 0.2); // 至少150px，或窗口高度的20%
            if (logArea != null) {
                logArea.setPrefHeight(logAreaHeight);
            }
            if (networkDataArea != null) {
                networkDataArea.setPrefHeight(logAreaHeight);
            }

            // 根据窗口宽度调整表格区域的高度
            double tableHeight = Math.max(150, height * 0.3); // 至少150px，或窗口高度的30%
            if (barcodeTable != null && barcodeTable.getParent() instanceof ScrollPane) {
                ((ScrollPane) barcodeTable.getParent()).setPrefHeight(tableHeight);
            }
            if (burnResultTable != null && burnResultTable.getParent() instanceof ScrollPane) {
                ((ScrollPane) burnResultTable.getParent()).setPrefHeight(tableHeight);
            }
        }
    }

    // 上位机配置相关常量
    private static final String CONFIG_KEY_UPPER_COMPUTER_IP = "upper_computer_ip";
    private static final String CONFIG_KEY_UPPER_COMPUTER_PORT = "upper_computer_port";
    private static final String DEFAULT_UPPER_COMPUTER_IP = "127.0.0.1";
    private static final String DEFAULT_UPPER_COMPUTER_PORT = "8080";

    // EMS配置相关常量
    private static final String CONFIG_KEY_EMS_URL = "ems_api_url";
    private static final String DEFAULT_EMS_URL = "http://localhost:8080/api";

    // 流程控制开关
    private ToggleButton processControlToggle;
    private ToggleButton serverToggleButton; // Server控制按钮（合并启动和停止功能）

    // 初始化网络服务
    private void initializeNetworkService() {
        networkService = NetworkService.getInstance();

        // 设置网络数据监听器
        networkService.setNetworkListener(new NetworkListener() {
            @Override
            public void onLogReceived(String logMessage) {
                // 在JavaFX应用线程中更新UI日志区域
                Platform.runLater(() -> {
                    log(logMessage);
                });
            }

            @Override
            public void onLog(String message) {
                // 兼容扫码机日志方法，调用已有的onLogReceived
                onLogReceived(message);
            }

            // 只实现新版方法，支持服务类型区分
            @Override
            public void onDataReceived(String data, TcpServiceEnum serviceType) {
                // 根据服务类型获取别名信息
                final String alias;
                NetworkConfig config = networkService.getConfig(serviceType);
                if (config != null && config.getAlias() != null && !config.getAlias().isEmpty()) {
                    alias = "[" + config.getAlias() + "] ";
                } else {
                    alias = "";
                }
                
                // 根据不同的服务类型处理数据
                if (data != null && !data.trim().isEmpty()) {
                    if (serviceType == TcpServiceEnum.SCANNER) {
                        // 扫码机服务，保存条码数据
                        try {
                            // 保存条码数据到数据库
                            String deviceId = "SCANNER_NETWORK";
                            String portName = "NETWORK_PORT";
                            DatabaseManager.saveBarcodeData(deviceId, data.trim(), portName);
                            
                            // 调用封装的条码处理方法
                            scannerProcessBarcodeData(data.trim());
                        } catch (SQLException e) {
                            logger.error("保存扫码机条码数据失败: " + e.getMessage());
                        }
                    } else if (serviceType == TcpServiceEnum.BURNER) {
                        // 烧录机服务，处理特殊制令单数据
                        try {
                            log(data);
                            // 调用封装的条码处理方法处理烧录机发送的特殊制令单
                            burnerProcessBarcodeData(data.trim());
                        } catch (Exception e) {
                            logger.error("烧录机信息处理失败: " + e.getMessage());
                        }
                    } else if (serviceType == TcpServiceEnum.PLC) {
                        // PLC服务，记录接收到的数据
                        try {
                            log("[PLC数据] " + data.trim());
                        } catch (Exception e) {
                            logger.error("PLC信息处理失败: " + e.getMessage());
                        }
                    }
                }
                
                // 在JavaFX应用线程中更新UI，包含别名信息
                Platform.runLater(() -> {
                    addNetworkData(alias + data);
                });
            }

            @Override
            public void onDataReceived(byte[] data, TcpServiceEnum serviceType) {
                // 将字节数组转换为字符串
                final String dataStr = new String(data);
                
                // 根据服务类型获取别名信息
                final String alias;
                NetworkConfig config = networkService.getConfig(serviceType);
                if (config != null && config.getAlias() != null && !config.getAlias().isEmpty()) {
                    alias = "[" + config.getAlias() + "] ";
                } else {
                    alias = "";
                }
                
                // 根据不同的服务类型处理数据
                if (dataStr != null && !dataStr.trim().isEmpty()) {
                    if (serviceType == TcpServiceEnum.SCANNER) {
                        // 扫码机服务，保存条码数据
                        try {
                            // 调用封装的条码处理方法
                            scannerProcessBarcodeData(dataStr.trim());
                        } catch (Exception e) {
                            logger.error("保存扫码机条码数据失败: " + e.getMessage());
                        }
                    } else if (serviceType == TcpServiceEnum.PLC) {
                        // PLC服务，记录接收到的数据
                        try {
                            log("[PLC数据] " + dataStr.trim());
                            // 调用封装的条码处理方法处理PLC发送的条码数据
                            plcProcessBarcodeData(dataStr.trim());
                        } catch (Exception e) {
                            logger.error("PLC信息处理失败: " + e.getMessage());
                        }
                    } else if (serviceType == TcpServiceEnum.BURNER) {
                        // 烧录机服务，处理特殊制令单数据
                        try {
                            log(dataStr);
                            // 调用封装的条码处理方法处理烧录机发送的特殊制令单
                            burnerProcessBarcodeData(dataStr.trim());
                        } catch (Exception e) {
                            logger.error("烧录机信息处理失败: " + e.getMessage());
                        }
                    }
                }
                
                // 在JavaFX应用线程中更新UI，包含别名信息
                Platform.runLater(() -> {
                    addNetworkData(alias + dataStr);
                });
            }

            @Override
            public void onConnectionStatusChanged(boolean connected, TcpServiceEnum serviceType) {
                handleConnectionStatusChanged(connected, serviceType);
            }

            @Override
            public void onConnectionCountChanged(int count, TcpServiceEnum serviceType) {
                handleConnectionCountChanged(count, serviceType);
            }

            // 处理连接状态变化的私有辅助方法
            private void handleConnectionStatusChanged(boolean connected, TcpServiceEnum serviceType) {
                // 在JavaFX应用线程中更新UI
                Platform.runLater(() -> {
                    String status = connected ? "开启" : "关闭";

                    // 根据服务类型记录不同的日志和更新不同的状态显示
                    if (serviceType == TcpServiceEnum.BURNER) {
                        log("[烧录机网络状态] " + status);
                        plcStatus.set(status);
                        // 获取烧录机服务的连接数
                        int burnerConnections = networkService
                                .getConnectedClientCount(TcpServiceEnum.BURNER);
                        connectionStatus.set(burnerConnections + "个连接");
                    } else if (serviceType == TcpServiceEnum.SCANNER) {
                        log("[扫码机网络状态] " + status);
                        scannerStatus.set(status);
                        // 获取扫码机服务的连接数
                        int scannerConnections = networkService
                                .getConnectedClientCount(TcpServiceEnum.SCANNER);
                        scannerConnectionStatus.set(scannerConnections + "个连接");
                    }

                    // 只有当两个服务都处于开启状态时，才将按钮标记为选中状态
                    if (serverToggleButton != null) {
                        boolean bothServicesRunning = networkService.isServiceRunning(TcpServiceEnum.BURNER)
                                &&
                                networkService.isServiceRunning(TcpServiceEnum.SCANNER);
                        serverToggleButton.setSelected(bothServicesRunning);
                        serverToggleButton.setText(bothServicesRunning ? "关闭" : "开启");
                    }
                });
            }

            // 处理连接数变化的私有辅助方法
            private void handleConnectionCountChanged(int count, TcpServiceEnum serviceType) {
                // 在JavaFX应用线程中更新连接数显示
                Platform.runLater(() -> {
                    if (serviceType == TcpServiceEnum.BURNER) {
                        connectionStatus.set(count + "个连接");
                    } else if (serviceType == TcpServiceEnum.SCANNER) {
                        scannerConnectionStatus.set(count + "个连接");
                    }
                });
            }
        });
    }

    // 添加网络数据到显示区域，并保持只显示100条
    private void addNetworkData(String data) {
        String timestamp = LocalDateTime.now().format(formatter);
        String formattedData = timestamp + " - " + data;

        // 添加到缓冲区
        networkDataBuffer.addLast(formattedData);

        // 如果超过最大行数，移除最旧的
        if (networkDataBuffer.size() > MAX_NETWORK_DATA_LINES) {
            networkDataBuffer.removeFirst();
        }

        // 重新构建显示内容
        StringBuilder sb = new StringBuilder();
        for (String line : networkDataBuffer) {
            sb.append(line).append("\n");
        }

        networkDataArea.setText(sb.toString());
        networkDataArea.setScrollTop(Double.MAX_VALUE); // 自动滚动到底部
    }

    /**
     * 创建状态显示框
     * @return
     */
    private VBox createStatusBox() {
        VBox statusBox = new VBox(5);
        statusBox.setPadding(new Insets(5));
        statusBox.setStyle("-fx-border-color: lightgray; -fx-border-width: 1px; -fx-border-radius: 5px;");

        // 创建一个VBox容器用于放置流程控制和网络配置相关组件
        VBox processNetworkSection = new VBox(5);
        processNetworkSection.setPadding(new Insets(0));

        // 添加流程控制开关
        HBox processControlBox = new HBox(10);
        processControlToggle = new ToggleButton();
        processControlToggle.setStyle("-fx-base: #ff0000;");
        processControlToggle.setText("流程关闭");
        processControlToggle.setPrefWidth(100);

        // 设置开关的事件处理
        processControlToggle.setOnAction(e -> {
            if (processControlToggle.isSelected()) {
                // 开启流程
                processControlToggle.setStyle("-fx-base: #00ff00;");
                processControlToggle.setText("流程开启");
                startProcess();
            } else {
                // 关闭流程（执行重置操作）
                processControlToggle.setStyle("-fx-base: #ff0000;");
                processControlToggle.setText("流程关闭");
                resetProcess();
                processStarted.set(false);
                currentStatus.set("空闲");
            }
        });

        processControlBox.getChildren().addAll(
                // new Label("流程控制: "),
                processControlToggle,
                new Label("当前状态: "),
                createStatusLabel(currentStatus));

        // 创建一个HBox来放置网络配置、Server开关、Server状态和连接数，确保它们始终在同一行显示且靠左对齐
        HBox networkConfigRow = new HBox(10); // 设置组件间距为10
        networkConfigRow.setAlignment(Pos.CENTER_LEFT);
        networkConfigRow.setPadding(new Insets(0)); // 移除内边距
        networkConfigRow.setFillHeight(true); // 填充高度
        HBox.setHgrow(networkConfigRow, Priority.ALWAYS); // 水平方向占据全部可用空间

        // 网络配置按钮已移至菜单栏，此处不再创建

        // 添加Server控制按钮（合并启动和停止按钮）
        serverToggleButton = new ToggleButton("server开启");
        serverToggleButton.setPrefWidth(80);

        // 设置按钮事件处理
        serverToggleButton.setOnAction(e -> {
            if (serverToggleButton.isSelected()) {
                // 启动所有Server服务
                log("[操作] 用户点击了'Server开启'按钮");
                try {
                    //遍历所有服务类型
                    for (TcpServiceEnum serviceType : TcpServiceEnum.values()) {
                        // 使用通用方法启动服务（从配置管理获取参数）
                        log("[操作] 正在启动" + serviceType.name() + "服务...");
                        networkService.startTcpService(serviceType);
                        log("[操作结果] 已启动" + serviceType.name() + "服务");
                    }
                    serverToggleButton.setText("关闭");
                    
                    // 更新所有服务状态为已连接
                    plcStatus.set("开启");
                    scannerStatus.set("开启");
                    connectionStatus.set("1个连接");
                    scannerConnectionStatus.set("1个连接");
                    plcConnectionStatus.set("1个连接");
                    
                    // 启动其他服务
                    getBurnerBarcodeCount();
                } catch (Exception ex) {
                    log("[操作错误] 启动Server服务失败: " + ex.getMessage());
                    ex.printStackTrace();
                    serverToggleButton.setSelected(false);
                    serverToggleButton.setText("开启");
                    
                    // 更新所有服务状态为未连接
                    plcStatus.set("关闭");
                    scannerStatus.set("关闭");
                    connectionStatus.set("0个连接");
                    scannerConnectionStatus.set("0个连接");
                    plcConnectionStatus.set("0个连接");
                }
            } else {
                // 停止所有Server服务
                log("[操作] 用户点击了'Server关闭'按钮");
                try {
                    // 停止所有网络服务
                    if (networkService != null) {
                        networkService.stopAllServices();
                        log("[操作结果] 已停止所有Server服务");
                    } else {
                        log("[操作结果] Server服务未初始化");
                    }
                    serverToggleButton.setText("开启");
                    
                    // 更新所有服务状态为未连接
                    plcStatus.set("关闭");
                    scannerStatus.set("关闭");
                    connectionStatus.set("0个连接");
                    scannerConnectionStatus.set("0个连接");
                    plcConnectionStatus.set("0个连接");
                } catch (Exception ex) {
                    log("[操作错误] 停止Server服务失败: " + ex.getMessage());
                    ex.printStackTrace();
                    serverToggleButton.setSelected(true);
                    serverToggleButton.setText("关闭");
                    
                    // 更新所有服务状态为已连接
                    plcStatus.set("开启");
                    scannerStatus.set("开启");
                    connectionStatus.set("1个连接");
                    scannerConnectionStatus.set("1个连接");
                    plcConnectionStatus.set("1个连接");
                }
            }
        });

        // 检查初始状态并更新按钮显示
        if (networkService != null && networkService.isRunning()) {
            serverToggleButton.setSelected(true);
            serverToggleButton.setText("关闭");
        }

        // 将所有网络配置相关组件添加到同一行
        networkConfigRow.getChildren().add(serverToggleButton);
        
        // 为烧录机状态和连接数创建隔离框
        HBox burnerBox = createServiceStatusBox("烧录机", plcStatus, connectionStatus);
        networkConfigRow.getChildren().add(burnerBox);
        
        // 为扫码机状态和连接数创建隔离框
        HBox scannerBox = createServiceStatusBox("扫码机", scannerStatus, scannerConnectionStatus);
        networkConfigRow.getChildren().add(scannerBox);
        
        // 为PLC状态和连接数创建隔离框
        HBox plcBox = createServiceStatusBox("PLC", plcStatus, plcConnectionStatus);
        networkConfigRow.getChildren().add(plcBox);

        // 将流程控制和网络配置行添加到流程控制区域
        processNetworkSection.getChildren().addAll(processControlBox, networkConfigRow);

        // 使用TilePane替代HBox，使状态标签在小屏幕上可以自动换行
        TilePane mainStatusBox = new TilePane();
        mainStatusBox.setHgap(15);
        mainStatusBox.setVgap(5);
        mainStatusBox.setPrefColumns(2); // 控制每行显示的列数
        mainStatusBox.setTileAlignment(Pos.CENTER_LEFT);

        // 添加当前状态和串口状态
        mainStatusBox.getChildren().addAll(
                new Label("串口状态: "),
                createStatusLabel(serialPortStatus));

        // 使用FlowPane来放置配置按钮，使按钮在小屏幕上可以换行
        FlowPane configButtonsBox = new FlowPane();
        configButtonsBox.setHgap(10);
        configButtonsBox.setVgap(5);
        configButtonsBox.setPrefWrapLength(600); // 设置换行宽度

        // 配置按钮已移至菜单栏，此处为空的FlowPane以保持布局一致性

        HBox barcodeCountBox = new HBox(10);
        barcodeCountBox.setAlignment(Pos.CENTER_LEFT);
        expectedBarcodeCountInput = new TextField("6");
        expectedBarcodeCountInput.setPrefWidth(80);
        expectedBarcodeCountInput.setAlignment(Pos.CENTER);

        // 为文本框添加文本变化监听器，实现直接修改生效
        expectedBarcodeCountInput.textProperty().addListener((observable, oldValue, newValue) -> {
            // 当用户修改文本内容时自动验证并更新预期数量
            try {
                if (!newValue.isEmpty()) {
                    int count = Integer.parseInt(newValue.trim());
                    if (count >= 0) {
                        expectedBarcodeCount.set(newValue);
                        // 只有当输入是有效的数字时才记录日志，避免频繁日志记录
                        if (!oldValue.equals(newValue)) {
                            log("[自动更新] 预期条码数量已更新为: " + count);
                        }
                    }
                }
            } catch (NumberFormatException ignored) {
                // 忽略格式错误，等待用户输入有效的数字
            }
        });

        applyExpectedCountButton = new Button("应用");
        applyExpectedCountButton.setOnAction(e -> applyExpectedCount());

        barcodeCountBox.getChildren().addAll(
                new Label("预期条码数量: "),
                expectedBarcodeCountInput,
                applyExpectedCountButton,
                new Label("实际条码数量: "),
                createStatusLabel(actualBarcodeCount));

        statusBox.getChildren().addAll(processNetworkSection, mainStatusBox, configButtonsBox, barcodeCountBox);
        return statusBox;
    }



    /**
     * 应用用户输入的预期条码数量
     */
    private void applyExpectedCount() {
        log("[操作] 用户点击了'应用'按钮，确认设置预期条码数量");
        try {
            String countText = expectedBarcodeCountInput.getText().trim();
            int count = Integer.parseInt(countText);
            if (count >= 0) {
                expectedBarcodeCount.set(countText);
                log("[操作结果] 已确认设置预期条码数量: " + count);

                // 将新的预期条码数量保存到配置服务
                try {
                    // 先检查配置项是否已存在
                    String existingValue = ConfigService.getInstance()
                            .getConfigValueByKey(CONFIG_KEY_EXPECTED_BARCODE_COUNT);
                    ConfigItem configItem = null;

                    if (existingValue != null) {
                        // 如果已存在，获取现有配置项
                        try {
                            List<ConfigItem> allConfigs = DatabaseManager.getAllConfigItems();
                            for (ConfigItem item : allConfigs) {
                                if (item.getConfigKey().equals(CONFIG_KEY_EXPECTED_BARCODE_COUNT)) {
                                    configItem = item;
                                    configItem.setConfigValue(countText); // 更新值
                                    break;
                                }
                            }
                        } catch (SQLException e) {
                            // 如果获取现有配置项失败，创建新的配置项
                            log("[配置检查] 获取现有配置项失败: " + e.getMessage() + ", 将创建新配置项");
                            configItem = new ConfigItem(
                                    CONFIG_KEY_EXPECTED_BARCODE_COUNT,
                                    countText,
                                    "自动处理流程中预期的条码数量",
                                    "INTEGER",
                                    false // 设置为非必填配置项
                            );
                        }
                    } else {
                        // 如果不存在，创建新配置项
                        configItem = new ConfigItem(
                                CONFIG_KEY_EXPECTED_BARCODE_COUNT,
                                countText,
                                "自动处理流程中预期的条码数量",
                                "INTEGER",
                                false // 设置为非必填配置项
                        );
                    }

                    ConfigService.getInstance().saveConfigItem(configItem);
                    log("[配置更新] 已将预期条码数量更新到配置服务: " + countText);
                } catch (Exception e) {
                    log("[配置更新错误] 更新预期条码数量到配置服务失败: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                log("[操作结果] 无效输入：请输入非负整数作为预期条码数量");
                expectedBarcodeCountInput.setText(expectedBarcodeCount.get());
            }
        } catch (NumberFormatException e) {
            log("[操作结果] 无效输入：请输入有效的数字作为预期条码数量");
            expectedBarcodeCountInput.setText(expectedBarcodeCount.get());
        }
    }

    /** */
    private VBox createScanBox() {
        VBox scanBox = new VBox(5);
        scanBox.setPadding(new Insets(5));
        scanBox.setStyle(
                "-fx-border-color: lightblue; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-color: #f0f8ff;");

        Label scanLabel = new Label("条码输入");
        scanLabel.setStyle("-fx-font-weight: bold;");

        HBox inputBox = new HBox(10);
        barcodeInputField = new TextField();
        barcodeInputField.setPromptText("手动输入条码...");
        barcodeInputField.setPrefWidth(300);

        confirmBarcodeButton = new Button("确认输入");
        confirmBarcodeButton.setOnAction(e -> handleManualBarcodeInput());

        // // 串口选择和监控
        // Label comPortLabel = new Label("COM端口：");
        // comPortComboBox = new ComboBox<>();
        // comPortComboBox.getItems().addAll("COM1", "COM2", "COM3", "COM4", "COM5");
        // comPortComboBox.setValue("COM1");

        // startComMonitorButton = new Button("开始监控");
        // startComMonitorButton.setOnAction(e -> startComPortMonitoring());

        // stopComMonitorButton = new Button("停止监控");
        // stopComMonitorButton.setOnAction(e -> stopComPortMonitoring());
        // stopComMonitorButton.setDisable(true);

        // 串口换成tcp。
        inputBox.getChildren().addAll(
                scanLabel, barcodeInputField, confirmBarcodeButton
        // comPortLabel,
        // comPortComboBox,
        // startComMonitorButton, stopComMonitorButton
        );

        scanBox.getChildren().add(inputBox);
        return scanBox;
    }

    private Label createStatusLabel(StringProperty statusProperty) {
        Label label = new Label();
        label.textProperty().bind(statusProperty);

        // 状态颜色变化
        statusProperty.addListener((observable, oldValue, newValue) -> {
            if ("空闲".equals(newValue) || "未连接".equals(newValue)) {
                label.setTextFill(Color.BLACK);
            } else if ("运行中".equals(newValue) || "已连接".equals(newValue)) {
                label.setTextFill(Color.GREEN);
            } else if ("错误".equals(newValue) || "异常".equals(newValue)) {
                label.setTextFill(Color.RED);
            } else if ("验证通过".equals(newValue)) {
                label.setTextFill(Color.BLUE);
            }
        });

        return label;
    }
    
    /**
     * 创建服务状态隔离框
     * @param serviceName 服务名称
     * @param statusProperty 状态属性
     * @param connectionProperty 连接数属性
     * @return 包含状态和连接数的HBox
     */
    private HBox createServiceStatusBox(String serviceName, StringProperty statusProperty, StringProperty connectionProperty) {
        HBox box = new HBox(10);
        box.setPadding(new Insets(5, 10, 5, 10));
        box.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 5px;");
        
        // 根据初始状态设置背景颜色
        updateBoxBackground(box, statusProperty.get());
        
        // 添加状态监听器，动态更新背景颜色
        statusProperty.addListener((observable, oldValue, newValue) -> {
            updateBoxBackground(box, newValue);
        });
        
        Label nameLabel = new Label(serviceName + "：");
        // Label statusLabel = new Label("状态: ");
        Label connectionLabel = new Label("连接: ");
        
        box.getChildren().addAll(
                nameLabel,
                // statusLabel,
                createStatusLabel(statusProperty),
                connectionLabel,
                createStatusLabel(connectionProperty)
        );
        
        return box;
    }
    
    /**
     * 根据服务状态更新框的背景颜色
     * @param box 要更新的HBox
     * @param status 服务状态
     */
    private void updateBoxBackground(HBox box, String status) {
        if ("开启".equals(status)) {
            box.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-color: #e6ffe6;");
        } else {
            box.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 5px;");
        }
    }

    private TableView<BarcodeData> createBarcodeTable() {
        TableView<BarcodeData> table = new TableView<>();
        table.setItems(barcodeDataList);
        table.setPrefHeight(200); // 设置表格高度

        TableColumn<BarcodeData, String> deviceIdColumn = new TableColumn<>("设备ID");
        deviceIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDeviceId()));
        deviceIdColumn.setPrefWidth(200);

        TableColumn<BarcodeData, String> barcodeColumn = new TableColumn<>("条码内容");
        barcodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBarcode()));
        barcodeColumn.setPrefWidth(300);

        TableColumn<BarcodeData, String> scanTimeColumn = new TableColumn<>("扫描时间");
        scanTimeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getScanTime() != null ? 
                cellData.getValue().getScanTime().format(formatter) : ""));
        scanTimeColumn.setPrefWidth(200);

        // TableColumn<BarcodeData, String> portNameColumn = new TableColumn<>("串口名称");
        // portNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPortName()));
        // portNameColumn.setPrefWidth(100);

        table.getColumns().addAll(deviceIdColumn, barcodeColumn, scanTimeColumn
        // , portNameColumn
        );
        return table;
    }

    private TableView<BurnResultData> createBurnResultTable() {
        TableView<BurnResultData> table = new TableView<>();
        table.setItems(burnResultDataList);
        table.setPrefHeight(200); // 设置表格高度

        TableColumn<BurnResultData, String> barcodeColumn = new TableColumn<>("条码内容");
        barcodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBarcode()));
        barcodeColumn.setPrefWidth(200);

        TableColumn<BurnResultData, String> statusColumn = new TableColumn<>("烧录状态");
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().isSuccess() ? "成功" : "失败"));
        statusColumn.setPrefWidth(100);

        TableColumn<BurnResultData, String> messageColumn = new TableColumn<>("消息");
        messageColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getMessage()));
        messageColumn.setPrefWidth(300);

        table.getColumns().addAll(barcodeColumn, statusColumn, messageColumn);
        return table;
    }

    private void startStatusUpdateThread() {
        // 启动状态更新线程
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(() -> {
                        actualBarcodeCount.set(String.valueOf(barcodeDataList.size()));
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    /**
     * 显示上位机配置对话框
     */
    private void showUpperComputerConfigDialog() {
        // 创建对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("编辑上位机配置");
        dialog.setHeaderText("请输入上位机的IP地址和端口号");

        // 设置对话框按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 创建表单布局
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // 创建输入字段
        TextField ipTextField = new TextField();
        TextField portTextField = new TextField();

        // 从配置服务加载现有配置
        try {
            String savedIp = ConfigService.getInstance().getConfigValueByKey(CONFIG_KEY_UPPER_COMPUTER_IP);
            String savedPort = ConfigService.getInstance().getConfigValueByKey(CONFIG_KEY_UPPER_COMPUTER_PORT);

            ipTextField.setText(savedIp != null ? savedIp : DEFAULT_UPPER_COMPUTER_IP);
            portTextField.setText(savedPort != null ? savedPort : DEFAULT_UPPER_COMPUTER_PORT);
        } catch (Exception e) {
            log("[配置加载错误] 从上位机配置服务加载配置失败: " + e.getMessage());
            // 使用默认值
            ipTextField.setText(DEFAULT_UPPER_COMPUTER_IP);
            portTextField.setText(DEFAULT_UPPER_COMPUTER_PORT);
        }

        // 添加标签和输入字段到网格
        grid.add(new Label("IP地址:"), 0, 0);
        grid.add(ipTextField, 1, 0);
        grid.add(new Label("端口号:"), 0, 1);
        grid.add(portTextField, 1, 1);

        // 设置对话框内容
        dialog.getDialogPane().setContent(grid);

        // 验证输入
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(
                Bindings.createBooleanBinding(() -> {
                    String ip = ipTextField.getText().trim();
                    String port = portTextField.getText().trim();

                    // 简单的IP地址验证
                    boolean validIp = !ip.isEmpty() && ip.matches("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");
                    // 简单的端口验证
                    boolean validPort = !port.isEmpty();
                    try {
                        if (validPort) {
                            int portNum = Integer.parseInt(port);
                            validPort = portNum > 0 && portNum <= 65535;
                        }
                    } catch (NumberFormatException e) {
                        validPort = false;
                    }

                    return !validIp || !validPort;
                }, ipTextField.textProperty(), portTextField.textProperty()));

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(result -> {
            if (result == saveButtonType) {
                String ip = ipTextField.getText().trim();
                String port = portTextField.getText().trim();

                try {
                    // 保存IP地址配置
                    saveUpperComputerConfig(CONFIG_KEY_UPPER_COMPUTER_IP, ip, "上位机IP地址", "STRING");
                    // 保存端口配置
                    saveUpperComputerConfig(CONFIG_KEY_UPPER_COMPUTER_PORT, port, "上位机端口号", "INTEGER");

                    log("[配置更新] 成功保存上位机配置 - IP: " + ip + ", 端口: " + port);
                    showSuccessMessage("配置保存成功");
                } catch (Exception e) {
                    log("[配置保存错误] 保存上位机配置失败: " + e.getMessage());
                    e.printStackTrace();
                    showSuccessMessage("配置保存失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 保存上位机配置到配置服务
     */
    private void saveUpperComputerConfig(String configKey, String configValue, String description, String dataType) {
        try {
            // 先检查配置项是否已存在
            String existingValue = ConfigService.getInstance().getConfigValueByKey(configKey);
            ConfigItem configItem = null;

            if (existingValue != null) {
                // 如果已存在，获取现有配置项
                try {
                    List<ConfigItem> allConfigs = DatabaseManager.getAllConfigItems();
                    for (ConfigItem item : allConfigs) {
                        if (item.getConfigKey().equals(configKey)) {
                            configItem = item;
                            configItem.setConfigValue(configValue); // 更新值
                            break;
                        }
                    }
                } catch (SQLException e) {
                    // 如果获取现有配置项失败，创建新的配置项
                    log("[配置检查] 获取现有配置项失败: " + e.getMessage() + ", 将创建新配置项");
                    configItem = new ConfigItem(
                            configKey,
                            configValue,
                            description,
                            dataType,
                            true // 设置为必填配置项
                    );
                }
            } else {
                // 如果不存在，创建新配置项
                configItem = new ConfigItem(
                        configKey,
                        configValue,
                        description,
                        dataType,
                        true // 设置为必填配置项
                );
            }

            ConfigService.getInstance().saveConfigItem(configItem);
            log("[配置保存] 已保存上位机配置 - 键: " + configKey + ", 值: " + configValue);
        } catch (Exception e) {
            log("[配置保存错误] 保存上位机配置失败: " + e.getMessage());
            throw new RuntimeException("保存上位机配置失败", e);
        }
    }

    /**
     * 显示成功消息
     */
    private void showSuccessMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("成功");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * 显示EMS配置对话框
     */
    private void showEmsConfigDialog() {
        // 创建对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("编辑EMS配置");
        dialog.setHeaderText("请输入EMS系统的API URL");

        // 设置对话框按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 创建表单布局
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // 创建URL输入字段
        TextField urlTextField = new TextField();

        // 从配置服务加载现有配置
        try {
            String savedUrl = ConfigService.getInstance().getConfigValueByKey(CONFIG_KEY_EMS_URL);
            urlTextField.setText(savedUrl != null ? savedUrl : DEFAULT_EMS_URL);
        } catch (Exception e) {
            log("[配置加载错误] 从EMS配置服务加载配置失败: " + e.getMessage());
            // 使用默认值
            urlTextField.setText(DEFAULT_EMS_URL);
        }

        // 添加标签和输入字段到网格
        grid.add(new Label("EMS API URL:"), 0, 0);
        grid.add(urlTextField, 1, 0);

        // 设置对话框内容
        dialog.getDialogPane().setContent(grid);

        // 验证输入
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.disableProperty().bind(
                Bindings.createBooleanBinding(() -> {
                    String url = urlTextField.getText().trim();

                    // 简化的URL验证
                    boolean validUrl = !url.isEmpty() && (url.startsWith("http://") || url.startsWith("https://"));

                    return !validUrl;
                }, urlTextField.textProperty()));

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(result -> {
            if (result == saveButtonType) {
                String url = urlTextField.getText().trim();

                try {
                    // 保存EMS URL配置
                    saveEmsConfig(CONFIG_KEY_EMS_URL, url, "EMS系统API地址", "STRING");

                    log("[配置更新] 成功保存EMS配置 - URL: " + url);
                    showSuccessMessage("EMS配置保存成功");
                } catch (Exception e) {
                    log("[配置保存错误] 保存EMS配置失败: " + e.getMessage());
                    e.printStackTrace();
                    showSuccessMessage("EMS配置保存失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 保存EMS配置到配置服务
     */
    private void saveEmsConfig(String configKey, String configValue, String description, String dataType) {
        try {
            // 先检查配置项是否已存在
            String existingValue = ConfigService.getInstance().getConfigValueByKey(configKey);
            ConfigItem configItem = null;

            if (existingValue != null) {
                // 如果已存在，获取现有配置项
                try {
                    List<ConfigItem> allConfigs = DatabaseManager.getAllConfigItems();
                    for (ConfigItem item : allConfigs) {
                        if (item.getConfigKey().equals(configKey)) {
                            configItem = item;
                            configItem.setConfigValue(configValue); // 更新值
                            break;
                        }
                    }
                } catch (SQLException e) {
                    // 如果获取现有配置项失败，创建新的配置项
                    log("[配置检查] 获取现有EMS配置项失败: " + e.getMessage() + ", 将创建新配置项");
                    configItem = new ConfigItem(
                            configKey,
                            configValue,
                            description,
                            dataType,
                            true // 设置为必填配置项
                    );
                }
            } else {
                // 如果不存在，创建新配置项
                configItem = new ConfigItem(
                        configKey,
                        configValue,
                        description,
                        dataType,
                        true // 设置为必填配置项
                );
            }

            ConfigService.getInstance().saveConfigItem(configItem);
            log("[配置保存] 已保存EMS配置 - 键: " + configKey + ", 值: " + configValue);
        } catch (Exception e) {
            log("[配置保存错误] 保存EMS配置失败: " + e.getMessage());
            throw new RuntimeException("保存EMS配置失败", e);
        }
    }

    /**
     * 显示网络配置对话框的通用方法
     * @param serviceType 服务类型（烧录机、扫码机、PLC）
     * @param title 对话框标题
     * @param headerText 对话框头信息
     * @param typeName 类型名称（用于日志记录）
     */
    private void showNetworkConfigDialog(TcpServiceEnum serviceType, String title, String headerText, String typeName) {
        try {
            // 创建对话框
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle(title);
            dialog.setHeaderText(headerText);
            
            // 设置对话框按钮
            ButtonType closeButtonType = new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(closeButtonType);
            
            // 创建配置面板并设置为对话框内容
            NetworkConfigPanel configPanel = new NetworkConfigPanel(serviceType);
            dialog.getDialogPane().setContent(configPanel);
            
            // 设置对话框大小
            dialog.getDialogPane().setPrefWidth(500);
            dialog.getDialogPane().setPrefHeight(400);
            
            // 显示对话框
            dialog.showAndWait();
        } catch (Exception e) {
            log("[配置加载错误] 打开" + typeName + "配置面板失败: " + e.getMessage());
            showSuccessMessage("打开" + typeName + "配置面板失败: " + e.getMessage());
        }
    }

    /**
     * 显示烧录机配置对话框
     */
    private void showBurnerConfigDialog() {
        showNetworkConfigDialog(
            TcpServiceEnum.BURNER,
            "编辑TCP服务配置",
            "配置TCP服务的网络参数",
            "TCP服务"
        );
    }

    // /**
    //  * 显示扫码机配置对话框
    //  */
    // private void showScannerConfigDialog() {
    //     showNetworkConfigDialog(
    //         TcpServiceEnum.SCANNER,
    //         "编辑扫码机配置",
    //         "配置扫码机的网络参数",
    //         "扫码机"
    //     );
    // }
    
    // /**
    //  * 显示PLC配置对话框
    //  */
    // private void showPlcConfigDialog() {
    //     showNetworkConfigDialog(
    //         TcpServiceEnum.PLC,
    //         "编辑PLC配置",
    //         "配置PLC的网络参数",
    //         "PLC"
    //     );
    // }

    /**
     * 显示三方软件配置对话框
     */
    private void showThirdPartyConfigDialog() {
        // 创建对话框
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("三方软件配置");
        dialog.setHeaderText("设置三方软件路径和启动参数");

        // 设置对话框按钮
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 创建表单布局
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // 创建输入字段
        TextField pathTextField = new TextField();
        TextField argsTextField = new TextField();

        // 从配置服务加载现有配置
        try {
            String savedPath = ConfigService.getInstance().getConfigValueByKey(CONFIG_KEY_THIRD_PARTY_PATH);
            pathTextField.setText(savedPath != null ? savedPath : DEFAULT_THIRD_PARTY_PATH);

            String savedArgs = ConfigService.getInstance().getConfigValueByKey(CONFIG_KEY_THIRD_PARTY_ARGS);
            argsTextField.setText(savedArgs != null ? savedArgs : DEFAULT_THIRD_PARTY_ARGS);
        } catch (Exception e) {
            log("[配置加载错误] 从配置服务加载三方软件配置失败: " + e.getMessage());
            // 使用默认值
            pathTextField.setText(DEFAULT_THIRD_PARTY_PATH);
            argsTextField.setText(DEFAULT_THIRD_PARTY_ARGS);
        }

        // 添加标签和输入字段到网格
        grid.add(new Label("软件路径:"), 0, 0);
        grid.add(pathTextField, 1, 0);
        grid.add(new Label("启动参数:"), 0, 1);
        grid.add(argsTextField, 1, 1);

        // 设置对话框内容
        dialog.getDialogPane().setContent(grid);

        // 验证输入 - 三方软件配置允许空值，所以不需要禁用保存按钮

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(result -> {
            if (result == saveButtonType) {
                String path = pathTextField.getText().trim();
                String args = argsTextField.getText().trim();

                try {
                    // 保存三方软件配置
                    saveConfig(CONFIG_KEY_THIRD_PARTY_PATH, path, "三方软件路径", "STRING");
                    saveConfig(CONFIG_KEY_THIRD_PARTY_ARGS, args, "三方软件启动参数", "STRING");

                    log("[配置更新] 成功保存三方软件配置 - 路径: " + path + ", 参数: " + args);
                    showSuccessMessage("三方软件配置保存成功");
                } catch (Exception e) {
                    log("[配置保存错误] 保存三方软件配置失败: " + e.getMessage());
                    e.printStackTrace();
                    showSuccessMessage("三方软件配置保存失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 通用的配置保存方法
     */
    private void saveConfig(String configKey, String configValue, String description, String dataType) {
        try {
            // 先检查配置项是否已存在
            String existingValue = ConfigService.getInstance().getConfigValueByKey(configKey);
            ConfigItem configItem = null;

            if (existingValue != null) {
                // 如果已存在，获取现有配置项
                try {
                    List<ConfigItem> allConfigs = DatabaseManager.getAllConfigItems();
                    for (ConfigItem item : allConfigs) {
                        if (item.getConfigKey().equals(configKey)) {
                            configItem = item;
                            configItem.setConfigValue(configValue); // 更新值
                            break;
                        }
                    }
                } catch (SQLException e) {
                    // 如果获取现有配置项失败，创建新的配置项
                    log("[配置检查] 获取现有配置项失败: " + e.getMessage() + ", 将创建新配置项");
                    configItem = new ConfigItem(
                            configKey,
                            configValue,
                            description,
                            dataType,
                            false // 一般配置项设为非必填
                    );
                }
            } else {
                // 如果不存在，创建新配置项
                configItem = new ConfigItem(
                        configKey,
                        configValue,
                        description,
                        dataType,
                        false // 一般配置项设为非必填
                );
            }

            ConfigService.getInstance().saveConfigItem(configItem);
            log("[配置保存] 已保存配置 - 键: " + configKey + ", 值: " + configValue);
        } catch (Exception e) {
            log("[配置保存错误] 保存配置失败: " + e.getMessage());
            throw new RuntimeException("保存配置失败", e);
        }
    }

    private void startProcess() {
        log("[操作] 用户点击了'启动流程'按钮");
        if (processStarted.get()) {
            log("[操作结果] 流程已经启动，请先重置流程");
            return;
        }

        // 启动流程
        processStarted.set(true);
        barcodeVerified.set(false);
        waitingForStartCommand.set(false);
        programCommandSent.set(false);
        waitingForProgramResult.set(false);
        processCompleted.set(false);
        currentStatus.set("运行中");
        log("[操作结果] 流程已启动，请扫描条码...");
        log("[流程状态] 当前流程状态：运行中");
    }

    private void resetProcess() {
        log("[操作] 用户点击了'重置流程'按钮");
        // 不设置processStarted为false，保持流程运行状态
        barcodeVerified.set(false);
        waitingForStartCommand.set(false);
        programCommandSent.set(false);
        waitingForProgramResult.set(false);
        processCompleted.set(false);
        currentStatus.set("运行中");
        // 保留预期条码数量不变
        // 调用clearBarcodes方法清空所有条码数据
        clearBarcodes();
        log("[操作结果] 流程已重置，保持运行状态");
        log("[流程状态] 当前流程状态：运行中，重置了所有流程标志");
    }

    private void clearBarcodes() {
        log("[操作] 用户点击了'清空条码'按钮");
        barcodeDataList.clear();
        // burnResultDataList.clear();
        currentBarcodes.clear();
        log("[操作结果] 条码缓存已清空");
        log("[流程状态] 当前流程状态保持不变");
    }

    // // 烧录机配置相关常量
    // private static final String CONFIG_KEY_BURNER_IP = "burner.ip";
    // private static final String CONFIG_KEY_BURNER_PORT = "burner.port";
    // private static final String CONFIG_KEY_BURNER_TIMEOUT = "burner.timeout";
    // private static final String DEFAULT_BURNER_IP = "127.0.0.1";
    // private static final String DEFAULT_BURNER_PORT = "8888";
    // private static final String DEFAULT_BURNER_TIMEOUT = "30000";

    // // 扫码机配置相关常量
    // private static final String CONFIG_KEY_SCANNER_COM_PORTS = "scanner.com_ports";
    // private static final String CONFIG_KEY_SCANNER_BAUD_RATE = "scanner.baud_rate";
    // private static final String DEFAULT_SCANNER_COM_PORTS = "COM1,COM2,COM3";
    // private static final String DEFAULT_SCANNER_BAUD_RATE = "9600";

    // 三方软件配置相关常量
    private static final String CONFIG_KEY_THIRD_PARTY_PATH = "third_party.path";
    private static final String CONFIG_KEY_THIRD_PARTY_ARGS = "third_party.args";
    private static final String DEFAULT_THIRD_PARTY_PATH = "";
    private static final String DEFAULT_THIRD_PARTY_ARGS = "";

    /**
     * 创建菜单栏，包含网络配置、编辑上位机配置和编辑EMS配置功能
     */
    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // 创建烧录机配置菜单
        Menu burnerMenu = new Menu("TCP配置");
        MenuItem burnerConfigItem = new MenuItem("编辑TCP配置");
        burnerConfigItem.setOnAction(e -> showBurnerConfigDialog());
        burnerMenu.getItems().add(burnerConfigItem);

        // // 创建扫码机配置菜单
        // Menu scannerMenu = new Menu("扫码机配置");
        // MenuItem scannerConfigItem = new MenuItem("编辑扫码机配置");
        // scannerConfigItem.setOnAction(e -> showScannerConfigDialog());
        // scannerMenu.getItems().add(scannerConfigItem);
        
        // // 创建PLC配置菜单
        // Menu plcMenu = new Menu("PLC配置");
        // MenuItem plcConfigItem = new MenuItem("编辑PLC配置");
        // plcConfigItem.setOnAction(e -> showPlcConfigDialog());
        // plcMenu.getItems().add(plcConfigItem);

        // 创建三方软件配置菜单
        Menu thirdPartyMenu = new Menu("三方软件配置");
        MenuItem thirdPartyConfigItem = new MenuItem("编辑三方软件配置");
        thirdPartyConfigItem.setOnAction(e -> showThirdPartyConfigDialog());
        thirdPartyMenu.getItems().add(thirdPartyConfigItem);

        // 将菜单添加到菜单栏
        menuBar.getMenus().addAll(
                // configMenu,
                burnerMenu,
                // scannerMenu,
                // plcMenu,
                thirdPartyMenu);

        return menuBar;
    }

    /**
     * 处理PLC数据
     * @param value
     */
    protected void plcProcessBarcodeData(String value) {
        log("[PLC数据] " + value);
        //判断信息是否是：burner.qty.query.response配置的接收指令
        ConfigService configService = ConfigService.getInstance();
        String receiveCommand = configService.getConfigValueByKey("burner.qty.query.response");
        if (value.contains(receiveCommand)) {
            log("[烧录机] 收到数量响应: " + value+";前缀："+receiveCommand);
            // 提取条码数量,把value中的receiveCommand替换为空字符串
            String qty = value.replaceAll(receiveCommand, "").trim();
            log("[操作结果] 烧录机条码数量: " + qty);
            //把条码数量更新到配置服务
            int intQty = Integer.parseInt(qty);
            expectedBarcodeCount.set(String.valueOf(intQty));
            expectedBarcodeCountInput.setText(String.valueOf(intQty));
            applyExpectedCount();//应用条码数量。
            log("[配置更新] 已将预期条码数量更新为当前条码数量: " + intQty);
        }
    }
    /**
     * 处理烧录机信息数据
     * @param value
     */
    protected void burnerProcessBarcodeData(String value) {
        log("[烧录机信息] " + value);
        
    }
    /**
     * 处理条码数据的封装方法
     * @param barcode 条码内容
     * @param portName 端口名称
     * @param source 条码来源标识（用于日志记录）
     */
    private void scannerProcessBarcodeData(String barcode) throws SQLException{
        if (!processStarted.get()) {
            log("[扫码机] 请先启动流程");
            return;
        }

        if (barcode == null || barcode.isEmpty()) {
            log("[扫码机] 无效的数据");
            return;
        }
        
        //判断是否是
        // 防重逻辑：检查当前条码是否已存在
        if (currentBarcodes.contains(barcode)) {
            log("[扫码机] 条码已存在，跳过处理: " + barcode);
            return;
        }

        // 创建条码数据对象
        BarcodeData barcodeData = new BarcodeData(deviceId, barcode, "NETWORK_PORT");
        // 保存条码数据到数据库
        DatabaseManager.saveBarcodeData(barcodeData);
        // 添加到缓存
        barcodeDataList.add(barcodeData);
        currentBarcodes.add(barcode);

        log("[扫码机] 条码: " + barcode);
        log("[扫码机] 当前条码数量: " + barcodeDataList.size() + ", 预期条码数量: " + expectedBarcodeCount.get());
        
        // 调用方法获取烧录机条码数并比较
        getBurnerBarcodeCount();
        int burnerBarcodeCount = Integer.parseInt(expectedBarcodeCount.get());
        log("[扫码机] 烧录机条码数: " + burnerBarcodeCount);
        if (burnerBarcodeCount >= 0) {
            if (burnerBarcodeCount > currentBarcodes.size()) {
                log("[扫码机] 条码数量不一致，继续处理");
            } else if (burnerBarcodeCount == currentBarcodes.size()) {
                log("[扫码机] 条码数量一致，执行PLC-OK指令");
                //从配置服务获取PLC-OK指令
                ConfigService configService = ConfigService.getInstance();
                String plcOkCommand = configService.getConfigValueByKey("plc.tcp.ok.command");
                // 发送PLC-OK指令
                NetworkService networkService = NetworkService.getInstance();
                networkService.sendData(plcOkCommand, TcpServiceEnum.PLC);
                log("[操作] 成功发送OK指令到PLC: " + plcOkCommand);
                sendBarcodeToBurner();
                // 重置当前条码列表
                // currentBarcodes.clear();
                //清空条码数据
                // barcodeDataList.clear();
                // //清空当前条码列表
                // currentBarcodes.clear();
            }else{
                log("[错误] 数量错误: " + burnerBarcodeCount+",当前条码数量: "+currentBarcodes.size());
                //发送PLC-ERROR指令
                ConfigService configService = ConfigService.getInstance();
                String plcErrorCommand = configService.getConfigValueByKey("plc.tcp.error.command");
                // 发送PLC-ERROR指令
                NetworkService networkService = NetworkService.getInstance();
                networkService.sendData(plcErrorCommand, TcpServiceEnum.PLC);
                log("[操作] 发送NG指令到PLC: " + plcErrorCommand);
            }
        }
        
    }
    /**
     * 发送当前条码列表到烧录机
     */
    private void  sendBarcodeToBurner(){
        log("[流程] 发送条码列表到烧录机: " );
        String barcodesStr = getBarcodesStr();
        // 通过NetworkService发送指令到烧录机
        NetworkService networkService = NetworkService.getInstance();
        networkService.sendData(barcodesStr, TcpServiceEnum.BURNER);
        log("[操作] 条码数据: " + barcodesStr);
    }
    //把当前条码列表打包成json字符串
    private String packBarcodesToJson() {
       JSONObject jsonObject = new JSONObject();
       jsonObject.put("barcodes", currentBarcodes);
       return jsonObject.toString();
    }
    /**
     * 把当前条码列表转换为逗号分隔的字符串
     * @return 逗号分隔的条码字符串
     */
    private String getBarcodesStr(){
        return String.join(",", currentBarcodes);
    }
    /**
     * 获取烧录机条码数
     * 通过TCP发送指令并获取返回结果
     * @return 条码数量
     */
    private void getBurnerBarcodeCount() {
        try {
            // 从配置服务获取发送指令
            ConfigService configService = ConfigService.getInstance();
            String sendCommand = configService.getConfigValueByKey("burner.qty.query.command");
            // //判断是否为空
            // if (sendCommand == null || sendCommand.isEmpty()) {
            //     ConfigItem configItem = new ConfigItem();
            //     configItem.setConfigKey("burner.qty.query.command");
            //     configItem.setConfigValue("");
            //     // configItem.setConfigType("String");
            //     configItem.setDataType("String");
            //     configItem.setConfigDescription("烧录机数量查询指令");
            //     configService.addConfigItem(configItem);
            //     log("[错误]"+configItem.getConfigKey()+" 配置中未指定烧录机数量查询指令");
            //     return -1;
            // }
            // 通过NetworkService发送指令到烧录机
            NetworkService networkService = NetworkService.getInstance();
            networkService.sendData(sendCommand, TcpServiceEnum.BURNER);
            log("[操作] 发送指令到烧录机: " + sendCommand);
        } catch (Exception e) {
            log("[错误] 获取烧录机条码数失败: " + e.getMessage());
        }
    }
    /**
     * 处理手动输入的条码
     */
    private void handleManualBarcodeInput() {
        log("[操作] 用户点击了'确认输入'按钮");
        
        String barcode = barcodeInputField.getText().trim();
        try {
            scannerProcessBarcodeData(barcode);
        } catch (SQLException e) {
            log("[错误] 处理条码数据失败: " + e.getMessage());
        }
        // 清除输入框
        barcodeInputField.clear();
    }
    /**
     * 模拟PLC开始指令处理
     */
    private void startComPortMonitoring() {
        log("[操作] 用户点击了'开始监控'按钮");
        if (!processStarted.get()) {
            log("[操作结果] 请先启动流程");
            return;
        }

        if (isMonitoringComPort) {
            log("[操作结果] 串口监控已经在运行中");
            return;
        }

        String selectedPort = comPortComboBox.getValue();
        isMonitoringComPort = true;
        startComMonitorButton.setDisable(true);
        stopComMonitorButton.setDisable(false);
        comPortComboBox.setDisable(true);

        log("[操作结果] 开始监控串口: " + selectedPort);

        // 执行COM口有效性检查
        log("[COM口检查] 正在验证串口有效性...");
        try {
            // 使用默认参数初始化串口进行验证
            int baudRate = 9600;
            int dataBits = 8;
            int stopBits = 1;
            int parity = 0; // 无校验

            // 检查串口是否有效
            boolean portValid = serialPortService.initSerialPort("monitor_device", selectedPort,
                    baudRate, dataBits, stopBits, parity);

            if (portValid) {
                log("[COM口检查] 串口" + selectedPort + " 验证成功");
                serialPortStatus.set("已连接");
                log("[提示] 串口监控已启动，请使用'模拟COM口数据'按钮手动输入数据");
            } else {
                log("[COM口检查] 串口" + selectedPort + " 验证失败");
                serialPortStatus.set("连接失败");
                log("[提示] 请检查串口连接或尝试其他COM端口");
            }

            // 验证完成后关闭测试连接（实际监控由事件监听器处理）
            serialPortService.closeSerialPort("monitor_device");
        } catch (Exception e) {
            log("[COM口检查] 验证过程发生错误: " + e.getMessage());
            serialPortStatus.set("检查错误");
        }
    }

    /**
     * 模拟PLC开始指令处理
     */
    private void stopComPortMonitoring() {
        log("[操作] 用户点击了'停止监控'按钮");
        if (!isMonitoringComPort) {
            log("[操作结果] 串口监控未运行");
            return;
        }

        isMonitoringComPort = false;
        startComMonitorButton.setDisable(false);
        stopComMonitorButton.setDisable(true);
        comPortComboBox.setDisable(false);

        log("[操作结果] 停止监控串口: " + comPortComboBox.getValue());
    }

        /**
         * 模拟PLC产品数量处理
         */
    private void simulatePlcProductCount() {
        log("[操作] 用户点击了'模拟PLC数量'按钮");
        if (!processStarted.get() || barcodeVerified.get()) {
            log("[操作结果] 请先启动流程并扫描条码，或条码已经验证通过");
            return;
        }

        // 使用用户设置的预期数量
        int actualCount = barcodeDataList.size();
        String expectedCountStr = expectedBarcodeCount.get();

        try {
            int count = Integer.parseInt(expectedCountStr);
            log("[PLC数据] 接收到PLC产品数量: " + count);

            // 验证条码数量
            if (actualCount == count) {
                barcodeVerified.set(true);
                currentStatus.set("验证通过");
                log("[验证结果] 条码数量验证通过: " + actualCount + " = " + count);
                waitingForStartCommand.set(true);
                log("[流程状态] 等待PLC开始指令...");
            } else {
                log("[验证结果] 错误: 条码数量不匹配! 实际: " + actualCount + " 预期: " + count);
                currentStatus.set("异常");
                log("[流程状态] 进入异常状态，准备重置流程...");
                resetProcess(); // 重置但保持流程运行状态
            }
        } catch (NumberFormatException e) {
            log("[验证结果] 预期条码数量格式错误，请输入有效的数字");
            currentStatus.set("异常");
            log("[流程状态] 进入异常状态，准备重置流程...");
            resetProcess(); // 重置但保持流程运行状态
        }
    }

    /**
     * 模拟PLC开始指令处理
     */
    private void simulatePlcStartCommand() {
        log("[操作] 用户点击了'模拟PLC开始'按钮");
        if (!processStarted.get() || !barcodeVerified.get() || !waitingForStartCommand.get()
                || programCommandSent.get()) {
            log("[操作结果] 请先完成条码验证步骤");
            return;
        }

        log("[PLC指令] 接收到PLC开始指令，准备发送烧录指令给上位机...");
        currentStatus.set("发送烧录指令");
        log("[流程状态] 当前流程状态：发送烧录指令");

        // 模拟发送烧录指令
        programCommandSent.set(true);
        waitingForProgramResult.set(true);
        currentStatus.set("等待烧录结果");
        log("[流程状态] 当前流程状态：等待烧录结果");

        // 模拟上位机烧录结果
        log("[系统操作] 开始模拟上位机烧录结果处理...");
        simulateUpperComputerResult();
    }

    /**
     * 模拟上位机烧录结果处理
     */
    private void simulateUpperComputerResult() {
        // 模拟延迟
        new Thread(() -> {
            try {
                Thread.sleep(2000); // 模拟2秒烧录时间

                Platform.runLater(() -> {
                    if (processStarted.get() && programCommandSent.get() && waitingForProgramResult.get()) {
                        log("[上位机数据] 收到上位机烧录结果");
                        currentStatus.set("处理结果");
                        log("[流程状态] 当前流程状态：处理结果");

                        // 清空之前的结果
                        burnResultDataList.clear();
                        log("[数据操作] 清空之前的烧录结果");

                        // 创建批次ID
                        String batchId = UUID.randomUUID().toString();
                        log("[数据操作] 创建新批次ID: " + batchId);

                        // 创建ProgramResult对象，准备回传给EMS
                        ProgramResult programResult = new ProgramResult();
                        programResult.setBatchId(batchId);
                        programResult.setCompleteTime(LocalDateTime.now());
                        log("[数据操作] 创建ProgramResult对象，准备处理结果数据");

                        // 模拟生成烧录结果
                        boolean allSuccess = true;
                        LocalDateTime now = LocalDateTime.now();
                        log("[系统操作] 开始为每个条码生成烧录结果...");

                        for (String barcode : currentBarcodes) {
                            boolean success = random.nextDouble() > 0.1; // 90%成功率
                            String message = success ? "烧录成功" : "烧录失败，未知错误";

                            burnResultDataList.add(new BurnResultData(barcode, success, message));
                            log("[烧录结果] 条码: " + barcode + ", 状态: " + (success ? "成功" : "失败") + ", 消息: " + message);

                            // 添加设备结果到ProgramResult
                            DeviceResult deviceResult = new DeviceResult(deviceId, barcode, success, message);
                            programResult.addDeviceResult(deviceResult);
                            log("[数据操作] 添加设备结果到ProgramResult对象");

                            // 保存到数据库
                            try {
                                DatabaseManager.saveProgramResult(batchId, deviceId, barcode, success, message, now);
                                log("[数据库操作] 成功保存烧录结果到数据库");
                            } catch (SQLException e) {
                                log("[数据库错误] 保存烧录结果到数据库失败: " + e.getMessage());
                            }

                            if (!success) {
                                allSuccess = false;
                            }
                        }

                        // 设置整体状态
                        programResult.setStatus(allSuccess ? "success" : "partial_failure");
                        log("[数据操作] 设置批次整体状态为: " + programResult.getStatus());

                        // 回传EMS
                        try {
                            EmsService.getInstance().sendProgramResult(programResult);
                            log("[EMS操作] 烧录结果已成功回传给EMS系统");
                        } catch (Exception e) {
                            log("[EMS错误] 烧录结果回传EMS系统失败: " + e.getMessage());
                        }

                        // 流程完成
                        currentStatus.set(allSuccess ? "完成" : "部分失败");
                        waitingForProgramResult.set(false);
                        processCompleted.set(true);
                        log("[流程状态] 当前流程状态：" + (allSuccess ? "完成" : "部分失败"));

                        if (allSuccess) {
                            log("[流程完成] 流程执行完成！");
                            log("[流程统计] 总共处理条码数量: " + currentBarcodes.size() + "，全部成功");
                        } else {
                            log("[流程完成] 警告: 流程执行完成，但部分条码烧录失败！");
                            log("[流程统计] 总共处理条码数量: " + currentBarcodes.size());
                        }
                        // 重置流程
                        resetProcess();

                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("[系统错误] 模拟上位机结果处理线程被中断");
            }
        }).start();
    }

    /**
     * 记录日志到UI和控制台
     * @param message 日志消息
     */
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = "log：[" + timestamp + "] " + message;

        Platform.runLater(() -> {
            logArea.appendText(logMessage + "\n");
            // 自动滚动到底部
            logArea.setScrollTop(Double.MAX_VALUE);
        });

        System.out.println(logMessage);
    }

    // 为面板初始化添加日志记录
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        // 只在初始化时记录一次
        if (!isInitialized) {
            log("[面板初始化] 自动处理面板UI组件布局完成");
            isInitialized = true;
        }
    }

    private boolean isInitialized = false;

    /**
     * 显示模拟COM口数据的输入弹窗
     */
    private void showComDataInputDialog() {
        if (!isMonitoringComPort) {
            log("[操作结果] 请先开始串口监控");
            return;
        }

        String selectedPort = comPortComboBox.getValue();

        // 创建对话框
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("模拟COM口数据");
        dialog.setHeaderText("请输入要模拟的COM口数据 (当前端口: " + selectedPort + ")");

        // 设置对话框按钮
        ButtonType confirmButtonType = new ButtonType("确认", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        // 创建输入框
        TextField dataTextField = new TextField();
        dataTextField.setPromptText("输入条码数据，例如: BAR-123456");

        // 添加输入框到对话框
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        grid.add(new Label("条码数据:"), 0, 0);
        grid.add(dataTextField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        // 请求焦点到输入框
        Platform.runLater(() -> dataTextField.requestFocus());

        // 设置结果转换器
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) {
                return dataTextField.getText();
            }
            return null;
        });

        // 显示对话框并处理结果
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(inputData -> {
            if (inputData != null && !inputData.trim().isEmpty()) {
                String barcode = inputData.trim();

                // 模拟COM口数据接收
                Platform.runLater(() -> {
                    BarcodeData barcodeData = new BarcodeData(deviceId, barcode, selectedPort);
                    barcodeDataList.add(barcodeData);
                    currentBarcodes.add(barcode);
                    log("[串口数据] 从串口" + selectedPort + "模拟接收到条码: " + barcode);
                    log("[数据状态] 当前条码数量: " + barcodeDataList.size() + ", 预期条码数量: " + expectedBarcodeCount.get());
                });
            } else {
                log("[操作结果] 输入的COM口数据不能为空");
            }
        });
    }


    /**
     * 模拟扫描条码
     */
    private void simulateBarcodeScan() {
        log("[操作] 用户点击了'模拟扫描'按钮");
        if (!processStarted.get()) {
            log("[操作结果] 请先启动流程");
            return;
        }

        // 生成随机条码
        String randomBarcode = "BAR-" + System.currentTimeMillis() + "-" + random.nextInt(1000);

        // 创建条码数据对象
        BarcodeData barcodeData = new BarcodeData(deviceId, randomBarcode, comPortComboBox.getValue());

        // 添加到缓存
        barcodeDataList.add(barcodeData);
        currentBarcodes.add(randomBarcode);

        log("[操作结果] 扫描到条码: " + randomBarcode);
        log("[数据状态] 当前条码数量: " + barcodeDataList.size() + ", 预期条码数量: " + expectedBarcodeCount.get());
    }

}