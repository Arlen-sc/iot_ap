package com.iot.plc.ui.logs;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.LogItem;
import com.iot.plc.ui.base.JavaFXBasePanel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 日志管理面板
 * 用于管理和查看系统各类日志
 */
public class LogsManagementPanel extends JavaFXBasePanel {
    // 日志类型枚举
    public enum LogType {
        ALL("全部"),
        PLC_DATA("PLC数据"),
        BARCODE_DATA("条码数据"),
        VERIFICATION_RESULT("验证结果"),
        BURN_RESULT("烧录结果");

        private final String description;

        LogType(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private TableView<LogTableModel> logTable;
    private ObservableList<LogTableModel> logData;
    private ComboBox<LogType> logTypeComboBox;
    private TextField daysTextField;

    public LogsManagementPanel() {
        initComponents();
        loadData();
    }

    @Override
    protected void initComponents() {
        this.setPadding(new Insets(15));
        this.setSpacing(15);

        // 创建标题
        Label titleLabel = new Label("日志管理");
        titleLabel.setFont(Font.font(18));
        titleLabel.setAlignment(Pos.CENTER_LEFT);

        // 创建筛选面板
        HBox filterPanel = new HBox(15);
        filterPanel.setPadding(new Insets(10));
        filterPanel.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 4px; -fx-padding: 15px;");

        Label logTypeLabel = new Label("日志类型：");
        logTypeComboBox = new ComboBox<>();
        logTypeComboBox.getItems().addAll(LogType.values());
        logTypeComboBox.setValue(LogType.ALL);
        logTypeComboBox.setPrefWidth(120);

        Button refreshButton = new Button("刷新");
        refreshButton.setPrefWidth(80);
        refreshButton.setOnAction(e -> loadData());

        Button clearButton = new Button("清空当前日志");
        clearButton.setPrefWidth(120);
        clearButton.setOnAction(e -> clearCurrentLogs());

        filterPanel.getChildren().addAll(logTypeLabel, logTypeComboBox, refreshButton, clearButton);

        // 创建日志清理设置面板
        HBox cleanupPanel = new HBox(15);
        cleanupPanel.setPadding(new Insets(10));
        cleanupPanel.setStyle("-fx-background-color: #f9f9f9; -fx-border-radius: 4px; -fx-padding: 15px;");

        Label daysLabel = new Label("日志保存天数：");
        daysTextField = new TextField("7");
        daysTextField.setPrefWidth(80);
        daysTextField.setPromptText("7");

        Button cleanupButton = new Button("清理过期日志");
        cleanupButton.setPrefWidth(120);
        cleanupButton.setOnAction(e -> cleanupLogs());

        cleanupPanel.getChildren().addAll(daysLabel, daysTextField, cleanupButton);

        // 创建日志表格
        logTable = new TableView<>();
        logData = FXCollections.observableArrayList();
        logTable.setItems(logData);
        logTable.setRowFactory(tv -> new TableRow<LogTableModel>() {
            @Override
            protected void updateItem(LogTableModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.getLogType().equals("失败")) {
                    setStyle("-fx-background-color: #ffeeee;");
                } else if (item.getLogType().equals("成功")) {
                    setStyle("-fx-background-color: #eeffee;");
                }
            }
        });

        // 设置表格列
        TableColumn<LogTableModel, String> typeColumn = new TableColumn<>("类型");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("logType"));
        typeColumn.setPrefWidth(100);

        TableColumn<LogTableModel, String> timeColumn = new TableColumn<>("时间");
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timeColumn.setPrefWidth(200);

        TableColumn<LogTableModel, String> dataColumn = new TableColumn<>("数据内容");
        dataColumn.setCellValueFactory(new PropertyValueFactory<>("dataContent"));
        dataColumn.setPrefWidth(400);

        TableColumn<LogTableModel, String> statusColumn = new TableColumn<>("状态");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(100);

        logTable.getColumns().addAll(typeColumn, timeColumn, dataColumn, statusColumn);
        logTable.setFixedCellSize(35);

        // 添加所有组件到主面板
        this.getChildren().addAll(titleLabel, filterPanel, cleanupPanel, logTable);

        // 设置布局权重
        VBox.setVgrow(logTable, Priority.ALWAYS);
    }

