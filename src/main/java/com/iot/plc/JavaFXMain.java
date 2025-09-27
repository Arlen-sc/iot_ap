package com.iot.plc;

import com.iot.plc.logger.Logger;
import com.iot.plc.ui.JavaFXLogPanel;
import com.iot.plc.ui.LogsManagementPanel;
import com.iot.plc.ui.JavaFXConfigPanel;
import com.iot.plc.ui.AutoProcessPanel;
import com.iot.plc.service.ConfigService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class JavaFXMain extends Application {
    private static TabPane tabbedPane;

    @Override
    public void start(Stage primaryStage) {
        // 初始化配置服务
        ConfigService.getInstance();
        Logger.getInstance().info("配置服务已初始化");

        // 创建主布局
        VBox root = new VBox();
        tabbedPane = new TabPane();

        // 配置界面
        JavaFXConfigPanel configPanel = new JavaFXConfigPanel();
        Tab configTab = new Tab("配置管理",configPanel);
        configTab.setClosable(false);

        // 创建自动控制面板
        AutoProcessPanel autoControlPanel = new AutoProcessPanel();
        Tab autoControlTab = new Tab("自动控制", autoControlPanel);
        autoControlTab.setClosable(false);
        
        // 运行日志界面
        JavaFXLogPanel logPanel = new JavaFXLogPanel();
        Tab logTab = new Tab("运行日志", logPanel);
        logTab.setClosable(false);

        // 日志管理界面
        LogsManagementPanel logsManagementPanel = new LogsManagementPanel();
        Tab logsManagementTab = new Tab("日志管理", logsManagementPanel);
        logsManagementTab.setClosable(false);

        // 将标签页添加到标签面板
        tabbedPane.getTabs().addAll(autoControlTab, configTab, logTab, logsManagementTab);

        // 将LogPanel实例传递给Logger
        Logger.getInstance().setLogPanel(logPanel);
        Logger.getInstance().info("PLC任务管理系统启动成功");

        // 设置场景和舞台
        root.getChildren().add(tabbedPane);
        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setTitle("PLC任务管理系统");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}