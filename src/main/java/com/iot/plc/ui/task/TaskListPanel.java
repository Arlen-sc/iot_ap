package com.iot.plc.ui.task;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.TaskInfo;
import com.iot.plc.ui.base.BasePanel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.geometry.Orientation;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 任务列表管理面板
 * 用于管理系统定时任务
 */
public class TaskListPanel extends BasePanel {
    private TableView<TaskInfoTableModel> taskTable;
    private ObservableList<TaskInfoTableModel> taskData;
    private VBox detailPanel;
    private Button refreshButton;
    private Button addButton;
    private Button startAllButton;
    private Button stopAllButton;

    public TaskListPanel() {
        initComponents();
        loadData();
    }

    @Override
    protected void initComponents() {
        this.setPadding(new Insets(15));

        // 创建标题和操作按钮
        HBox headerBox = new HBox(20);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("任务列表管理");
        titleLabel.setFont(Font.font(18));
        
        refreshButton = new Button("刷新");
        refreshButton.setPrefWidth(80);
        refreshButton.setOnAction(e -> loadData());
        
        addButton = new Button("新增任务");
        addButton.setPrefWidth(100);
        addButton.setOnAction(e -> showAddTaskDialog());
        
        startAllButton = new Button("开启所有任务");
        startAllButton.setPrefWidth(120);
        startAllButton.setOnAction(e -> startAllTasks());
        
        stopAllButton = new Button("关闭所有任务");
        stopAllButton.setPrefWidth(120);
        stopAllButton.setOnAction(e -> stopAllTasks());
        
        headerBox.getChildren().addAll(titleLabel, refreshButton, addButton, startAllButton, stopAllButton);

        // 创建分割面板
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.VERTICAL);
        splitPane.setDividerPosition(0, 0.6);