    @Override
    protected void loadData() {
        Platform.runLater(() -> {
            logData.clear();
            try {
                LogType selectedType = logTypeComboBox.getValue();
                List<LogItem> logs;

                // 根据选择的日志类型加载不同的数据
                if (selectedType == LogType.ALL) {
                    logs = DatabaseManager.getAllLogs();
                } else if (selectedType == LogType.PLC_DATA) {
                    logs = DatabaseManager.getLogsByType("PLC数据");
                } else if (selectedType == LogType.BARCODE_DATA) {
                    logs = DatabaseManager.getLogsByType("条码数据");
                } else if (selectedType == LogType.VERIFICATION_RESULT) {
                    logs = DatabaseManager.getLogsByType("验证结果");
                } else if (selectedType == LogType.BURN_RESULT) {
                    logs = DatabaseManager.getBurnResultLogs();
                } else {
                    logs = DatabaseManager.getAllLogs();
                }

                // 添加加载条码数据
                if (selectedType == LogType.BARCODE_DATA) {
                    loadBarcodeDataLogs();
                }

                // 转换为表格模型
                for (LogItem log : logs) {
                    logData.add(new LogTableModel(log));
                }

                // 自动调整列宽
                logTable.getColumns().forEach(column -> {
                    column.setResizable(true);
                });

            } catch (SQLException e) {
                showErrorDialog("加载日志失败", e.getMessage());
            }
        });
    }

    @Override
    public void refresh() {
        loadData();
    }

    /**
     * 加载条码数据日志
     */
    private void loadBarcodeDataLogs() {
        try {
            // 从条码数据表中加载条码数据作为日志
            List<String> barcodeData = DatabaseManager.getRecentBarcodeData();
            for (String barcode : barcodeData) {
                LogItem logItem = new LogItem();
                logItem.setLogType("条码数据");
                logItem.setTimestamp(LocalDateTime.now());
                logItem.setDataContent("扫描到条码：" + barcode);
                logItem.setStatus("成功");
                logData.add(new LogTableModel(logItem));
            }
        } catch (SQLException e) {
            showErrorDialog("加载条码数据失败", e.getMessage());
        }
    }

    /**
     * 清空当前类型的日志
     */
    private void clearCurrentLogs() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认清空");
        alert.setHeaderText("清空日志");
        alert.setContentText("确定要清空当前筛选的所有日志吗？");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    LogType selectedType = logTypeComboBox.getValue();
                    if (selectedType == LogType.ALL) {
                        DatabaseManager.clearAllLogs();
                    } else {
                        DatabaseManager.clearLogsByType(selectedType.toString());
                    }
                    logData.clear();
                    showInfoDialog("成功", "日志已清空");
                } catch (SQLException e) {
                    showErrorDialog("清空日志失败", e.getMessage());
                }
            }
        });
    }

    /**
     * 清理过期日志
     */
    private void cleanupLogs() {
        String daysText = daysTextField.getText().trim();
        int days;

        try {
            days = Integer.parseInt(daysText);
            if (days <= 0) {
                showErrorDialog("错误", "请输入大于0的天数");
                return;
            }
        } catch (NumberFormatException e) {
            showErrorDialog("错误", "请输入有效的数字");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认清理");
        alert.setHeaderText("清理过期日志");
        alert.setContentText("确定要清理" + days + "天前的所有日志吗？");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    int deletedCount = DatabaseManager.cleanupLogsByDays(days);
                    loadData(); // 重新加载日志
                    showInfoDialog("成功", "已清理" + deletedCount + "条过期日志");
                } catch (SQLException e) {
                    showErrorDialog("清理日志失败", e.getMessage());
                }
            }
        });
    }

    // 日志表格模型
    public static class LogTableModel {
        private final String logType;
        private final String timestamp;
        private final String dataContent;
        private final String status;

        public LogTableModel(LogItem logItem) {
            this.logType = logItem.getLogType();
            this.timestamp = logItem.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
            this.dataContent = logItem.getDataContent();
            this.status = logItem.getStatus();
        }

        public String getLogType() {
            return logType;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getDataContent() {
            return dataContent;
        }

        public String getStatus() {
            return status;
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