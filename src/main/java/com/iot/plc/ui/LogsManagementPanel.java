package com.iot.plc.ui;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.logger.Logger;
import com.iot.plc.model.BarcodeData;
import com.iot.plc.model.DeviceResult;
import com.iot.plc.model.ProgramResult;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.util.Callback;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class LogsManagementPanel extends VBox {
    private TableView<LogEntry> logTable;
    private ObservableList<LogEntry> logData;
    private ComboBox<String> logTypeComboBox;
    private Button refreshButton;
    private Button clearAllButton;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public LogsManagementPanel() {
        initComponents();
        loadLogs();
    }

    private void initComponents() {
        // 设置布局
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // 创建标题
        Label titleLabel = new Label("日志管理");
        titleLabel.setFont(Font.font(18));
        titleLabel.setAlignment(Pos.CENTER_LEFT);

        // 创建控制面板
        HBox controlPanel = new HBox(10);
        controlPanel.setAlignment(Pos.CENTER_LEFT);
        controlPanel.setPadding(new Insets(5));

        // 日志类型选择
        logTypeComboBox = new ComboBox<>();
        logTypeComboBox.getItems().addAll("全部日志", "PLC数据", "条码数据", "验证结果", "烧录结果");
        logTypeComboBox.setValue("全部日志");
        logTypeComboBox.setOnAction(e -> loadLogs());

        // 刷新按钮
        refreshButton = new Button("刷新");
        refreshButton.setOnAction(e -> loadLogs());

        // 清空日志按钮
        clearAllButton = new Button("清空所有日志");
        clearAllButton.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white;");
        clearAllButton.setOnAction(e -> clearAllLogs());

        // 添加组件到控制面板
        controlPanel.getChildren().addAll(
                new Label("日志类型: "), logTypeComboBox,
                refreshButton,
                clearAllButton
        );

        // 创建日志表格
        logTable = new TableView<>();
        logData = FXCollections.observableArrayList();
        logTable.setItems(logData);
        logTable.setRowFactory(tv -> new TableRow<LogEntry>() {
            @Override
            protected void updateItem(LogEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (logTable.getSelectionModel().isSelected(getIndex())) {
                    setStyle("-fx-background-color: #cceeff;");
                }
            }
        });

        // 设置表格列
        TableColumn<LogEntry, String> typeColumn = new TableColumn<>("类型");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.setPrefWidth(100);

        TableColumn<LogEntry, String> timeColumn = new TableColumn<>("时间");
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timeColumn.setPrefWidth(150);

        TableColumn<LogEntry, String> dataColumn = new TableColumn<>("数据内容");
        dataColumn.setCellValueFactory(new PropertyValueFactory<>("data"));
        dataColumn.setPrefWidth(600);

        TableColumn<LogEntry, String> statusColumn = new TableColumn<>("状态");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(80);

        // 添加列到表格
        logTable.getColumns().addAll(typeColumn, timeColumn, dataColumn, statusColumn);

        // 设置表格自适应高度
        VBox.setVgrow(logTable, Priority.ALWAYS);

        // 添加所有组件到主面板
        this.getChildren().addAll(titleLabel, controlPanel, logTable);
    }

    private void loadLogs() {
        Platform.runLater(() -> {
            logData.clear();
            try {
                String selectedType = logTypeComboBox.getValue();
                List<LogEntry> logs = new ArrayList<>();

                // 根据选择的日志类型加载不同的日志数据
                if ("全部日志".equals(selectedType) || "条码数据".equals(selectedType)) {
                    logs.addAll(loadBarcodeLogs());
                }
                
                if ("全部日志".equals(selectedType) || "烧录结果".equals(selectedType)) {
                    logs.addAll(loadProgramResultLogs());
                }

                // 按时间倒序排序
                logs.sort((log1, log2) -> log2.getTimestamp().compareTo(log1.getTimestamp()));
                
                logData.addAll(logs);

            } catch (SQLException e) {
                Logger.getInstance().error("加载日志失败: " + e.getMessage());
                showErrorDialog("加载失败", "加载日志数据时出错: " + e.getMessage());
            }
        });
    }

    private List<LogEntry> loadBarcodeLogs() throws SQLException {
        List<LogEntry> logs = new ArrayList<>();
        List<BarcodeData> barcodeDataList = DatabaseManager.getAllBarcodes();
        
        for (BarcodeData data : barcodeDataList) {
            LogEntry log = new LogEntry();
            log.setType("条码数据");
            // 使用数据库中的扫描时间
            log.setTimestamp(data.getScanTime().format(formatter));
            log.setData("设备ID: " + data.getDeviceId() + ", 条码: " + data.getBarcode() + ", 端口: " + data.getPortName());
            log.setStatus("成功");
            logs.add(log);
        }
        
        return logs;
    }

    private List<LogEntry> loadProgramResultLogs() throws SQLException {
        List<LogEntry> logs = new ArrayList<>();
        // 从数据库获取所有批次的烧录结果
        List<ProgramResult> allProgramResults = DatabaseManager.getAllProgramResults();
        
        for (ProgramResult result : allProgramResults) {
            // 对每个批次的每个设备结果创建日志条目
            for (DeviceResult deviceResult : result.getResults()) {
                LogEntry log = new LogEntry();
                log.setType("烧录结果");
                log.setTimestamp(result.getTimestamp());
                log.setData("批次: " + result.getBatchId() + ", 设备ID: " + deviceResult.getDeviceId() + ", 条码: " + deviceResult.getBarcode());
                log.setStatus(deviceResult.isSuccess() ? "成功" : "失败");
                logs.add(log);
            }
        }
        
        return logs;
    }

    private void clearAllLogs() {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("确认清空");
        confirmationAlert.setHeaderText(null);
        confirmationAlert.setContentText("确定要清空所有日志吗？此操作不可恢复！");

        confirmationAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // 清空所有日志表
                    clearBarcodeLogs();
                    clearProgramResultLogs();
                    // 可以继续添加其他日志表的清空操作
                    
                    loadLogs(); // 重新加载日志，现在应该是空的
                    showInfoDialog("操作成功", "所有日志已成功清空");
                } catch (SQLException e) {
                    Logger.getInstance().error("清空日志失败: " + e.getMessage());
                    showErrorDialog("清空失败", "清空日志时出错: " + e.getMessage());
                }
            }
        });
    }

    private void clearBarcodeLogs() throws SQLException {
        String sql = "DELETE FROM barcode_data";
        try (java.sql.Connection conn = DatabaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        }
    }

    private void clearProgramResultLogs() throws SQLException {
        String sql = "DELETE FROM program_result";
        try (java.sql.Connection conn = DatabaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.executeUpdate();
        }
    }

    private void showErrorDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showInfoDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // 日志条目类
    public static class LogEntry {
        private String type;
        private String timestamp;
        private String data;
        private String status;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}