        // 创建任务表格
        taskTable = new TableView<>();
        taskData = FXCollections.observableArrayList();
        taskTable.setItems(taskData);
        taskTable.setRowFactory(tv -> new TableRow<TaskInfoTableModel>() {
            @Override
            protected void updateItem(TaskInfoTableModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.getStatus() == 1) {
                    setStyle("-fx-background-color: #eeffee; -fx-text-fill: #006600;");
                } else {
                    setStyle("-fx-background-color: #f5f5f5; -fx-text-fill: #666666;");
                }
            }
        });

        // 设置表格列
        TableColumn<TaskInfoTableModel, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(60);

        TableColumn<TaskInfoTableModel, String> deviceColumn = new TableColumn<>("设备号");
        deviceColumn.setCellValueFactory(new PropertyValueFactory<>("deviceId"));
        deviceColumn.setPrefWidth(120);

        TableColumn<TaskInfoTableModel, String> taskNameColumn = new TableColumn<>("任务名称");
        taskNameColumn.setCellValueFactory(new PropertyValueFactory<>("taskName"));
        taskNameColumn.setPrefWidth(150);

        TableColumn<TaskInfoTableModel, String> cronColumn = new TableColumn<>("Cron表达式");
        cronColumn.setCellValueFactory(new PropertyValueFactory<>("cronExpression"));
        cronColumn.setPrefWidth(200);

        TableColumn<TaskInfoTableModel, String> startTimeColumn = new TableColumn<>("开始时间");
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        startTimeColumn.setPrefWidth(180);

        TableColumn<TaskInfoTableModel, Integer> statusColumn = new TableColumn<>("状态");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(80);
        statusColumn.setCellFactory(column -> {
            return new TableCell<TaskInfoTableModel, Integer>() {
                @Override
                protected void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("");
                    } else {
                        setText(item == 1 ? "运行中" : "已停止");
                    }
                }
            };
        });

        // 操作列
        TableColumn<TaskInfoTableModel, Void> actionColumn = new TableColumn<>("操作");
        actionColumn.setPrefWidth(150);
        actionColumn.setCellFactory(param -> new ActionButtonTableCell());

        taskTable.getColumns().addAll(
                idColumn, deviceColumn, taskNameColumn, cronColumn,
                startTimeColumn, statusColumn, actionColumn
        );
        taskTable.setFixedCellSize(40);

        // 监听表格选择事件
        taskTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showTaskDetails(newSelection.getId());
            }
        });

        // 创建详情面板
        detailPanel = new VBox(10);
        detailPanel.setPadding(new Insets(15));
        detailPanel.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 4px;");
        Label detailPlaceholder = new Label("请选择一个任务查看详情");
        detailPlaceholder.setFont(Font.font(14));
        detailPlaceholder.setAlignment(Pos.CENTER);
        detailPlaceholder.setPrefHeight(150);
        detailPanel.getChildren().add(detailPlaceholder);

        // 添加到分割面板
        splitPane.getItems().addAll(taskTable, detailPanel);

        // 添加所有组件到主面板
        this.getChildren().addAll(headerBox, splitPane);

        // 设置布局权重
        VBox.setVgrow(splitPane, Priority.ALWAYS);
    }

    @Override
    protected void loadData() {
        Platform.runLater(() -> {
            taskData.clear();
            try {
                List<TaskInfo> taskList = DatabaseManager.getAllTaskInfo();
                for (TaskInfo task : taskList) {
                    taskData.add(new TaskInfoTableModel(task));
                }
                
                // 自动选择第一个任务
                if (!taskData.isEmpty() && taskTable.getSelectionModel().getSelectedItem() == null) {
                    taskTable.getSelectionModel().selectFirst();
                }
            } catch (SQLException e) {
                showErrorDialog("加载任务失败", e.getMessage());
            }
        });
    }

    @Override
    public void refresh() {
        loadData();
    }

    /**
     * 显示任务详情
     */
    private void showTaskDetails(int taskId) {
        Platform.runLater(() -> {
            try {
                TaskInfo taskInfo = DatabaseManager.getTaskInfoById(taskId);
                if (taskInfo == null) {
                    detailPanel.getChildren().clear();
                    detailPanel.getChildren().add(new Label("任务不存在或已被删除"));
                    return;
                }

                // 清空详情面板
                detailPanel.getChildren().clear();

                // 创建详情标题
                Label detailTitle = new Label("任务详情");
                detailTitle.setFont(Font.font(16));
                detailTitle.setPadding(new Insets(0, 0, 10, 0));
                detailPanel.getChildren().add(detailTitle);

                // 创建详情表格
                GridPane detailGrid = new GridPane();
                detailGrid.setHgap(15);
                detailGrid.setVgap(12);
                detailGrid.setPadding(new Insets(10));

                detailGrid.add(new Label("任务ID: "), 0, 0);
                detailGrid.add(new Label(String.valueOf(taskInfo.getId())), 1, 0);

                detailGrid.add(new Label("设备号: "), 0, 1);
                detailGrid.add(new Label(taskInfo.getDeviceId()), 1, 1);

                detailGrid.add(new Label("任务名称: "), 0, 2);
                detailGrid.add(new Label(taskInfo.getTaskName()), 1, 2);

                detailGrid.add(new Label("Cron表达式: "), 0, 3);
                detailGrid.add(new Label(taskInfo.getCronExpression()), 1, 3);

                detailGrid.add(new Label("开始时间: "), 0, 4);
                detailGrid.add(new Label(taskInfo.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))), 1, 4);

                detailGrid.add(new Label("上次执行时间: "), 0, 5);
                detailGrid.add(new Label(taskInfo.getLastExecuteTime() != null ? 
                        taskInfo.getLastExecuteTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "未执行"), 1, 5);

                detailGrid.add(new Label("状态: "), 0, 6);
                detailGrid.add(new Label(taskInfo.getStatus() == 1 ? "运行中" : "已停止"), 1, 6);

                detailGrid.add(new Label("备注: "), 0, 7);
                detailGrid.add(new Label(taskInfo.getRemark() != null ? taskInfo.getRemark() : "无"), 1, 7);

                // 添加到详情面板
                detailPanel.getChildren().add(detailGrid);

            } catch (SQLException e) {
                detailPanel.getChildren().clear();
                detailPanel.getChildren().add(new Label("加载任务详情失败: " + e.getMessage()));
            }
        });
    }

    /**
     * 显示新增任务对话框
     */
    private void showAddTaskDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("新增任务");
        dialog.setHeaderText("创建新的定时任务");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField deviceIdField = new TextField();
        deviceIdField.setPromptText("请输入设备号");
        TextField taskNameField = new TextField();
        taskNameField.setPromptText("请输入任务名称");
        TextField cronField = new TextField();
        cronField.setPromptText("请输入Cron表达式");
        TextField remarkField = new TextField();
        remarkField.setPromptText("请输入备注（可选）");

        grid.add(new Label("设备号: *"), 0, 0);
        grid.add(deviceIdField, 1, 0);
        grid.add(new Label("任务名称: *"), 0, 1);
        grid.add(taskNameField, 1, 1);
        grid.add(new Label("Cron表达式: *"), 0, 2);
        grid.add(cronField, 1, 2);
        grid.add(new Label("备注: "), 0, 3);
        grid.add(remarkField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // 验证输入
                if (deviceIdField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "设备号不能为空");
                    return null;
                }
                if (taskNameField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "任务名称不能为空");
                    return null;
                }
                if (cronField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "Cron表达式不能为空");
                    return null;
                }

                try {
                    // 创建任务
                    TaskInfo taskInfo = new TaskInfo();
                    taskInfo.setDeviceId(deviceIdField.getText().trim());
                    taskInfo.setTaskName(taskNameField.getText().trim());
                    taskInfo.setCronExpression(cronField.getText().trim());
                    taskInfo.setStartTime(LocalDateTime.now());
                    taskInfo.setStatus(0); // 默认停止状态
                    taskInfo.setRemark(remarkField.getText().trim());

                    DatabaseManager.addTaskInfo(taskInfo);
                    loadData();
                    showInfoDialog("成功", "任务创建成功");
                } catch (SQLException e) {
                    showErrorDialog("错误", "创建任务失败: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    /**
     * 开始所有任务
     */
    private void startAllTasks() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认开启");
        alert.setHeaderText("开启所有任务");
        alert.setContentText("确定要开启所有任务吗？");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    DatabaseManager.updateAllTasksStatus(1);
                    loadData();
                    showInfoDialog("成功", "所有任务已开启");
                } catch (SQLException e) {
                    showErrorDialog("错误", "开启任务失败: " + e.getMessage());
                }
            }
        });
    }

    /**
     * 停止所有任务
     */
    private void stopAllTasks() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认停止");
        alert.setHeaderText("停止所有任务");
        alert.setContentText("确定要停止所有任务吗？");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    DatabaseManager.updateAllTasksStatus(0);
                    loadData();
                    showInfoDialog("成功", "所有任务已停止");
                } catch (SQLException e) {
                    showErrorDialog("错误", "停止任务失败: " + e.getMessage());
                }
            }
        });
    }

    // 操作按钮单元格
    private class ActionButtonTableCell extends TableCell<TaskInfoTableModel, Void> {
        private final Button startButton = new Button("开启");
        private final Button stopButton = new Button("停止");
        private final Button editButton = new Button("编辑");
        private final Button deleteButton = new Button("删除");

        ActionButtonTableCell() {
            HBox buttons = new HBox(5);
            buttons.getChildren().addAll(startButton, stopButton, editButton, deleteButton);
            buttons.setAlignment(Pos.CENTER);

            startButton.setOnAction(e -> {
                TaskInfoTableModel model = getTableView().getItems().get(getIndex());
                try {
                    DatabaseManager.updateTaskInfoStatus(model.getId(), 1);
                    loadData();
                    showInfoDialog("成功", "任务已开启");
                } catch (SQLException ex) {
                    showErrorDialog("错误", "开启任务失败: " + ex.getMessage());
                }
            });

            stopButton.setOnAction(e -> {
                TaskInfoTableModel model = getTableView().getItems().get(getIndex());
                try {
                    DatabaseManager.updateTaskInfoStatus(model.getId(), 0);
                    loadData();
                    showInfoDialog("成功", "任务已停止");
                } catch (SQLException ex) {
                    showErrorDialog("错误", "停止任务失败: " + ex.getMessage());
                }
            });

            editButton.setOnAction(e -> {
                TaskInfoTableModel model = getTableView().getItems().get(getIndex());
                showEditTaskDialog(model.getId());
            });

            deleteButton.setOnAction(e -> {
                TaskInfoTableModel model = getTableView().getItems().get(getIndex());
                deleteTask(model.getId());
            });

            setGraphic(buttons);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : getGraphic());
        }
    }

    /**
     * 显示编辑任务对话框
     */
    private void showEditTaskDialog(int taskId) {
        try {
            TaskInfo taskInfo = DatabaseManager.getTaskInfoById(taskId);
            if (taskInfo == null) {
                showErrorDialog("错误", "任务不存在或已被删除");
                return;
            }

            Dialog<Void> dialog = new Dialog<>();
            dialog.setTitle("编辑任务");
            dialog.setHeaderText("修改任务信息");

            ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            GridPane grid = new GridPane();
            grid.setHgap(15);
            grid.setVgap(15);
            grid.setPadding(new Insets(20, 150, 10, 10));

            TextField deviceIdField = new TextField(taskInfo.getDeviceId());
            TextField taskNameField = new TextField(taskInfo.getTaskName());
            TextField cronField = new TextField(taskInfo.getCronExpression());
            TextField remarkField = new TextField(taskInfo.getRemark() != null ? taskInfo.getRemark() : "");

            grid.add(new Label("设备号: *"), 0, 0);
            grid.add(deviceIdField, 1, 0);
            grid.add(new Label("任务名称: *"), 0, 1);
            grid.add(taskNameField, 1, 1);
            grid.add(new Label("Cron表达式: *"), 0, 2);
            grid.add(cronField, 1, 2);
            grid.add(new Label("备注: "), 0, 3);
            grid.add(remarkField, 1, 3);

            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    // 验证输入
                    if (deviceIdField.getText().trim().isEmpty()) {
                        showErrorDialog("错误", "设备号不能为空");
                        return null;
                    }
                    if (taskNameField.getText().trim().isEmpty()) {
                        showErrorDialog("错误", "任务名称不能为空");
                        return null;
                    }
                    if (cronField.getText().trim().isEmpty()) {
                        showErrorDialog("错误", "Cron表达式不能为空");
                        return null;
                    }

                    try {
                        taskInfo.setDeviceId(deviceIdField.getText().trim());
                        taskInfo.setTaskName(taskNameField.getText().trim());
                        taskInfo.setCronExpression(cronField.getText().trim());
                        taskInfo.setRemark(remarkField.getText().trim());

                        DatabaseManager.updateTaskInfo(taskInfo);
                        loadData();
                        showInfoDialog("成功", "任务更新成功");
                    } catch (SQLException e) {
                        showErrorDialog("错误", "更新任务失败: " + e.getMessage());
                    }
                }
                return null;
            });

            dialog.showAndWait();
        } catch (SQLException e) {
            showErrorDialog("错误", "获取任务失败: " + e.getMessage());
        }
    }

    /**
     * 删除任务
     */
    private void deleteTask(int taskId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("删除任务");
        alert.setContentText("确定要删除该任务吗？此操作不可撤销。");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    DatabaseManager.deleteTaskInfo(taskId);
                    loadData();
                    showInfoDialog("成功", "任务删除成功");
                } catch (SQLException e) {
                    showErrorDialog("错误", "删除任务失败: " + e.getMessage());
                }
            }
        });
    }

    // 任务表格模型
    public static class TaskInfoTableModel {
        private final int id;
        private final String deviceId;
        private final String taskName;
        private final String cronExpression;
        private final String startTime;
        private final int status;

        public TaskInfoTableModel(TaskInfo taskInfo) {
            this.id = taskInfo.getId();
            this.deviceId = taskInfo.getDeviceId();
            this.taskName = taskInfo.getTaskName();
            this.cronExpression = taskInfo.getCronExpression();
            this.startTime = taskInfo.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.status = taskInfo.getStatus();
        }

        public int getId() {
            return id;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getTaskName() {
            return taskName;
        }

        public String getCronExpression() {
            return cronExpression;
        }

        public String getStartTime() {
            return startTime;
        }

        public int getStatus() {
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