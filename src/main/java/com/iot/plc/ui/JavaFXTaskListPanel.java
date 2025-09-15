package com.iot.plc.ui;

import com.iot.plc.MainApp;
import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.Task;
import com.iot.plc.model.TaskDetail;
import com.iot.plc.scheduler.TaskScheduler;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import java.sql.SQLException;
import java.util.List;

public class JavaFXTaskListPanel extends VBox {
    private TableView<TaskTableModel> taskTable;
    private ObservableList<TaskTableModel> taskData;
    private TableView<TaskDetailTableModel> detailTable;
    private ObservableList<TaskDetailTableModel> detailData;
    private int currentTaskId = -1;
    private BorderPane detailPanelContainer;

    public JavaFXTaskListPanel() {
        initComponents();
        loadTasks();
    }

    private void initComponents() {
        // 设置布局
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // 创建表头控制按钮面板
        HBox headerPanel = new HBox(10);
        headerPanel.setAlignment(Pos.CENTER_LEFT);
        Button startAllButton = new Button("开启所有定时任务");
        Button stopAllButton = new Button("关闭所有定时任务");
        startAllButton.setPrefSize(150, 30);
        stopAllButton.setPrefSize(150, 30);

        startAllButton.setOnAction(e -> startAllTasks());
        stopAllButton.setOnAction(e -> stopAllTasks());

        headerPanel.getChildren().addAll(startAllButton, stopAllButton);

        // 创建任务表格
        taskTable = new TableView<>();
        taskData = FXCollections.observableArrayList();
        taskTable.setItems(taskData);
        taskTable.setRowFactory(tv -> new TableRow<TaskTableModel>() {
            @Override
            protected void updateItem(TaskTableModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (taskTable.getSelectionModel().isSelected(getIndex())) {
                    setStyle("-fx-background-color: #cceeff;");
                }
            }
        });

        // 设置表格列
        TableColumn<TaskTableModel, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);

        TableColumn<TaskTableModel, String> deviceIdColumn = new TableColumn<>("设备号");
        deviceIdColumn.setCellValueFactory(new PropertyValueFactory<>("deviceId"));
        deviceIdColumn.setPrefWidth(100);

        TableColumn<TaskTableModel, String> cronColumn = new TableColumn<>("定时任务cron");
        cronColumn.setCellValueFactory(new PropertyValueFactory<>("cronExpression"));
        cronColumn.setPrefWidth(150);

        TableColumn<TaskTableModel, String> descColumn = new TableColumn<>("说明");
        descColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descColumn.setPrefWidth(200);

