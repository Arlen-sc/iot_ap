package com.iot.plc.ui;

import com.iot.plc.model.Task;
import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.ConfigItem;
import com.iot.plc.scheduler.LogCleanupJob;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

public class ConfigPanel extends JPanel {
    private static final String LOG_RETENTION_PERIOD_KEY = "log_retention_period";
    
    private JPanel taskInfoPanel; // 表头区域
    private JPanel tableBodyPanel; // 表体区域
    
    private JLabel deviceIdLabel;
    private JLabel cronLabel;
    private JLabel descriptionLabel;
    private JLabel taskTypeLabel;
    private JLabel taskNameLabel;
    
    private JTable taskTable;
    private DefaultTableModel tableModel;
    private JButton addButton;
    private JTextArea resultArea;
    
    public ConfigPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // 创建表头区域
        initTaskInfoPanel();
        
        // 创建表体区域（任务列表）
        initTableBodyPanel();
        
        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addButton = new JButton("新增任务");
        addButton.addActionListener(e -> showTaskConfigDialog(null));
        buttonPanel.add(addButton);
        
        // 结果区域
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane resultScroll = new JScrollPane(resultArea);
        resultScroll.setBorder(BorderFactory.createTitledBorder("执行结果"));
        
        // 创建系统配置区域
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        configPanel.setBorder(BorderFactory.createTitledBorder("系统配置"));
        configPanel.setPreferredSize(new Dimension(0, 80));
        
        JLabel retentionLabel = new JLabel("日志保留周期(单位:周):");
        JTextField retentionField = new JTextField(5);
        JButton saveConfigButton = new JButton("保存配置");
        
