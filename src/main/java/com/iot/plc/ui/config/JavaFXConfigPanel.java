package com.iot.plc.ui.config;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.ConfigItem;
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
import java.util.List;

/**
 * 系统配置管理面板
 */
public class JavaFXConfigPanel extends JavaFXBasePanel {
    private TableView<ConfigTableModel> configTable;
    private ObservableList<ConfigTableModel> configData;
    private BorderPane detailPanelContainer;

    public JavaFXConfigPanel() {
        initComponents();
        loadData();
    }

    @Override
    protected void initComponents() {
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
        refreshButton.setOnAction(e -> refresh());

        buttonPanel.getChildren().addAll(addButton, refreshButton);

        // 添加所有组件到主面板
        this.getChildren().addAll(titleLabel, configTable, detailPanelContainer, buttonPanel);

        // 设置表格自适应高度
        VBox.setVgrow(configTable, Priority.ALWAYS);
    }


    @Override
    protected void loadData() {
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

    @Override
    public void refresh() {
        loadData();
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
                detailPanelContainer.setCenter(errorLabel);
            }
        });
    }

    // 显示添加配置对话框
    private void showAddConfigDialog() {
        // 原实现保留
    }

    // 显示更新配置对话框
    private void showUpdateConfigDialog(ConfigItem configItem) {
        // 原实现保留，但允许修改备注
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("更新配置");
        dialog.setHeaderText("修改配置值和说明");

        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField valueField = new TextField(configItem.getConfigValue());
        TextField descField = new TextField(configItem.getDescription());

        grid.add(new Label("配置键: "), 0, 0);
        grid.add(new Label(configItem.getConfigKey()), 1, 0);
        grid.add(new Label("配置值: "), 0, 1);
        grid.add(valueField, 1, 1);
        grid.add(new Label("说明: "), 0, 2);
        grid.add(descField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    configItem.setConfigValue(valueField.getText().trim());
                    configItem.setDescription(descField.getText().trim());
                    DatabaseManager.saveConfigItem(configItem);
                    loadData();
                    showInfoDialog("成功", "配置项更新成功");
                } catch (SQLException e) {
                    showErrorDialog("错误", "更新配置项失败: " + e.getMessage());
                }
            }
            return null;
        });

        dialog.showAndWait();
    }

    // 操作按钮单元格
    private class ActionButtonTableCell extends TableCell<ConfigTableModel, Void> {
        private final Button editButton = new Button("编辑");
        private final Button deleteButton = new Button("删除");

        ActionButtonTableCell() {
            HBox buttons = new HBox(5);
            buttons.getChildren().addAll(editButton, deleteButton);
            buttons.setAlignment(Pos.CENTER);

            editButton.setOnAction(e -> {
                ConfigTableModel model = getTableView().getItems().get(getIndex());
                try {
                    ConfigItem configItem = DatabaseManager.getConfigItemById(model.getId());
                    if (configItem != null) {
                        showUpdateConfigDialog(configItem);
                    }
                } catch (SQLException ex) {
                    showErrorDialog("错误", "获取配置项失败: " + ex.getMessage());
                }
            });

            deleteButton.setOnAction(e -> {
                ConfigTableModel model = getTableView().getItems().get(getIndex());
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("确认删除");
                alert.setHeaderText("删除配置项");
                alert.setContentText("确定要删除配置项 '" + model.getConfigKey() + "' 吗？");

                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            DatabaseManager.deleteConfigItem(model.getId());
                            loadData();
                            showInfoDialog("成功", "配置项删除成功");
                        } catch (SQLException ex) {
                            showErrorDialog("错误", "删除配置项失败: " + ex.getMessage());
                        }
                    }
                });
            });

            setGraphic(buttons);
        }

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : getGraphic());
        }
    }

    // 配置表格模型
    public static class ConfigTableModel {
        private final int id;
        private final String configKey;
        private final String configValue;
        private final String description;
        private final String dataType;
        private final boolean required;

        public ConfigTableModel(ConfigItem configItem) {
            this.id = configItem.getId();
            this.configKey = configItem.getConfigKey();
            this.configValue = configItem.getConfigValue();
            this.description = configItem.getDescription();
            this.dataType = configItem.getDataType();
            this.required = configItem.isRequired();
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
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // 显示信息对话框
    private void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}