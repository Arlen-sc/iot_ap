package com.iot.plc.scheduler;

import com.iot.plc.logger.Logger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class TaskScheduler {
    private Scheduler scheduler;
    private final ConcurrentHashMap<String, JobKey> scheduledJobs = new ConcurrentHashMap<>();
    private static volatile TaskScheduler instance;

    private TaskScheduler() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            initLogCleanupTask();
        } catch (SchedulerException e) {
            Logger.getInstance().error("初始化调度器失败: " + e.getMessage());
        }
    }

    public static TaskScheduler getInstance() {
        if (instance == null) {
            synchronized (TaskScheduler.class) {
                if (instance == null) {
                    instance = new TaskScheduler();
                }
            }
        }
        return instance;
    }

    public void start() {
        try {
            if (scheduler != null && !scheduler.isStarted()) {
                scheduler.start();
                Logger.getInstance().info("任务调度器已启动");
            }
        } catch (SchedulerException e) {
            Logger.getInstance().error("启动调度器失败: " + e.getMessage());
        }
    }

    public void stop() {
        try {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdown(true);
                Logger.getInstance().info("任务调度器已停止");
            }
        } catch (SchedulerException e) {
            Logger.getInstance().error("停止调度器失败: " + e.getMessage());
        }
    }

    public void scheduleTask(String taskId, Class<? extends Job> jobClass, String cronExpression) {
        try {
            JobKey jobKey = new JobKey("job_" + taskId, "group_" + taskId);
            TriggerKey triggerKey = new TriggerKey("trigger_" + taskId, "group_" + taskId);

            // 检查任务是否已存在
            if (scheduler.checkExists(jobKey)) {
                Logger.getInstance().info("任务已存在，跳过创建: " + taskId);
                return;
            }

            JobDetail jobDetail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobKey)
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            scheduledJobs.put(taskId, jobKey);
            Logger.getInstance().info("任务已调度: " + taskId + "，表达式: " + cronExpression);
        } catch (SchedulerException e) {
            Logger.getInstance().error("调度任务失败: " + e.getMessage());
        }
    }

    public void unscheduleTask(String taskId) {
        try {
            JobKey jobKey = scheduledJobs.remove(taskId);
            if (jobKey != null && scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                Logger.getInstance().info("任务已取消调度: " + taskId);
            }
        } catch (SchedulerException e) {
            Logger.getInstance().error("取消调度任务失败: " + e.getMessage());
        }
    }

    private void initLogCleanupTask() {
        try {
            // 日志清理任务的JobKey
            JobKey logCleanupJobKey = new JobKey("logCleanupJob", "systemGroup");
            
            // 检查日志清理任务是否已经存在
            if (!scheduler.checkExists(logCleanupJobKey)) {
                // 创建日志清理任务的JobDetail
                JobDetail logCleanupJob = JobBuilder.newJob(LogCleanupJob.class)
                        .withIdentity(logCleanupJobKey)
                        .build();
                
                // 创建日志清理任务的CronTrigger - 每天午夜24点执行
                CronTrigger logCleanupTrigger = TriggerBuilder.newTrigger()
                        .withIdentity("logCleanupTrigger", "systemGroup")
                        .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?")) // 每天午夜24点执行
                        .build();
                
                // 调度日志清理任务
                scheduler.scheduleJob(logCleanupJob, logCleanupTrigger);
                Logger.getInstance().info("日志清理任务已初始化，每天午夜24点执行");
            } else {
                Logger.getInstance().info("日志清理任务已存在，跳过初始化");
            }
        } catch (SchedulerException e) {
            Logger.getInstance().error("初始化日志清理任务失败: " + e.getMessage());
        }
    }
}