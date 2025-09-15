package com.iot.plc.service;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.Task;
import com.iot.plc.model.TaskDetail;
import com.iot.plc.scheduler.TaskScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 任务列表服务类
 * 负责定时任务的管理、调度和执行
 */
public class TaskListService {
    private static final Logger logger = LoggerFactory.getLogger(TaskListService.class);
    private static TaskListService instance;
    private final ExecutorService executorService;

    /**
     * 单例模式私有构造函数
     */
    private TaskListService() {
        this.executorService = Executors.newSingleThreadExecutor();
        logger.info("TaskListService 初始化成功");
    }

    /**
     * 获取单例实例
     * @return TaskListService实例
     */
    public static synchronized TaskListService getInstance() {
        if (instance == null) {
            instance = new TaskListService();
        }
        return instance;
    }

    /**
     * 加载所有任务
     * @return 任务列表
     */
    public List<Task> loadTasks() {
        try {
            List<Task> tasks = DatabaseManager.getAllTasks();
            logger.info("加载任务成功，共加载 {} 个任务", tasks.size());
            return tasks;
        } catch (SQLException e) {
            logger.error("加载任务失败: {}", e.getMessage(), e);
            throw new RuntimeException("加载任务失败", e);
        }
    }

    /**
     * 根据任务ID获取任务详情
     * @param taskId 任务ID
     * @return 任务详情列表
     */
    public List<TaskDetail> getTaskDetails(int taskId) {
        try {
            List<TaskDetail> details = DatabaseManager.getTaskDetailsByTaskId(taskId);
            logger.info("加载任务详情成功，任务ID: {}, 详情数量: {}", taskId, details.size());
            return details;
        } catch (SQLException e) {
            logger.error("加载任务详情失败，任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
            throw new RuntimeException("加载任务详情失败", e);
        }
    }

    /**
     * 开启所有定时任务
     */
    public void startAllTasks() {
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

            logger.info("所有定时任务已成功开启");
        } catch (SQLException e) {
            logger.error("开启定时任务失败: {}", e.getMessage(), e);
            throw new RuntimeException("开启定时任务失败", e);
        }
    }

    /**
     * 关闭所有定时任务
     */
    public void stopAllTasks() {
        try {
            // 1. 更新所有任务的enabled状态为false
            List<Task> tasks = DatabaseManager.getAllTasks();
            for (Task task : tasks) {
                task.setEnabled(false);
                DatabaseManager.saveTask(task);
            }

            // 2. 停止TaskScheduler
            TaskScheduler.stop();

            logger.info("所有定时任务已成功关闭");
        } catch (SQLException e) {
            logger.error("关闭定时任务失败: {}", e.getMessage(), e);
            throw new RuntimeException("关闭定时任务失败", e);
        }
    }

    /**
     * 保存任务
     * @param task 任务对象
     */
    public void saveTask(Task task) {
        try {
            validateTask(task);
            DatabaseManager.saveTask(task);
            logger.info("任务保存成功，任务ID: {}, 任务名称: {}", task.getId(), task.getTaskName());
        } catch (SQLException e) {
            logger.error("任务保存失败: {}", e.getMessage(), e);
            throw new RuntimeException("任务保存失败", e);
        }
    }

    /**
     * 保存任务详情
     * @param detail 任务详情对象
     */
    public void saveTaskDetail(TaskDetail detail) {
        try {
            validateTaskDetail(detail);
            DatabaseManager.saveTaskDetail(detail);
            logger.info("任务详情保存成功，任务ID: {}, 字段名称: {}", detail.getTaskId(), detail.getFieldName());
        } catch (SQLException e) {
            logger.error("任务详情保存失败: {}", e.getMessage(), e);
            throw new RuntimeException("任务详情保存失败", e);
        }
    }

    /**
     * 删除任务
     * @param taskId 任务ID
     */
    public void deleteTask(int taskId) {
        try {
            DatabaseManager.deleteTask(taskId);
            logger.info("任务删除成功，任务ID: {}", taskId);
        } catch (SQLException e) {
            logger.error("任务删除失败，任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
            throw new RuntimeException("任务删除失败", e);
        }
    }

    /**
     * 删除任务详情
     * @param detailId 任务详情ID
     */
    public void deleteTaskDetail(int detailId) {
        try {
            DatabaseManager.deleteTaskDetail(detailId);
            logger.info("任务详情删除成功，详情ID: {}", detailId);
        } catch (SQLException e) {
            logger.error("任务详情删除失败，详情ID: {}, 错误: {}", detailId, e.getMessage(), e);
            throw new RuntimeException("任务详情删除失败", e);
        }
    }

    /**
     * 根据ID获取任务
     * @param taskId 任务ID
     * @return 任务对象
     */
    public Task getTaskById(int taskId) {
        try {
            Task task = DatabaseManager.getTaskById(taskId);
            if (task != null) {
                logger.info("获取任务成功，任务ID: {}", taskId);
            } else {
                logger.warn("未找到任务，任务ID: {}", taskId);
            }
            return task;
        } catch (SQLException e) {
            logger.error("获取任务信息失败，任务ID: {}, 错误: {}", taskId, e.getMessage(), e);
            throw new RuntimeException("获取任务信息失败", e);
        }
    }

    /**
     * 异步执行任务
     * @param runnable 要执行的任务
     */
    public void executeAsync(Runnable runnable) {
        executorService.submit(runnable);
    }

    /**
     * 验证任务对象
     * @param task 任务对象
     * @throws IllegalArgumentException 当必填字段为空时抛出
     */
    private void validateTask(Task task) {
        if (task.getDeviceId() == null || task.getDeviceId().trim().isEmpty()) {
            throw new IllegalArgumentException("设备号为必填项");
        }
        if (task.getCronExpression() == null || task.getCronExpression().trim().isEmpty()) {
            throw new IllegalArgumentException("定时任务cron为必填项");
        }
        if (task.getTaskType() == null || task.getTaskType().trim().isEmpty()) {
            throw new IllegalArgumentException("任务类型为必填项");
        }
        if (task.getTaskName() == null || task.getTaskName().trim().isEmpty()) {
            throw new IllegalArgumentException("任务名称为必填项");
        }
    }

    /**
     * 验证任务详情对象
     * @param detail 任务详情对象
     * @throws IllegalArgumentException 当必填字段为空时抛出
     */
    private void validateTaskDetail(TaskDetail detail) {
        if (detail.getFieldName() == null || detail.getFieldName().trim().isEmpty()) {
            throw new IllegalArgumentException("字段名称为必填项");
        }
        if (detail.getFieldValue() == null || detail.getFieldValue().trim().isEmpty()) {
            throw new IllegalArgumentException("字段值为必填项");
        }
        if (detail.getDataType() == null || detail.getDataType().trim().isEmpty()) {
            throw new IllegalArgumentException("数据类型为必填项");
        }
    }

    /**
     * 关闭服务
     */
    public void shutdown() {
        executorService.shutdown();
        logger.info("TaskListService 已关闭");
    }
}