        // 加载当前配置
        try {
            List<ConfigItem> configItems = DatabaseManager.getAllConfigItems();
            for (ConfigItem item : configItems) {
                if (LOG_RETENTION_PERIOD_KEY.equals(item.getConfigKey())) {
                    retentionField.setText(item.getConfigValue());
                    break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        // 添加保存配置的事件监听
        saveConfigButton.addActionListener(e -> {
            try {
                String retentionValue = retentionField.getText();
                // 验证输入是否为整数
                if (!retentionValue.matches("\\d+")) {
                    JOptionPane.showMessageDialog(this, "请输入有效的整数", "输入错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                int retentionWeeks = Integer.parseInt(retentionValue);
                if (retentionWeeks < 0) {
                    JOptionPane.showMessageDialog(this, "保留周期不能为负数", "输入错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // 保存配置
                ConfigItem configItem = new ConfigItem();
                configItem.setConfigKey(LOG_RETENTION_PERIOD_KEY);
                configItem.setConfigValue(String.valueOf(retentionWeeks));
                configItem.setDescription("日志保留周期(单位:周)，0表示不保留日志");
                configItem.setDataType("integer");
                configItem.setRequired(false);
                
                // 检查是否已存在该配置
                List<ConfigItem> existingConfigs = DatabaseManager.getAllConfigItems();
                for (ConfigItem item : existingConfigs) {
                    if (LOG_RETENTION_PERIOD_KEY.equals(item.getConfigKey())) {
                        configItem.setId(item.getId());
                        break;
                    }
                }
                
                DatabaseManager.saveConfigItem(configItem);
                
                // 询问用户是否立即执行日志清理
                int confirmResult = JOptionPane.showConfirmDialog(
                        this, 
                        "配置保存成功，是否立即执行日志清理？", 
                        "确认", 
                        JOptionPane.YES_NO_OPTION);
                
                if (confirmResult == JOptionPane.YES_OPTION) {
                    try {
                        // 立即执行日志清理
                        LogCleanupJob cleanupJob = new LogCleanupJob();
                        cleanupJob.execute(null); // 传入null上下文，但实际实现中没有使用
                        JOptionPane.showMessageDialog(this, "日志清理执行完成", "成功", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                this, 
                                "日志清理执行失败: " + e.getMessage(), 
                                "错误", 
                                JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "配置保存成功，日志清理将在下次定时任务执行", "成功", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "配置保存失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        configPanel.add(retentionLabel);
        configPanel.add(retentionField);
        configPanel.add(Box.createHorizontalStrut(10));
        configPanel.add(saveConfigButton);
        
        // 组合所有组件
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(configPanel, BorderLayout.NORTH);
        centerPanel.add(tableBodyPanel, BorderLayout.CENTER);
        
        add(taskInfoPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void initTaskInfoPanel() {
        taskInfoPanel = new JPanel();
        taskInfoPanel.setBorder(BorderFactory.createTitledBorder("表头信息"));
        taskInfoPanel.setLayout(new GridLayout(5, 1));
        taskInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        deviceIdLabel = new JLabel("设备号: ");
        cronLabel = new JLabel("定时任务cron: ");
        descriptionLabel = new JLabel("说明: ");
        taskTypeLabel = new JLabel("任务类型: ");
        taskNameLabel = new JLabel("任务名称: ");
        
        taskInfoPanel.add(deviceIdLabel);
        taskInfoPanel.add(cronLabel);
        taskInfoPanel.add(descriptionLabel);
        taskInfoPanel.add(taskTypeLabel);
        taskInfoPanel.add(taskNameLabel);
    }
    
    private void initTableBodyPanel() {
        tableBodyPanel = new JPanel(new BorderLayout());
        tableBodyPanel.setBorder(BorderFactory.createTitledBorder("表体数据"));
        
        // 定义表格列名
        String[] columnNames = {"设备号", "任务类型", "任务名称", "操作"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3; // 只有操作列可编辑
            }
        };
        
        taskTable = new JTable(tableModel);
        taskTable.setRowHeight(40); // 设置行高，确保按钮完全显示
        
        // 添加表格监听器，处理编辑按钮点击
        taskTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int column = taskTable.columnAtPoint(e.getPoint());
                int row = taskTable.rowAtPoint(e.getPoint());
                
                if (column == 3 && row >= 0) { // 操作列
                    String deviceId = (String) tableModel.getValueAt(row, 0);
                    String taskType = (String) tableModel.getValueAt(row, 1);
                    String taskName = (String) tableModel.getValueAt(row, 2);
                    
                    // 通过设备号、任务类型和任务名称查找任务
                    try {
                        List<Task> tasks = DatabaseManager.getAllTasks();
                        for (Task task : tasks) {
                            if (task.getDeviceId().equals(deviceId) && 
                                task.getTaskType().equals(taskType) && 
                                task.getTaskName().equals(taskName)) {
                                showTaskConfigDialog(task);
                                break;
                            }
                        }
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(ConfigPanel.this, "加载任务信息失败: " + ex.getMessage(), 
                            "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(taskTable);
        tableBodyPanel.add(scrollPane, BorderLayout.CENTER);
    }
    
    public void loadTaskInfo(Task task) {
        if (task != null) {
            // 更新表头信息
            deviceIdLabel.setText("设备号: " + task.getDeviceId());
            cronLabel.setText("定时任务cron: " + task.getCronExpression());
            descriptionLabel.setText("说明: " + task.getDescription());
            taskTypeLabel.setText("任务类型: " + task.getTaskType());
            taskNameLabel.setText("任务名称: " + task.getTaskName());
        }
        
        // 刷新表体数据
        refreshTableData();
    }
    
    private void refreshTableData() {
        tableModel.setRowCount(0); // 清空表格
        
        try {
            List<Task> tasks = DatabaseManager.getAllTasks();
            for (Task task : tasks) {
                Object[] rowData = {
                    task.getDeviceId(),
                    task.getTaskType(),
                    task.getTaskName(),
                    "编辑"
                };
                tableModel.addRow(rowData);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "加载任务列表失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showTaskConfigDialog(Task task) {
        TaskConfigDialog dialog = new TaskConfigDialog(SwingUtilities.getWindowAncestor(this), task);
        dialog.setVisible(true);
        
        // 对话框关闭后刷新表格数据
        if (dialog.isSaved()) {
            refreshTableData();
            // 如果是编辑现有任务，更新表头信息
            if (task != null) {
                try {
                    Task updatedTask = DatabaseManager.getTaskById(task.getId());
                    loadTaskInfo(updatedTask);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // 任务配置对话框类
    private class TaskConfigDialog extends JDialog {
        private Task task;
        private boolean saved = false;
        
        private JTextField deviceIdField;
        private JTextField cronField;
        private JTextField descriptionField;
        private JComboBox<String> taskTypeCombo;
        private JTextField taskNameField;
        private JButton saveButton;
        private JButton cancelButton;
        
        public TaskConfigDialog(Window owner, Task task) {
            super(owner, task == null ? "新增任务" : "编辑任务", Dialog.ModalityType.APPLICATION_MODAL);
            this.task = task;
            
            setSize(400, 300);
            setLocationRelativeTo(owner);
            
            initComponents();
            
            if (task != null) {
                // 填充现有任务数据
                deviceIdField.setText(task.getDeviceId());
                cronField.setText(task.getCronExpression());
                descriptionField.setText(task.getDescription());
                taskTypeCombo.setSelectedItem(task.getTaskType());
                taskNameField.setText(task.getTaskName());
            }
        }
        
        private void initComponents() {
            JPanel panel = new JPanel(new GridLayout(6, 2, 10, 10));
            panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            panel.add(new JLabel("设备号*:"));
            deviceIdField = new JTextField();
            panel.add(deviceIdField);
            
            panel.add(new JLabel("定时任务cron:"));
            cronField = new JTextField("0 0/5 * * * ?"); // 默认5分钟执行一次
            panel.add(cronField);
            
            panel.add(new JLabel("说明:"));
            descriptionField = new JTextField();
            panel.add(descriptionField);
            
            panel.add(new JLabel("任务类型*:"));
            String[] taskTypes = {"PLC读取", "PLC传送", "数据解析", "数据保存"};
            taskTypeCombo = new JComboBox<>(taskTypes);
            panel.add(taskTypeCombo);
            
            panel.add(new JLabel("任务名称*:"));
            taskNameField = new JTextField();
            panel.add(taskNameField);
            
            // 按钮面板
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            saveButton = new JButton("保存");
            cancelButton = new JButton("取消");
            
            saveButton.addActionListener(e -> saveTask());
            cancelButton.addActionListener(e -> dispose());
            
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            
            panel.add(new JLabel());
            panel.add(buttonPanel);
            
            add(panel);
        }
        
        private void saveTask() {
            String deviceId = deviceIdField.getText().trim();
            String cron = cronField.getText().trim();
            String description = descriptionField.getText().trim();
            String taskType = (String) taskTypeCombo.getSelectedItem();
            String taskName = taskNameField.getText().trim();
            
            if (deviceId.isEmpty() || taskType.isEmpty() || taskName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "带*的字段为必填项", 
                    "警告", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                Task newTask = new Task(deviceId, cron, description, taskType, taskName);
                if (task != null) {
                    newTask.setId(task.getId()); // 使用已存在的任务ID进行更新
                }
                
                DatabaseManager.saveTask(newTask);
                saved = true;
                dispose();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "保存失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        public boolean isSaved() {
            return saved;
        }
    }
}