        TableColumn<TaskTableModel, String> typeColumn = new TableColumn<>("任务类型");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("taskType"));
        typeColumn.setPrefWidth(100);

        TableColumn<TaskTableModel, String> nameColumn = new TableColumn<>("任务名称");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("taskName"));
        nameColumn.setPrefWidth(150);

        TableColumn<TaskTableModel, String> statusColumn = new TableColumn<>("定时任务状态");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(100);

        // 操作列
        TableColumn<TaskTableModel, Void> actionColumn = new TableColumn<>("操作");
        actionColumn.setPrefWidth(150);
        actionColumn.setCellFactory(param -> new ActionButtonTableCell());

        taskTable.getColumns().addAll(
                idColumn, deviceIdColumn, cronColumn, descColumn, 
                typeColumn, nameColumn, statusColumn, actionColumn
        );

        // 设置表格行高
        taskTable.setFixedCellSize(40);

        // 监听表格选择事件
        taskTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showTaskDetails(newSelection.getId());
            }
        });

        // 创建任务详情面板
        detailPanelContainer = new BorderPane();
        detailPanelContainer.setPrefHeight(300);
        detailPanelContainer.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 4px;");
        Label detailPlaceholder = new Label("请选择一个任务查看详情");
        detailPlaceholder.setFont(Font.font(14));
        detailPlaceholder.setAlignment(Pos.CENTER);
        detailPanelContainer.setCenter(detailPlaceholder);

        // 创建按钮面板
        HBox buttonPanel = new HBox(10);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);
        Button addButton = new Button("新增");
        Button refreshButton = new Button("刷新");

        addButton.setOnAction(e -> showAddTaskDialog());
        refreshButton.setOnAction(e -> loadTasks());

        buttonPanel.getChildren().addAll(addButton, refreshButton);

        // 添加所有组件到主面板
        this.getChildren().addAll(headerPanel, taskTable, detailPanelContainer, buttonPanel);

        // 设置任务表格自适应高度
        VBox.setVgrow(taskTable, Priority.ALWAYS);
    }

    // 加载任务列表
    private void loadTasks() {
        Platform.runLater(() -> {
            taskData.clear();
            try {
                List<Task> tasks = DatabaseManager.getAllTasks();
                for (Task task : tasks) {
                    taskData.add(new TaskTableModel(task));
                }
            } catch (SQLException e) {
                showErrorDialog("加载任务失败", e.getMessage());
            }
        });
    }

    // 显示任务详情
    private void showTaskDetails(int taskId) {
        currentTaskId = taskId;
        Platform.runLater(() -> {
            detailPanelContainer.setCenter(null);
            try {
                List<TaskDetail> details = DatabaseManager.getTaskDetailsByTaskId(taskId);

                // 创建详情表格
                detailTable = new TableView<>();
                detailData = FXCollections.observableArrayList();
                detailTable.setItems(detailData);

                TableColumn<TaskDetailTableModel, String> fieldNameColumn = new TableColumn<>("字段名称");
                fieldNameColumn.setCellValueFactory(new PropertyValueFactory<>("fieldName"));
                fieldNameColumn.setPrefWidth(120);

                TableColumn<TaskDetailTableModel, String> fieldValueColumn = new TableColumn<>("字段值");
                fieldValueColumn.setCellValueFactory(new PropertyValueFactory<>("fieldValue"));
                fieldValueColumn.setPrefWidth(150);

                TableColumn<TaskDetailTableModel, String> dataTypeColumn = new TableColumn<>("数据类型");
                dataTypeColumn.setCellValueFactory(new PropertyValueFactory<>("dataType"));
                dataTypeColumn.setPrefWidth(100);

                TableColumn<TaskDetailTableModel, String> requiredColumn = new TableColumn<>("是否必填");
                requiredColumn.setCellValueFactory(new PropertyValueFactory<>("required"));
                requiredColumn.setPrefWidth(80);

                TableColumn<TaskDetailTableModel, String> descColumn = new TableColumn<>("说明");
                descColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
                descColumn.setPrefWidth(200);

                TableColumn<TaskDetailTableModel, Void> actionColumn = new TableColumn<>("操作");
                actionColumn.setPrefWidth(100);
                actionColumn.setCellFactory(param -> new DetailActionButtonTableCell());

                detailTable.getColumns().addAll(
                        fieldNameColumn, fieldValueColumn, dataTypeColumn,
                        requiredColumn, descColumn, actionColumn
                );

                // 添加数据
                for (TaskDetail detail : details) {
                    detailData.add(new TaskDetailTableModel(detail));
                }

                // 设置行高
                detailTable.setFixedCellSize(35);

                // 添加详情按钮面板
                HBox detailButtonPanel = new HBox(10);
                detailButtonPanel.setAlignment(Pos.CENTER_RIGHT);
                Button addDetailButton = new Button("添加详情");
                addDetailButton.setOnAction(e -> showAddDetailDialog());
                detailButtonPanel.getChildren().add(addDetailButton);

                // 创建详情面板
                VBox detailPanel = new VBox(10);
                detailPanel.setPadding(new Insets(10));
                detailPanel.getChildren().addAll(detailTable, detailButtonPanel);
                VBox.setVgrow(detailTable, Priority.ALWAYS);

                detailPanelContainer.setCenter(detailPanel);

            } catch (SQLException e) {
                Label errorLabel = new Label("加载任务详情失败: " + e.getMessage());
                errorLabel.setAlignment(Pos.CENTER);
                detailPanelContainer.setCenter(errorLabel);
            }
        });
    }

    // 开启所有定时任务
    private void startAllTasks() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认开启");
        confirmAlert.setHeaderText("确定要开启所有定时任务吗？");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    // 1. 更新所有任务的enabled状态为true
                    List<Task> tasks = DatabaseManager.getAllTasks();
                    for (Task task : tasks) {
                        task.setEnabled(true);
                        DatabaseManager.saveTask(task);
                    }

                    // 2. 重启TaskScheduler以重新加载所有任务
                    TaskScheduler.stop();
                    TaskScheduler.start();

                    // 3. 刷新表格显示
                    loadTasks();

                    showInfoDialog("成功", "所有定时任务已成功开启");
                } catch (SQLException e) {
                    showErrorDialog("开启定时任务失败", e.getMessage());
                }
            }
        });
    }

    // 关闭所有定时任务
    private void stopAllTasks() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认关闭");
        confirmAlert.setHeaderText("确定要关闭所有定时任务吗？");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    // 1. 更新所有任务的enabled状态为false
                    List<Task> tasks = DatabaseManager.getAllTasks();
                    for (Task task : tasks) {
                        task.setEnabled(false);
                        DatabaseManager.saveTask(task);
                    }

                    // 2. 停止TaskScheduler
                    TaskScheduler.stop();

                    // 3. 刷新表格显示
                    loadTasks();

                    showInfoDialog("成功", "所有定时任务已成功关闭");
                } catch (SQLException e) {
                    showErrorDialog("关闭定时任务失败", e.getMessage());
                }
            }
        });
    }

    // 显示新增任务对话框
    private void showAddTaskDialog() {
        showAddTaskDialog(null);
    }

    // 显示编辑任务对话框
    private void showAddTaskDialog(Task task) {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle(task == null ? "新增任务" : "编辑任务");
        dialog.setHeaderText(task == null ? "请填写任务信息" : "编辑任务信息");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField deviceIdField = new TextField();
        TextField cronField = new TextField();
        TextField descField = new TextField();
        TextField typeField = new TextField();
        TextField nameField = new TextField();

        // 如果是编辑模式，填充任务数据
        if (task != null) {
            deviceIdField.setText(task.getDeviceId());
            cronField.setText(task.getCronExpression());
            descField.setText(task.getDescription());
            typeField.setText(task.getTaskType());
            nameField.setText(task.getTaskName());
        }

        grid.add(new Label("设备号: *"), 0, 0);
        grid.add(deviceIdField, 1, 0);
        grid.add(new Label("定时任务cron: *"), 0, 1);
        grid.add(cronField, 1, 1);
        grid.add(new Label("说明: "), 0, 2);
        grid.add(descField, 1, 2);
        grid.add(new Label("任务类型: *"), 0, 3);
        grid.add(typeField, 1, 3);
        grid.add(new Label("任务名称: *"), 0, 4);
        grid.add(nameField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // 设置OK按钮的行为
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                // 验证必填字段
                if (deviceIdField.getText().trim().isEmpty() || 
                    cronField.getText().trim().isEmpty() || 
                    typeField.getText().trim().isEmpty() || 
                    nameField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "带*的字段为必填项");
                    return null;
                }

                if (task == null) {
                    // 新增模式
                    return new Task(
                            deviceIdField.getText().trim(),
                            cronField.getText().trim(),
                            descField.getText().trim(),
                            typeField.getText().trim(),
                            nameField.getText().trim()
                    );
                } else {
                    // 编辑模式
                    task.setDeviceId(deviceIdField.getText().trim());
                task.setCronExpression(cronField.getText().trim());
                task.setDescription(descField.getText().trim());
                task.setTaskType(typeField.getText().trim());
                task.setTaskName(nameField.getText().trim());
                    return task;
                }
            }
            return null;
        });

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(resultTask -> {
            try {
                DatabaseManager.saveTask(resultTask);
                loadTasks();
                showInfoDialog("成功", task == null ? "任务添加成功" : "任务更新成功");
            } catch (SQLException e) {
                showErrorDialog("操作失败", e.getMessage());
            }
        });
    }

    // 显示添加详情对话框
    private void showAddDetailDialog() {
        if (currentTaskId == -1) {
            showErrorDialog("错误", "请先选择一个任务");
            return;
        }

        Dialog<TaskDetail> dialog = new Dialog<>();
        dialog.setTitle("添加任务详情");
        dialog.setHeaderText("请填写任务详情信息");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField fieldNameField = new TextField();
        TextField fieldValueField = new TextField();
        TextField dataTypeField = new TextField();
        CheckBox requiredCheckBox = new CheckBox();
        TextField descField = new TextField();

        grid.add(new Label("字段名称: *"), 0, 0);
        grid.add(fieldNameField, 1, 0);
        grid.add(new Label("字段值: *"), 0, 1);
        grid.add(fieldValueField, 1, 1);
        grid.add(new Label("数据类型: *"), 0, 2);
        grid.add(dataTypeField, 1, 2);
        grid.add(new Label("是否必填: "), 0, 3);
        grid.add(requiredCheckBox, 1, 3);
        grid.add(new Label("说明: "), 0, 4);
        grid.add(descField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // 设置OK按钮的行为
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                // 验证必填字段
                if (fieldNameField.getText().trim().isEmpty() || 
                    fieldValueField.getText().trim().isEmpty() || 
                    dataTypeField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "带*的字段为必填项");
                    return null;
                }

                return new TaskDetail(
                        currentTaskId,
                        fieldNameField.getText().trim(),
                        fieldValueField.getText().trim(),
                        dataTypeField.getText().trim(),
                        requiredCheckBox.isSelected(),
                        descField.getText().trim()
                );
            }
            return null;
        });

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(detail -> {
            try {
                DatabaseManager.saveTaskDetail(detail);
                showTaskDetails(currentTaskId);
                showInfoDialog("成功", "任务详情添加成功");
            } catch (SQLException e) {
                showErrorDialog("操作失败", e.getMessage());
            }
        });
    }

    // 显示编辑详情对话框
    private void showEditDetailDialog(TaskDetail detail) {
        Dialog<TaskDetail> dialog = new Dialog<>();
        dialog.setTitle("编辑任务详情");
        dialog.setHeaderText("编辑任务详情信息");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField fieldNameField = new TextField(detail.getFieldName());
        TextField fieldValueField = new TextField(detail.getFieldValue());
        TextField dataTypeField = new TextField(detail.getDataType());
        CheckBox requiredCheckBox = new CheckBox();
        requiredCheckBox.setSelected(detail.isRequired());
        TextField descField = new TextField(detail.getDescription());

        grid.add(new Label("字段名称: *"), 0, 0);
        grid.add(fieldNameField, 1, 0);
        grid.add(new Label("字段值: *"), 0, 1);
        grid.add(fieldValueField, 1, 1);
        grid.add(new Label("数据类型: *"), 0, 2);
        grid.add(dataTypeField, 1, 2);
        grid.add(new Label("是否必填: "), 0, 3);
        grid.add(requiredCheckBox, 1, 3);
        grid.add(new Label("说明: "), 0, 4);
        grid.add(descField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // 设置OK按钮的行为
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                // 验证必填字段
                if (fieldNameField.getText().trim().isEmpty() || 
                    fieldValueField.getText().trim().isEmpty() || 
                    dataTypeField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "带*的字段为必填项");
                    return null;
                }

                detail.setFieldName(fieldNameField.getText().trim());
                detail.setFieldValue(fieldValueField.getText().trim());
                detail.setDataType(dataTypeField.getText().trim());
                detail.setRequired(requiredCheckBox.isSelected());
                detail.setDescription(descField.getText().trim());
                return detail;
            }
            return null;
        });

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(resultDetail -> {
            try {
                DatabaseManager.saveTaskDetail(resultDetail);
                showTaskDetails(currentTaskId);
                showInfoDialog("成功", "任务详情更新成功");
            } catch (SQLException e) {
                showErrorDialog("操作失败", e.getMessage());
            }
        });
    }

    // 删除任务
    private void deleteTask(int taskId) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText("确定要删除选中的任务吗？");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    DatabaseManager.deleteTask(taskId);
                    loadTasks();
                    // 如果删除的是当前查看详情的任务，清空详情面板
                    if (currentTaskId == taskId) {
                        detailPanelContainer.setCenter(new Label("请选择一个任务查看详情"));
                        currentTaskId = -1;
                    }
                    showInfoDialog("成功", "删除成功");
                } catch (SQLException e) {
                    showErrorDialog("删除失败", e.getMessage());
                }
            }
        });
    }

    // 删除任务详情
    private void deleteTaskDetail(int detailId) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText("确定要删除选中的详情项吗？");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    DatabaseManager.deleteTaskDetail(detailId);
                    showTaskDetails(currentTaskId);
                    showInfoDialog("成功", "删除成功");
                } catch (SQLException e) {
                    showErrorDialog("删除失败", e.getMessage());
                }
            }
        });
    }

    // 操作按钮TableCell
    private class ActionButtonTableCell extends TableCell<TaskTableModel, Void> {
        private final Button editButton = new Button("编辑");
        private final Button deleteButton = new Button("删除");
        private final HBox pane = new HBox(5, editButton, deleteButton);

        public ActionButtonTableCell() {
            editButton.setPrefSize(60, 25);
            deleteButton.setPrefSize(60, 25);
            pane.setAlignment(Pos.CENTER);
            
            editButton.setOnAction(event -> {
                TaskTableModel task = getTableView().getItems().get(getIndex());
                try {
                    Task realTask = DatabaseManager.getTaskById(task.getId());
                    if (realTask != null) {
                        showAddTaskDialog(realTask);
                    }
                } catch (SQLException e) {
                    showErrorDialog("获取任务信息失败", e.getMessage());
                }
            });

            deleteButton.setOnAction(event -> {
                TaskTableModel task = getTableView().getItems().get(getIndex());
                deleteTask(task.getId());
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : pane);
        }
    }

    // 详情操作按钮TableCell
    private class DetailActionButtonTableCell extends TableCell<TaskDetailTableModel, Void> {
        private final Button editButton = new Button("编辑");
        private final Button deleteButton = new Button("删除");
        private final HBox pane = new HBox(5, editButton, deleteButton);

        public DetailActionButtonTableCell() {
            editButton.setPrefSize(50, 25);
            deleteButton.setPrefSize(50, 25);
            pane.setAlignment(Pos.CENTER);

            editButton.setOnAction(event -> {
                TaskDetailTableModel detailModel = getTableView().getItems().get(getIndex());
                try {
                    // 查找对应的TaskDetail对象
                    List<TaskDetail> details = DatabaseManager.getTaskDetailsByTaskId(currentTaskId);
                    for (TaskDetail detail : details) {
                        if (detail.getId() == detailModel.getId()) {
                            showEditDetailDialog(detail);
                            break;
                        }
                    }
                } catch (SQLException e) {
                    showErrorDialog("获取详情失败", e.getMessage());
                }
            });

            deleteButton.setOnAction(event -> {
                TaskDetailTableModel detailModel = getTableView().getItems().get(getIndex());
                deleteTaskDetail(detailModel.getId());
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : pane);
        }
    }

    // 任务表格模型
    public static class TaskTableModel {
        private final int id;
        private final String deviceId;
        private final String cronExpression;
        private final String description;
        private final String taskType;
        private final String taskName;
        private final String status;

        public TaskTableModel(Task task) {
            this.id = task.getId();
            this.deviceId = task.getDeviceId();
            this.cronExpression = task.getCronExpression();
            this.description = task.getDescription();
            this.taskType = task.getTaskType();
            this.taskName = task.getTaskName();
            this.status = task.isEnabled() ? "启用" : "禁用";
        }

        public int getId() {
            return id;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public String getCronExpression() {
            return cronExpression;
        }

        public String getDescription() {
            return description;
        }

        public String getTaskType() {
            return taskType;
        }

        public String getTaskName() {
            return taskName;
        }

        public String getStatus() {
            return status;
        }
    }

    // 任务详情表格模型
    public static class TaskDetailTableModel {
        private final int id;
        private final String fieldName;
        private final String fieldValue;
        private final String dataType;
        private final String required;
        private final String description;

        public TaskDetailTableModel(TaskDetail detail) {
            this.id = detail.getId();
            this.fieldName = detail.getFieldName();
            this.fieldValue = detail.getFieldValue();
            this.dataType = detail.getDataType();
            this.required = detail.isRequired() ? "是" : "否";
            this.description = detail.getDescription();
        }

        public int getId() {
            return id;
        }

        public String getFieldName() {
            return fieldName;
        }

        public String getFieldValue() {
            return fieldValue;
        }

        public String getDataType() {
            return dataType;
        }

        public String getRequired() {
            return required;
        }

        public String getDescription() {
            return description;
        }
    }

    // 显示错误对话框
    private void showErrorDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // 显示信息对话框
    private void showInfoDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}