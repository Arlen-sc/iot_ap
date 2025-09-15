package com.iot.plc;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.logger.Logger;
import com.iot.plc.model.Task;
import com.iot.plc.scheduler.TaskScheduler;
import com.iot.plc.service.AutoProcessService;
import com.iot.plc.service.AutoControlService;
import com.iot.plc.ui.JavaFXConfigPanel;
import com.iot.plc.ui.JavaFXLogPanel;
import com.iot.plc.ui.LogsManagementPanel;
// import com.iot.plc.ui.JavaFXAutoProcessPanel;
import com.iot.plc.ui.AutoProcessPanel;
import com.iot.plc.service.TaskListService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class JavaFXMain extends Application {
    private static JavaFXConfigPanel configPanel;
    private static TabPane tabbedPane;

    public static void showConfigPanelForTask(int taskId) {
        // 在JavaFX中，我们直接切换到配置管理标签页
        if (tabbedPane != null) {
            tabbedPane.getSelectionModel().select(1); // 切换到配置管理标签页
        }
    }

    @Override
    public void start(Stage primaryStage) {
        // 初始化数据库和调度器
        try {
            List<Task> tasks = DatabaseManager.getAllTasks();
            System.out.println("数据库中的任务数量: " + tasks.size());
            for (Task task : tasks) {
                System.out.println("任务ID: " + task.getId() + 
                        ", cron表达式: " + task.getCronExpression());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 启动定时任务调度器
        TaskScheduler.start();

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            TaskScheduler.stop();
            System.out.println("应用程序已关闭");
        }));

        // 临时注释：跳过自动处理流程服务和自动控制服务的初始化，因为它们依赖Netty
        // AutoProcessService autoProcessService = AutoProcessService.getInstance();
        // Logger.getInstance().info("自动处理流程服务已初始化");
        
        // AutoControlService autoControlService = AutoControlService.getInstance();
        // Logger.getInstance().info("自动控制服务已初始化");

        // 初始化任务列表服务（不依赖Netty）
        TaskListService taskListService = TaskListService.getInstance();
        Logger.getInstance().info("任务列表服务已初始化");

        // 创建主布局
        VBox root = new VBox();
        tabbedPane = new TabPane();

        // 任务列表界面
        Tab taskListTab = new Tab("任务列表");
        // 注意：这里不再创建UI面板，仅保留标签页以保持界面结构
        taskListTab.setClosable(false);

        // 配置界面
        configPanel = new JavaFXConfigPanel();
        Tab configTab = new Tab("配置管理", configPanel);
        configTab.setClosable(false);

        // 创建自动处理面板
        AutoProcessPanel autoProcessPanel = new AutoProcessPanel();
        Tab autoProcessTab = new Tab("自动处理", autoProcessPanel);
        autoProcessTab.setClosable(false);
        
        // 运行日志界面
        JavaFXLogPanel logPanel = new JavaFXLogPanel();
        Tab logTab = new Tab("运行日志", logPanel);
        logTab.setClosable(false);

        // 日志管理界面
        LogsManagementPanel logsManagementPanel = new LogsManagementPanel();
        Tab logsManagementTab = new Tab("日志管理", logsManagementPanel);
        logsManagementTab.setClosable(false);

        // 将标签页添加到标签面板 - 自动处理作为运行界面（第一个标签页）
        tabbedPane.getTabs().addAll(autoProcessTab, taskListTab, configTab, logTab, logsManagementTab);

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