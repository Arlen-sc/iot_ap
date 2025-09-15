package com.iot.plc.scheduler;

import com.iot.plc.model.Task;
import com.iot.plc.database.DatabaseManager;
import com.iot.plc.logger.Logger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import java.sql.SQLException;
import java.util.List;

public class TaskScheduler {
    private static Scheduler scheduler;
    
    public static void start() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            scheduleAllTasks();
            initLogCleanupTask();
        } catch (SchedulerException e) {
            Logger.getInstance().error("调度器启动失败: " + e.getMessage());
        }
    }
    
    public static void stop() {
        try {
            if (scheduler != null && scheduler.isStarted()) {
                scheduler.shutdown();
            }
        } catch (SchedulerException e) {
            Logger.getInstance().error("调度器关闭失败: " + e.getMessage());
        }
    }
    
    private static void scheduleAllTasks() {
        try {
            List<Task> tasks = DatabaseManager.getAllTasks();
            for (Task task : tasks) {
                if (task.isEnabled()) {
                    try {
                        scheduleTask(task);
                    } catch (SchedulerException e) {
                        Logger.getInstance().error("调度任务失败: " + e.getMessage());
                    }
                }
            }
        } catch (SQLException e) {
            Logger.getInstance().error("加载任务失败: " + e.getMessage());
        }
    }
    
    public static void scheduleTask(Task task) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(PlcJob.class)
                .withIdentity("job_" + task.getId(), "plc_tasks")
                .usingJobData("deviceId", task.getDeviceId())
                .usingJobData("taskType", task.getTaskType())
                .build();
        
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger_" + task.getId(), "plc_tasks")
                .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
                .build();
        
        scheduler.scheduleJob(job, trigger);
    }
    
    public static void unscheduleTask(int taskId) throws SchedulerException {
        scheduler.deleteJob(new JobKey("job_" + taskId, "plc_tasks"));
    }
    
    private static void initLogCleanupTask() throws SchedulerException {
        // 检查日志清理任务是否已存在
        JobKey jobKey = new JobKey("logCleanupJob", "system_tasks");
        if (!scheduler.checkExists(jobKey)) {
            // 创建日志清理任务，每天凌晨1点执行
            JobDetail job = JobBuilder.newJob(LogCleanupJob.class)
                    .withIdentity(jobKey)
                    .build();
            
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("logCleanupTrigger", "system_tasks")
                    .withSchedule(CronScheduleBuilder.cronSchedule("0 0 1 * * ?")) // 每天凌晨1点执行
                    .build();
            
            scheduler.scheduleJob(job, trigger);
            Logger.getInstance().info("日志清理任务已初始化: 每天凌晨1点执行");
        } else {
            Logger.getInstance().info("日志清理任务已存在");
        }
    }
}