package com.iot.plc.ui;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.ConfigItem;
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
import java.util.List;

public class JavaFXConfigPanel extends VBox {
    private TableView<ConfigTableModel> configTable;
    private ObservableList<ConfigTableModel> configData;
    private BorderPane detailPanelContainer;

    public JavaFXConfigPanel() {
        initComponents();
        loadConfigItems();
    }

    private void initComponents() {
        // 设置布局
        this.setPadding(new Insets(10));
        this.setSpacing(10);

        // 创建标题
        Label titleLabel = new Label("系统配置管理");
        titleLabel.setFont(Font.font(18));
        titleLabel.setAlignment(Pos.CENTER_LEFT);

        // 创建配置表格
        configTable = new TableView<>();
        configData = FXCollections.observableArrayList();
        configTable.setItems(configData);
        configTable.setRowFactory(tv -> new TableRow<ConfigTableModel>() {
            @Override
            protected void updateItem(ConfigTableModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (configTable.getSelectionModel().isSelected(getIndex())) {
                    setStyle("-fx-background-color: #cceeff;");
                }
            }
        });

        // 设置表格列
        TableColumn<ConfigTableModel, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);

        TableColumn<ConfigTableModel, String> keyColumn = new TableColumn<>("配置项");
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("configKey"));
        keyColumn.setPrefWidth(150);

        TableColumn<ConfigTableModel, String> valueColumn = new TableColumn<>("配置值");
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("configValue"));
        valueColumn.setPrefWidth(200);

        TableColumn<ConfigTableModel, String> descColumn = new TableColumn<>("说明");
        descColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        descColumn.setPrefWidth(250);

        TableColumn<ConfigTableModel, String> typeColumn = new TableColumn<>("数据类型");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("dataType"));
        typeColumn.setPrefWidth(100);

        TableColumn<ConfigTableModel, Boolean> requiredColumn = new TableColumn<>("是否必填");
        requiredColumn.setCellValueFactory(new PropertyValueFactory<>("required"));
        requiredColumn.setPrefWidth(80);
        requiredColumn.setCellFactory(column -> {
            return new TableCell<ConfigTableModel, Boolean>() {
                @Override
                protected void updateItem(Boolean item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText("");
                    } else {
                        setText(item ? "是" : "否");
                    }
                }
            };
        });

        // 操作列
        TableColumn<ConfigTableModel, Void> actionColumn = new TableColumn<>("操作");
        actionColumn.setPrefWidth(100);
        actionColumn.setCellFactory(param -> new ActionButtonTableCell());

        configTable.getColumns().addAll(
                idColumn, keyColumn, valueColumn, descColumn,
                typeColumn, requiredColumn, actionColumn
        );

        // 设置表格行高
        configTable.setFixedCellSize(40);

        // 监听表格选择事件
        configTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showConfigDetails(newSelection.getId());
            }
        });

        // 创建配置详情面板
        detailPanelContainer = new BorderPane();
        detailPanelContainer.setPrefHeight(200);
        detailPanelContainer.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1px; -fx-border-radius: 4px;");
        Label detailPlaceholder = new Label("请选择一个配置项查看详情");
        detailPlaceholder.setFont(Font.font(14));
        detailPlaceholder.setAlignment(Pos.CENTER);
        detailPanelContainer.setCenter(detailPlaceholder);

        // 创建按钮面板
        HBox buttonPanel = new HBox(10);
        buttonPanel.setAlignment(Pos.CENTER_RIGHT);
        Button addButton = new Button("新增");
        Button refreshButton = new Button("刷新");

        addButton.setOnAction(e -> showAddConfigDialog());
        refreshButton.setOnAction(e -> loadConfigItems());

        buttonPanel.getChildren().addAll(addButton, refreshButton);

        // 添加所有组件到主面板
        this.getChildren().addAll(titleLabel, configTable, detailPanelContainer, buttonPanel);

        // 设置表格自适应高度
        VBox.setVgrow(configTable, Priority.ALWAYS);
    }

    // 加载配置项
    private void loadConfigItems() {
        Platform.runLater(() -> {
            configData.clear();
            try {
                List<ConfigItem> configItems = DatabaseManager.getAllConfigItems();
                for (ConfigItem item : configItems) {
                    configData.add(new ConfigTableModel(item));
                }
            } catch (SQLException e) {
                showErrorDialog("加载配置项失败", e.getMessage());
            }
        });
    }

    // 显示配置详情
    private void showConfigDetails(int configId) {
        Platform.runLater(() -> {
            try {
                ConfigItem configItem = DatabaseManager.getConfigItemById(configId);
                if (configItem == null) {
                    detailPanelContainer.setCenter(new Label("配置项不存在或已被删除"));
                    return;
                }

                // 创建详情面板
                GridPane detailGrid = new GridPane();
                detailGrid.setHgap(10);
                detailGrid.setVgap(10);
                detailGrid.setPadding(new Insets(20));

                detailGrid.add(new Label("配置项ID: "), 0, 0);
                detailGrid.add(new Label(String.valueOf(configItem.getId())), 1, 0);

                detailGrid.add(new Label("配置键: "), 0, 1);
                detailGrid.add(new Label(configItem.getConfigKey()), 1, 1);

                detailGrid.add(new Label("配置值: "), 0, 2);
                detailGrid.add(new Label(configItem.getConfigValue()), 1, 2);

                detailGrid.add(new Label("说明: "), 0, 3);
                detailGrid.add(new Label(configItem.getDescription()), 1, 3);

                detailGrid.add(new Label("数据类型: "), 0, 4);
                detailGrid.add(new Label(configItem.getDataType()), 1, 4);

                detailGrid.add(new Label("是否必填: "), 0, 5);
                detailGrid.add(new Label(configItem.isRequired() ? "是" : "否"), 1, 5);

                // 添加更新按钮
                HBox buttonBox = new HBox(10);
                buttonBox.setAlignment(Pos.CENTER_RIGHT);
                Button updateButton = new Button("更新配置值");
                updateButton.setOnAction(e -> showUpdateConfigDialog(configItem));
                buttonBox.getChildren().add(updateButton);

                VBox detailPanel = new VBox(10);
                detailPanel.getChildren().addAll(detailGrid, buttonBox);

                detailPanelContainer.setCenter(detailPanel);

            } catch (SQLException e) {
                Label errorLabel = new Label("加载配置详情失败: " + e.getMessage());
                errorLabel.setAlignment(Pos.CENTER);
                detailPanelContainer.setCenter(errorLabel);
            }
        });
    }

    // 显示新增配置对话框
    private void showAddConfigDialog() {
        Dialog<ConfigItem> dialog = new Dialog<>();
        dialog.setTitle("新增配置项");
        dialog.setHeaderText("请填写配置项信息");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField keyField = new TextField();
        TextField valueField = new TextField();
        TextField descField = new TextField();
        TextField typeField = new TextField();
        CheckBox requiredCheckBox = new CheckBox();

        grid.add(new Label("配置项: *"), 0, 0);
        grid.add(keyField, 1, 0);
        grid.add(new Label("配置值: *"), 0, 1);
        grid.add(valueField, 1, 1);
        grid.add(new Label("说明: "), 0, 2);
        grid.add(descField, 1, 2);
        grid.add(new Label("数据类型: *"), 0, 3);
        grid.add(typeField, 1, 3);
        grid.add(new Label("是否必填: "), 0, 4);
        grid.add(requiredCheckBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // 设置OK按钮的行为
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                // 验证必填字段
                if (keyField.getText().trim().isEmpty() || 
                    valueField.getText().trim().isEmpty() || 
                    typeField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "带*的字段为必填项");
                    return null;
                }

                return new ConfigItem(
                        keyField.getText().trim(),
                        valueField.getText().trim(),
                        descField.getText().trim(),
                        typeField.getText().trim(),
                        requiredCheckBox.isSelected()
                );
            }
            return null;
        });

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(configItem -> {
            try {
                DatabaseManager.saveConfigItem(configItem);
                loadConfigItems();
                showInfoDialog("成功", "配置项添加成功");
            } catch (SQLException e) {
                showErrorDialog("操作失败", e.getMessage());
            }
        });
    }

    // 显示更新配置值对话框
    private void showUpdateConfigDialog(ConfigItem configItem) {
        Dialog<ConfigItem> dialog = new Dialog<>();
        dialog.setTitle("更新配置值");
        dialog.setHeaderText("请输入新的配置值");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // 创建表单
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        Label keyLabel = new Label(configItem.getConfigKey());
        TextField valueField = new TextField(configItem.getConfigValue());
        Label descLabel = new Label(configItem.getDescription());

        grid.add(new Label("配置项: "), 0, 0);
        grid.add(keyLabel, 1, 0);
        grid.add(new Label("配置值: *"), 0, 1);
        grid.add(valueField, 1, 1);
        grid.add(new Label("说明: "), 0, 2);
        grid.add(descLabel, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // 设置OK按钮的行为
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                // 验证必填字段
                if (valueField.getText().trim().isEmpty()) {
                    showErrorDialog("错误", "配置值不能为空");
                    return null;
                }

                configItem.setConfigValue(valueField.getText().trim());
                return configItem;
            }
            return null;
        });

        // 显示对话框并处理结果
        dialog.showAndWait().ifPresent(result -> {
            try {
                DatabaseManager.saveConfigItem(result);
                loadConfigItems();
                showInfoDialog("成功", "配置值更新成功");
            } catch (SQLException e) {
                showErrorDialog("操作失败", e.getMessage());
            }
        });
    }

    // 操作按钮TableCell
    private class ActionButtonTableCell extends TableCell<ConfigTableModel, Void> {
        private final Button editButton = new Button("编辑");
        private final Button deleteButton = new Button("删除");
        private final HBox pane = new HBox(5, editButton, deleteButton);

        public ActionButtonTableCell() {
            editButton.setPrefSize(50, 25);
            deleteButton.setPrefSize(50, 25);
            pane.setAlignment(Pos.CENTER);

            editButton.setOnAction(event -> {
                ConfigTableModel config = getTableView().getItems().get(getIndex());
                try {
                    ConfigItem realConfig = DatabaseManager.getConfigItemById(config.getId());
                    if (realConfig != null) {
                        showUpdateConfigDialog(realConfig);
                    }
                } catch (SQLException e) {
                    showErrorDialog("获取配置信息失败", e.getMessage());
                }
            });

            deleteButton.setOnAction(event -> {
                ConfigTableModel config = getTableView().getItems().get(getIndex());
                deleteConfigItem(config.getId());
            });
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : pane);
        }
    }

    // 删除配置项
    private void deleteConfigItem(int configId) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("确认删除");
        confirmAlert.setHeaderText("确定要删除选中的配置项吗？");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    DatabaseManager.deleteConfigItem(configId);
                    loadConfigItems();
                    // 清空详情面板
                    detailPanelContainer.setCenter(new Label("请选择一个配置项查看详情"));
                    showInfoDialog("成功", "删除成功");
                } catch (SQLException e) {
                    showErrorDialog("删除失败", e.getMessage());
                }
            }
        });
    }

    // 配置表格模型
    public static class ConfigTableModel {
        private final int id;
        private final String configKey;
        private final String configValue;
        private final String description;
        private final String dataType;
        private final boolean required;

        public ConfigTableModel(ConfigItem item) {
            this.id = item.getId();
            this.configKey = item.getConfigKey();
            this.configValue = item.getConfigValue();
            this.description = item.getDescription();
            this.dataType = item.getDataType();
            this.required = item.isRequired();
        }

        public int getId() {
            return id;
        }

        public String getConfigKey() {
            return configKey;
        }

        public String getConfigValue() {
            return configValue;
        }

        public String getDescription() {
            return description;
        }

        public String getDataType() {
            return dataType;
        }

        public boolean isRequired() {
            return required;
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