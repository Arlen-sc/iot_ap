package com.iot.plc;

import com.iot.plc.MainApp;
import com.iot.plc.database.DatabaseManager;
import com.iot.plc.scheduler.TaskScheduler;
import com.iot.plc.service.AutoProcessService;
import com.iot.plc.service.AutoControlService;
import com.iot.plc.model.Task;
import com.iot.plc.logger.Logger;
import java.sql.SQLException;
import java.util.List;

public class MainApp {
    public static void showConfigPanelForTask(int taskId) {
        try {
            Task task = DatabaseManager.getTaskById(taskId);
            Logger.getInstance().info("查看任务ID: " + taskId + " 的详细信息");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        // 检查数据库中的任务数据
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
        
        // 临时注释：跳过自动处理流程服务和自动控制服务的初始化，因为它们依赖Netty
        // AutoProcessService autoProcessService = AutoProcessService.getInstance();
        // Logger.getInstance().info("自动处理流程服务已初始化");
        
        // AutoControlService autoControlService = AutoControlService.getInstance();
        // Logger.getInstance().info("自动控制服务已初始化");
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            TaskScheduler.stop();
            System.out.println("应用程序已关闭");
        }));

        Logger.getInstance().info("PLC任务管理系统启动成功");
    }
}