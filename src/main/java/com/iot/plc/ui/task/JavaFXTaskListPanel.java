package com.iot.plc.ui.task;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.TaskItem;
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
 * 任务列表管理面板
 * 用于管理和监控PLC任务的执行状态
 */
public class JavaFXTaskListPanel extends JavaFXBasePanel {
    private TableView<TaskTableModel> taskTable;
    private ObservableList<TaskTableModel> taskData;
    private BorderPane detailPanelContainer;
    private Button refreshButton;
    private Button addButton;

    public JavaFXTaskListPanel() {
        initComponents();
        loadData();
    }

    @Override
    protected void initComponents() {
        this.setPadding(new Insets(15));
        this.setSpacing(15);

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
        
        headerBox.getChildren().addAll(titleLabel, refreshButton, addButton);

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
                } else if (item.getBurnStatus().equals("进行中")) {
                    setStyle("-fx-background-color: #e6f3ff; -fx-font-weight: bold;");
                } else if (item.getBurnStatus().equals("已完成")) {
                    setStyle("-fx-background-color: #eeffee; -fx-text-fill: #006600;");
                } else if (item.getBurnStatus().equals("失败")) {
                    setStyle("-fx-background-color: #ffe6e6; -fx-text-fill: #cc0000;");
                }
            }
        });

        // 设置表格列
        TableColumn<TaskTableModel, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(60);

        TableColumn<TaskTableModel, String> nameColumn = new TableColumn<>("任务名称");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("taskName"));
        nameColumn.setPrefWidth(150);

        TableColumn<TaskTableModel, String> plcAddressColumn = new TableColumn<>("PLC地址");
        plcAddressColumn.setCellValueFactory(new PropertyValueFactory<>("plcAddress"));
        plcAddressColumn.setPrefWidth(120);

        TableColumn<TaskTableModel, Integer> barcodeCountColumn = new TableColumn<>("条码数量");
        barcodeCountColumn.setCellValueFactory(new PropertyValueFactory<>("barcodeCount"));
        barcodeCountColumn.setPrefWidth(90);

        TableColumn<TaskTableModel, String> startTimeColumn = new TableColumn<>("开始时间");
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        startTimeColumn.setPrefWidth(180);

        TableColumn<TaskTableModel, String> endTimeColumn = new TableColumn<>("结束时间");
        endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        endTimeColumn.setPrefWidth(180);

        TableColumn<TaskTableModel, String> burnStatusColumn = new TableColumn<>("烧录状态");
        burnStatusColumn.setCellValueFactory(new PropertyValueFactory<>("burnStatus"));
        burnStatusColumn.setPrefWidth(100);

        // 操作列
        TableColumn<TaskTableModel, Void> actionColumn = new TableColumn<>("操作");
        actionColumn.setPrefWidth(120);
        actionColumn.setCellFactory(param -> new ActionButtonTableCell());

        taskTable.getColumns().addAll(
                idColumn, nameColumn, plcAddressColumn, barcodeCountColumn,
                startTimeColumn, endTimeColumn, burnStatusColumn, actionColumn
        );
        taskTable.setFixedCellSize(40);

        // 监听表格选择事件
        taskTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showTaskDetails(newSelection.getId());
            }
        });

        // 创建任务详情面板
        detailPanelContainer = new BorderPane();
        detailPanelContainer.setPrefHeight(250);
        detailPanelContainer.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 4px;");
        Label detailPlaceholder = new Label("请选择一个任务查看详情");
        detailPlaceholder.setFont(Font.font(14));
        detailPlaceholder.setAlignment(Pos.CENTER);
        detailPanelContainer.setCenter(detailPlaceholder);

        // 添加所有组件到主面板
        this.getChildren().addAll(headerBox, taskTable, detailPanelContainer);

        // 设置布局权重
        VBox.setVgrow(taskTable, Priority.ALWAYS);
    }

    @Override
    protected void loadData() {
        Platform.runLater(() -> {
            taskData.clear();
            try {
                List<TaskItem> taskItems = DatabaseManager.getAllTasks();
                for (TaskItem item : taskItems) {
                    taskData.add(new TaskTableModel(item));
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
                TaskItem taskItem = DatabaseManager.getTaskById(taskId);
                if (taskItem == null) {
                    detailPanelContainer.setCenter(new Label("任务不存在或已被删除"));
                    return;
                }

                // 创建详情面板
                GridPane detailGrid = new GridPane();
                detailGrid.setHgap(15);
                detailGrid.setVgap(15);
                detailGrid.setPadding(new Insets(20));

                detailGrid.add(new Label("任务ID: "), 0, 0);
                detailGrid.add(new Label(String.valueOf(taskItem.getId())), 1, 0);

                detailGrid.add(new Label("任务名称: "), 0, 1);
                detailGrid.add(new Label(taskItem.getTaskName()), 1, 1);

                detailGrid.add(new Label("PLC地址: "), 0, 2);
                detailGrid.add(new Label(taskItem.getPlcAddress()), 1, 2);

                detailGrid.add(new Label("条码数量: "), 0, 3);
                detailGrid.add(new Label(String.valueOf(taskItem.getBarcodeCount())), 1, 3);

                detailGrid.add(new Label("已处理数量: "), 0, 4);
                detailGrid.add(new Label(String.valueOf(taskItem.getProcessedCount())), 1, 4);

                detailGrid.add(new Label("开始时间: "), 0, 5);
                detailGrid.add(new Label(taskItem.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))), 1, 5);

                detailGrid.add(new Label("结束时间: "), 0, 6);
                detailGrid.add(new Label(taskItem.getEndTime() != null ? 
                        taskItem.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")) : "未结束"), 1, 6);

                detailGrid.add(new Label("烧录状态: "), 0, 7);
                detailGrid.add(new Label(taskItem.getBurnStatus()), 1, 7);

                detailGrid.add(new Label("备注: "), 0, 8);
                detailGrid.add(new Label(taskItem.getRemark() != null ? taskItem.getRemark() : "无"), 1, 8);

                // 添加操作按钮
                HBox buttonBox = new HBox(10);
                buttonBox.setAlignment(Pos.CENTER_RIGHT);
                Button updateButton = new Button("更新任务");
                Button deleteButton = new Button("删除任务");
                
                updateButton.setOnAction(e -> showUpdateTaskDialog(taskItem));
                deleteButton.setOnAction(e -> deleteTask(taskItem.getId()));
                
                buttonBox.getChildren().addAll(updateButton, deleteButton);

                VBox detailPanel = new VBox(15);
                detailPanel.getChildren().addAll(detailGrid, buttonBox);

                detailPanelContainer.setCenter(detailPanel);

            } catch (SQLException e) {
                Label errorLabel = new Label("加载任务详情失败: " + e.getMessage());
                detailPanelContainer.setCenter(errorLabel);
            }
        });
    }

    /**
     * 显示新增任务对话框
     */
    private void showAddTaskDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("新增任务");
        dialog.setHeaderText("创建新的烧录任务");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("请输入任务名称");
        TextField plcAddressField = new TextField();
        plcAddressField.setPromptText("请输入PLC地址");
        TextField barcodeCountField = new TextField();
        barcodeCountField.setPromptText("请输入条码数量");
        TextField remarkField = new TextField();
        remarkField.setPromptText("请输入备注（可选）");

        grid.add(new Label("任务名称: *"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("PLC地址: *"), 0, 1);
        grid.add(plcAddressField, 1, 1);
        grid.add(new Label("条码数量: *"), 0, 2);
        grid.add(barcodeCountField, 1, 2);
        grid.add(new Label("备注: "), 0, 3);
        grid.add(remarkField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // 验证输入
                if (nameField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "任务名称不能为空");
                    return null;
                }
                if (plcAddressField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "PLC地址不能为空");
                    return null;
                }
                if (barcodeCountField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "条码数量不能为空");
                    return null;
                }

                try {
                    int count = Integer.parseInt(barcodeCountField.getText().trim());
                    if (count <= 0) {
                        showErrorDialog("错误", "条码数量必须大于0");
                        return null;
                    }

                    // 创建任务
                    TaskItem taskItem = new TaskItem();
                    taskItem.setTaskName(nameField.getText().trim());
                    taskItem.setPlcAddress(plcAddressField.getText().trim());
                    taskItem.setBarcodeCount(count);
                    taskItem.setProcessedCount(0);
                    taskItem.setStartTime(LocalDateTime.now());
                    taskItem.setBurnStatus("等待中");
                    taskItem.setRemark(remarkField.getText().trim());

                    DatabaseManager.addTask(taskItem);
                    loadData();
                    showInfoDialog("成功", "任务创建成功");
                } catch (NumberFormatException e) {
                    showErrorDialog("错误", "条码数量必须是数字");
                } catch (SQLException e) {
                    showErrorDialog("错误", "创建任务失败: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    /**
     * 显示更新任务对话框
     */
    private void showUpdateTaskDialog(TaskItem taskItem) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("更新任务");
        dialog.setHeaderText("修改任务信息");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(taskItem.getTaskName());
        TextField plcAddressField = new TextField(taskItem.getPlcAddress());
        TextField remarkField = new TextField(taskItem.getRemark() != null ? taskItem.getRemark() : "");

        grid.add(new Label("任务名称: *"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("PLC地址: *"), 0, 1);
        grid.add(plcAddressField, 1, 1);
        grid.add(new Label("备注: "), 0, 2);
        grid.add(remarkField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                // 验证输入
                if (nameField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "任务名称不能为空");
                    return null;
                }
                if (plcAddressField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "PLC地址不能为空");
                    return null;
                }

                try {
                    taskItem.setTaskName(nameField.getText().trim());
                    taskItem.setPlcAddress(plcAddressField.getText().trim());
                    taskItem.setRemark(remarkField.getText().trim());

                    DatabaseManager.updateTask(taskItem);
                    loadData();
                    showInfoDialog("成功", "任务更新成功");
                } catch (SQLException e) {
                    showErrorDialog("错误", "更新任务失败: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
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
                    DatabaseManager.deleteTask(taskId);
                    loadData();
                    showInfoDialog("成功", "任务删除成功");
                } catch (SQLException e) {
                    showErrorDialog("错误", "删除任务失败: " + e.getMessage());
                }
            }
        });
    }

    // 操作按钮单元格
    private class ActionButtonTableCell extends TableCell<TaskTableModel, Void> {
        private final Button viewButton = new Button("查看");
        private final Button editButton = new Button("编辑");

        ActionButtonTableCell() {
            HBox buttons = new HBox(5);
            buttons.getChildren().addAll(viewButton, editButton);
            buttons.setAlignment(Pos.CENTER);

            viewButton.setOnAction(e -> {
                TaskTableModel model = getTableView().getItems().get(getIndex());
                showTaskDetails(model.getId());
            });

            editButton.setOnAction(e -> {
                TaskTableModel model = getTableView().getItems().get(getIndex());
                try {
                    TaskItem taskItem = DatabaseManager.getTaskById(model.getId());
                    if (taskItem != null) {
                        showUpdateTaskDialog(taskItem);
                    }
                } catch (SQLException ex) {
                    showErrorDialog("错误", "获取任务失败: " + ex.getMessage());
                }
            });

            setGraphic(buttons);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : getGraphic());
        }
    }

    // 任务表格模型
    public static class TaskTableModel {
        private final int id;
        private final String taskName;
        private final String plcAddress;
        private final int barcodeCount;
        private final String startTime;
        private final String endTime;
        private final String burnStatus;

        public TaskTableModel(TaskItem taskItem) {
            this.id = taskItem.getId();
            this.taskName = taskItem.getTaskName();
            this.plcAddress = taskItem.getPlcAddress();
            this.barcodeCount = taskItem.getBarcodeCount();
            this.startTime = taskItem.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            this.endTime = taskItem.getEndTime() != null ? 
                    taskItem.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "未结束";
            this.burnStatus = taskItem.getBurnStatus();
        }

        public int getId() {
            return id;
        }

        public String getTaskName() {
            return taskName;
        }

        public String getPlcAddress() {
            return plcAddress;
        }

        public int getBarcodeCount() {
            return barcodeCount;
        }

        public String getStartTime() {
            return startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public String getBurnStatus() {
            return burnStatus;
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