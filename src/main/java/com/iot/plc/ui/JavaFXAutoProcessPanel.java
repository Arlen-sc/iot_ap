package com.iot.plc.ui;

import com.iot.plc.logger.LoggerFactory;
import com.iot.plc.model.BarcodeData;
import com.iot.plc.service.AutoProcessService;
import com.iot.plc.service.SerialPortService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

/**
 * 自动处理流程界面
 * 负责显示流程状态、条码数据和操作按钮
 */
public class JavaFXAutoProcessPanel extends BorderPane {
    private static final Logger logger = LoggerFactory.getLogger(JavaFXAutoProcessPanel.class.getName());
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // 服务实例
    private final AutoProcessService autoProcessService = AutoProcessService.getInstance();
    private final SerialPortService serialPortService = SerialPortService.getInstance();
    
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
    private final String deviceId = "PLC_DEVICE_001";
    
    // UI组件
    private TableView<BarcodeData> barcodeTable;
    private TextArea logArea;
    private Button startProcessButton;
    private Button resetProcessButton;
    private Button clearBarcodesButton;
    
    public JavaFXAutoProcessPanel() {
        initUI();
        startStatusUpdateThread();
    }
    
    private void initUI() {
        // 顶部状态栏
        VBox statusBox = createStatusBox();
        setTop(statusBox);
        
        // 中部条码表格
        barcodeTable = createBarcodeTable();
        ScrollPane tableScrollPane = new ScrollPane(barcodeTable);
        tableScrollPane.setFitToWidth(true);
        
        // 底部日志和操作按钮
        VBox bottomBox = new VBox(10);
        
        // 操作按钮
        HBox buttonBox = new HBox(10);
        startProcessButton = new Button("启动流程");
        resetProcessButton = new Button("重置流程");
        clearBarcodesButton = new Button("清空条码");
        
        startProcessButton.setOnAction(e -> startProcess());
        resetProcessButton.setOnAction(e -> resetProcess());
        clearBarcodesButton.setOnAction(e -> clearBarcodes());
        
        buttonBox.getChildren().addAll(startProcessButton, resetProcessButton, clearBarcodesButton);
        
        // 日志区域
        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);
        ScrollPane logScrollPane = new ScrollPane(logArea);
        
        bottomBox.getChildren().addAll(buttonBox, logScrollPane);
        
        setCenter(tableScrollPane);
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
        barcodeCountBox.getChildren().addAll(
                new Label("预期条码数量: "),
                createStatusLabel(expectedBarcodeCount),
                new Label("实际条码数量: "),
                createStatusLabel(actualBarcodeCount)
        );
        
        statusBox.getChildren().addAll(mainStatusBox, barcodeCountBox);
        return statusBox;
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
    
    private void startStatusUpdateThread() {
        // 启动状态更新线程
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    updateStatus();
                    updateBarcodeData();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }
    
    private void updateBarcodeData() {
        Platform.runLater(() -> {
            List<BarcodeData> barcodes = serialPortService.getDeviceBarcodes(deviceId);
            int count = barcodes.size();
            actualBarcodeCount.set(String.valueOf(count));
            
            // 更新表格数据
            barcodeDataList.clear();
            barcodeDataList.addAll(barcodes);
        });
    }
    
    private void updateStatus() {
        Platform.runLater(() -> {
            // 从服务获取最新状态
            currentStatus.set(autoProcessService.getCurrentStatus());
            serialPortStatus.set(autoProcessService.getSerialPortStatus());
            plcStatus.set(autoProcessService.getPlcStatus());
            upperComputerStatus.set(autoProcessService.getUpperComputerStatus());
            emsStatus.set(autoProcessService.getEmsStatus());
            expectedBarcodeCount.set(String.valueOf(autoProcessService.getExpectedBarcodeCount()));
        });
    }
    
    private void startProcess() {
        // 调用服务的启动流程方法
        autoProcessService.startProcess();
        
        // 记录操作日志
        log("用户点击启动流程按钮");
    }
    
    private void resetProcess() {
        // 调用服务的重置流程方法
        autoProcessService.resetProcess();
        
        // 记录操作日志
        log("用户点击重置流程按钮");
    }
    
    private void clearBarcodes() {
        // 调用服务的清空条码方法
        autoProcessService.clearBarcodes();
        
        // 记录操作日志
        log("用户点击清空条码按钮");
    }
    
    private void log(String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = "[" + timestamp + "] " + message;
        
        Platform.runLater(() -> {
            logArea.appendText(logMessage + "\n");
            // 自动滚动到底部
            logArea.setScrollTop(Double.MAX_VALUE);
        });
        
        logger.info(message);
    }
}