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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.sql.SQLException;
import java.util.UUID;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.service.EmsService;
import com.iot.plc.model.ProgramResult;
import com.iot.plc.model.DeviceResult;

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
    
    // 状态管理
    private StringProperty currentStatus = new SimpleStringProperty("空闲");
    private StringProperty serialPortStatus = new SimpleStringProperty("未连接");
    private StringProperty plcStatus = new SimpleStringProperty("未连接");
    private StringProperty upperComputerStatus = new SimpleStringProperty("未连接");
    private StringProperty emsStatus = new SimpleStringProperty("未连接");
    private StringProperty expectedBarcodeCount = new SimpleStringProperty("0");
    private StringProperty actualBarcodeCount = new SimpleStringProperty("0");
    
    // 数据管理
    private ObservableList<BarcodeData> barcodeDataList = FXCollections.observableArrayList();
    private ObservableList<BurnResultData> burnResultDataList = FXCollections.observableArrayList();
    private final List<String> currentBarcodes = new ArrayList<>();
    private final String deviceId = "PLC_DEVICE_001";
    
    // UI组件
    private TableView<BarcodeData> barcodeTable;
    private TableView<BurnResultData> burnResultTable;
    private TextArea logArea;
    private Button startProcessButton;
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
    
    public AutoProcessPanel() {
        initUI();
        startStatusUpdateThread();
        simulateConnections(); // 模拟连接状态
    }

    private void initUI() {
        // 顶部状态栏
        VBox statusBox = createStatusBox();
        setTop(statusBox);
        
        // 扫描框区域
        VBox scanBox = createScanBox();
        statusBox.getChildren().add(scanBox); // 添加到状态栏下方
        
        // 中部表格区域 - 将条码数据和烧录结果合并到一个界面
        VBox dataPanel = new VBox(10);
        
        // 条码表格
        barcodeTable = createBarcodeTable();
        Label barcodeLabel = new Label("条码数据");
        barcodeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // 烧录结果表格
        burnResultTable = createBurnResultTable();
        Label resultLabel = new Label("烧录结果");
        resultLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        dataPanel.getChildren().addAll(
            barcodeLabel, 
            new ScrollPane(barcodeTable), 
            resultLabel, 
            new ScrollPane(burnResultTable)
        );
        
        // 底部日志和操作按钮
        VBox bottomBox = new VBox(10);
        
        // 操作按钮
        HBox buttonBox = new HBox(10);
        startProcessButton = new Button("启动流程");
        resetProcessButton = new Button("重置流程");
        clearBarcodesButton = new Button("清空条码");
        simulateScanButton = new Button("模拟扫描");
        simulatePlcCountButton = new Button("模拟PLC数量");
        simulatePlcStartButton = new Button("模拟PLC开始");
        
        startProcessButton.setOnAction(e -> startProcess());
        resetProcessButton.setOnAction(e -> resetProcess());
        clearBarcodesButton.setOnAction(e -> clearBarcodes());
        simulateScanButton.setOnAction(e -> simulateBarcodeScan());
        simulatePlcCountButton.setOnAction(e -> simulatePlcProductCount());
        simulatePlcStartButton.setOnAction(e -> simulatePlcStartCommand());
        
        buttonBox.getChildren().addAll(
                startProcessButton, resetProcessButton, clearBarcodesButton,
                simulateScanButton, simulatePlcCountButton, simulatePlcStartButton
        );
        
        // 日志区域
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        ScrollPane logScrollPane = new ScrollPane(logArea);
        
        bottomBox.getChildren().addAll(buttonBox, logScrollPane);
        
        setCenter(dataPanel);
        setBottom(bottomBox);
        
        setPadding(new Insets(10));
    }
    
    private VBox createStatusBox() {
        VBox statusBox = new VBox(5);
        statusBox.setPadding(new Insets(5));
        statusBox.setStyle("-fx-border-color: lightgray; -fx-border-width: 1px; -fx-border-radius: 5px;");
        
        HBox mainStatusBox = new HBox(20);
        mainStatusBox.getChildren().addAll(
                new Label("当前状态: "),
                createStatusLabel(currentStatus),
                new Label("串口状态: "),
                createStatusLabel(serialPortStatus),
                new Label("PLC状态: "),
                createStatusLabel(plcStatus),
                new Label("上位机状态: "),
                createStatusLabel(upperComputerStatus),
                new Label("EMS状态: "),
                createStatusLabel(emsStatus)
        );
        
        HBox barcodeCountBox = new HBox(20);
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
                createStatusLabel(actualBarcodeCount)
        );
        
        statusBox.getChildren().addAll(mainStatusBox, barcodeCountBox);
        return statusBox;
    }
    
    private void applyExpectedCount() {
        log("[操作] 用户点击了'应用'按钮，确认设置预期条码数量");
        try {
            String countText = expectedBarcodeCountInput.getText().trim();
            int count = Integer.parseInt(countText);
            if (count >= 0) {
                expectedBarcodeCount.set(countText);
                log("[操作结果] 已确认设置预期条码数量: " + count);
            } else {
                log("[操作结果] 无效输入：请输入非负整数作为预期条码数量");
                expectedBarcodeCountInput.setText(expectedBarcodeCount.get());
            }
        } catch (NumberFormatException e) {
            log("[操作结果] 无效输入：请输入有效的数字作为预期条码数量");
            expectedBarcodeCountInput.setText(expectedBarcodeCount.get());
        }
    }
    
    private VBox createScanBox() {
        VBox scanBox = new VBox(5);
        scanBox.setPadding(new Insets(5));
        scanBox.setStyle("-fx-border-color: lightblue; -fx-border-width: 1px; -fx-border-radius: 5px; -fx-background-color: #f0f8ff;");
        
        Label scanLabel = new Label("条码输入与串口监控");
        scanLabel.setStyle("-fx-font-weight: bold;");
        
        HBox inputBox = new HBox(10);
        barcodeInputField = new TextField();
        barcodeInputField.setPromptText("手动输入条码...");
        barcodeInputField.setPrefWidth(300);
        
        confirmBarcodeButton = new Button("确认输入");
        confirmBarcodeButton.setOnAction(e -> handleManualBarcodeInput());
        
        // 串口选择和监控
        Label comPortLabel = new Label("COM端口：");
        comPortComboBox = new ComboBox<>();
        comPortComboBox.getItems().addAll("COM1", "COM2", "COM3", "COM4", "COM5");
        comPortComboBox.setValue("COM1");
        
        startComMonitorButton = new Button("开始监控");
        startComMonitorButton.setOnAction(e -> startComPortMonitoring());
        
        stopComMonitorButton = new Button("停止监控");
        stopComMonitorButton.setOnAction(e -> stopComPortMonitoring());
        stopComMonitorButton.setDisable(true);
        
        inputBox.getChildren().addAll(
            scanLabel, barcodeInputField, confirmBarcodeButton,
            comPortLabel, comPortComboBox,
            startComMonitorButton, stopComMonitorButton
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
    
    private TableView<BarcodeData> createBarcodeTable() {
        TableView<BarcodeData> table = new TableView<>();
        table.setItems(barcodeDataList);
        table.setPrefHeight(200); // 设置表格高度
        
        TableColumn<BarcodeData, String> deviceIdColumn = new TableColumn<>("设备ID");
        deviceIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDeviceId()));
        deviceIdColumn.setPrefWidth(100);
        
        TableColumn<BarcodeData, String> barcodeColumn = new TableColumn<>("条码内容");
        barcodeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getBarcode()));
        barcodeColumn.setPrefWidth(200);
        
        TableColumn<BarcodeData, String> scanTimeColumn = new TableColumn<>("扫描时间");
        scanTimeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getScanTime().format(formatter)));
        scanTimeColumn.setPrefWidth(150);
        
        TableColumn<BarcodeData, String> portNameColumn = new TableColumn<>("串口名称");
        portNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPortName()));
        portNameColumn.setPrefWidth(100);
        
        table.getColumns().addAll(deviceIdColumn, barcodeColumn, scanTimeColumn, portNameColumn);
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
    
    private void simulateConnections() {
        // 模拟连接状态（1秒后全部连接）
        new Thread(() -> {
            try {
                Thread.sleep(1000);
                Platform.runLater(() -> {
                    serialPortStatus.set("已连接");
                    plcStatus.set("已连接");
                    upperComputerStatus.set("已连接");
                    emsStatus.set("已连接");
                    log("所有设备连接成功");
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
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
        currentBarcodes.clear();
        log("[操作结果] 流程已重置，保持运行状态");
        log("[流程状态] 当前流程状态：运行中，重置了所有流程标志");
    }
    
    private void clearBarcodes() {
        log("[操作] 用户点击了'清空条码'按钮");
        barcodeDataList.clear();
        burnResultDataList.clear();
        currentBarcodes.clear();
        log("[操作结果] 条码缓存已清空");
        log("[流程状态] 当前流程状态保持不变");
    }
    
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
    
    private void handleManualBarcodeInput() {
        log("[操作] 用户点击了'确认输入'按钮");
        if (!processStarted.get()) {
            log("[操作结果] 请先启动流程");
            return;
        }
        
        String barcode = barcodeInputField.getText().trim();
        if (barcode.isEmpty()) {
            log("[操作结果] 请输入有效的条码");
            return;
        }
        
        // 创建条码数据对象
        BarcodeData barcodeData = new BarcodeData(deviceId, barcode, comPortComboBox.getValue());
        
        // 添加到缓存
        barcodeDataList.add(barcodeData);
        currentBarcodes.add(barcode);
        
        log("[操作结果] 手动输入条码: " + barcode);
        log("[数据状态] 当前条码数量: " + barcodeDataList.size() + ", 预期条码数量: " + expectedBarcodeCount.get());
        barcodeInputField.clear();
    }
    
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
        
        // 模拟串口监控线程
        new Thread(() -> {
            while (isMonitoringComPort) {
                try {
                    // 模拟每3-5秒随机接收到一个条码
                    Thread.sleep(3000 + random.nextInt(2000));
                    
                    if (isMonitoringComPort) { // 再次检查，防止线程启动后立即被停止的情况
                        String randomBarcode = "AUTO-" + System.currentTimeMillis() + "-" + random.nextInt(1000);
                        
                        Platform.runLater(() -> {
                            BarcodeData barcodeData = new BarcodeData(deviceId, randomBarcode, selectedPort);
                            barcodeDataList.add(barcodeData);
                            currentBarcodes.add(randomBarcode);
                            log("[串口数据] 从串口" + selectedPort + "自动读取到条码: " + randomBarcode);
                            log("[数据状态] 当前条码数量: " + barcodeDataList.size() + ", 预期条码数量: " + expectedBarcodeCount.get());
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
    
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
    
    private void simulatePlcStartCommand() {
        log("[操作] 用户点击了'模拟PLC开始'按钮");
        if (!processStarted.get() || !barcodeVerified.get() || !waitingForStartCommand.get() || programCommandSent.get()) {
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
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log("[系统错误] 模拟上位机结果处理线程被中断");
            }
        }).start();
    }
    
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = "[" + timestamp + "] " + message;
        
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
    
    // 内部类：条码数据
    public static class BarcodeData {
        private String deviceId;
        private String barcode;
        private String portName;
        private LocalDateTime scanTime;
        
        public BarcodeData(String deviceId, String barcode, String portName) {
            this.deviceId = deviceId;
            this.barcode = barcode;
            this.portName = portName;
            this.scanTime = LocalDateTime.now();
        }
        
        public String getDeviceId() { return deviceId; }
        public String getBarcode() { return barcode; }
        public String getPortName() { return portName; }
        public LocalDateTime getScanTime() { return scanTime; }
    }
    
    // 内部类：烧录结果数据
    public static class BurnResultData {
        private String barcode;
        private boolean success;
        private String message;
        
        public BurnResultData(String barcode, boolean success, String message) {
            this.barcode = barcode;
            this.success = success;
            this.message = message;
        }
        
        public String getBarcode() { return barcode; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}