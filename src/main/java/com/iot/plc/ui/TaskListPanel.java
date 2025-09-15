package com.iot.plc.ui;

import com.iot.plc.model.Task;
import com.iot.plc.model.TaskDetail;
import com.iot.plc.database.DatabaseManager;
import com.iot.plc.scheduler.TaskScheduler;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.List;

public class TaskListPanel extends JPanel {
    private JTable taskTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JButton addButton;
    
    // 任务详情相关组件
    private JPanel detailPanel;
    private JScrollPane detailScrollPane;
    private JSplitPane splitPane;
    
    public TaskListPanel() {
        initComponents();
        loadTasks();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout());
        
        // 创建表头控制按钮面板
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JButton startAllButton = new JButton("开启所有定时任务");
        JButton stopAllButton = new JButton("关闭所有定时任务");
        
        // 设置按钮尺寸
        startAllButton.setPreferredSize(new Dimension(150, 30));
        stopAllButton.setPreferredSize(new Dimension(150, 30));
        
        // 添加按钮点击事件
        startAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startAllTasks();
            }
        });
        
        stopAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopAllTasks();
            }
        });
        
        // 添加按钮到表头面板
        headerPanel.add(startAllButton);
        headerPanel.add(stopAllButton);
        
        // 表头 - 添加操作列
        String[] columnNames = {"ID", "设备号", "定时任务cron", "说明", "任务类型", "任务名称", "定时任务状态", "操作"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                // 只有操作列是可编辑的
                return column == 7;
            }
            
            @Override
            public Class<?> getColumnClass(int column) {
                // 操作列返回Object类型以支持按钮渲染
                return column == 7 ? Object.class : super.getColumnClass(column);
            }
        };
        
        taskTable = new JTable(tableModel);
        // 设置表格行高，确保按钮完全显示
        taskTable.setRowHeight(40);
        
        // 为操作列设置渲染器和编辑器
        taskTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
        taskTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor(new JCheckBox()));
        taskTable.getColumnModel().getColumn(7).setPreferredWidth(150);
        
        // 将表头面板和表格添加到主面板
        add(headerPanel, BorderLayout.NORTH);
        
        // 监听任务表格选择事件，显示对应详情
        taskTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = taskTable.getSelectedRow();
                if (row >= 0) {
                    int taskId = (Integer) tableModel.getValueAt(row, 0);
                    showTaskDetails(taskId);
                }
            }
        });
        
        JScrollPane scrollPane = new JScrollPane(taskTable);
        
        // 创建任务详情面板
        detailPanel = new JPanel(new BorderLayout());
        detailPanel.setBorder(BorderFactory.createTitledBorder("表体数据（任务详情）"));
        detailPanel.add(new JLabel("请选择一个任务查看详情"), BorderLayout.CENTER);
        
        detailScrollPane = new JScrollPane(detailPanel);
        
        // 创建分割面板，上方显示任务列表（表头），下方显示任务详情（表体）
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane, detailScrollPane);
        splitPane.setDividerLocation(300); // 设置初始分割位置
        splitPane.setResizeWeight(0.5);
        
        // 按钮面板 - 只保留新增和刷新按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("刷新");
        addButton = new JButton("新增");
        
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadTasks();
            }
        });
        
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddTaskDialog();
            }
        });
        
        buttonPanel.add(addButton);
        buttonPanel.add(refreshButton);
        
        add(splitPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    // 显示任务详情
    private void showTaskDetails(int taskId) {
        detailPanel.removeAll();
        
        try {
            List<TaskDetail> details = DatabaseManager.getTaskDetailsByTaskId(taskId);
            
            // 创建任务详情表格
            String[] detailColumnNames = {"字段名称", "字段值", "数据类型", "是否必填", "说明", "操作"};
            DefaultTableModel detailTableModel = new DefaultTableModel(detailColumnNames, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    // 只有操作列是可编辑的
                    return column == 5;
                }
            };
            
            JTable detailTable = new JTable(detailTableModel);
            detailTable.setRowHeight(35);
            
            // 设置操作列渲染器和编辑器
            detailTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
            detailTable.getColumnModel().getColumn(5).setCellEditor(new DetailButtonEditor(new JCheckBox(), taskId, detailTableModel));
            detailTable.getColumnModel().getColumn(5).setPreferredWidth(100);
            
            // 添加详情数据到表格
            for (TaskDetail detail : details) {
                Object[] rowData = {
                    detail.getFieldName(),
                    detail.getFieldValue(),
                    detail.getDataType(),
                    detail.isRequired() ? "是" : "否",
                    detail.getDescription(),
                    "操作"
                };
                detailTableModel.addRow(rowData);
            }
            
            // 添加"添加详情"按钮
            JPanel detailButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton addDetailButton = new JButton("添加详情");
            addDetailButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showAddDetailDialog(taskId);
                }
            });
            detailButtonPanel.add(addDetailButton);
            
            detailPanel.setBorder(BorderFactory.createTitledBorder("表体数据（任务详情）"));
            detailPanel.add(new JScrollPane(detailTable), BorderLayout.CENTER);
            detailPanel.add(detailButtonPanel, BorderLayout.SOUTH);
            
        } catch (SQLException e) {
            detailPanel.add(new JLabel("加载任务详情失败: " + e.getMessage()), BorderLayout.CENTER);
        }
        
        detailPanel.revalidate();
        detailPanel.repaint();
    }
    
    // 添加任务详情对话框
    private void showAddDetailDialog(int taskId) {
        JDialog dialog = new JDialog();
        dialog.setTitle("添加任务详情");
        dialog.setSize(500, 400);
        dialog.setModal(true);
        dialog.setLocationRelativeTo(this);
        
        JPanel formPanel = new JPanel(new GridLayout(6, 2));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        formPanel.add(new JLabel("字段名称: *"));
        JTextField fieldNameField = new JTextField();
        formPanel.add(fieldNameField);
        
        formPanel.add(new JLabel("字段值: "));
        JTextField fieldValueField = new JTextField();
        formPanel.add(fieldValueField);
        
        formPanel.add(new JLabel("数据类型: "));
        String[] dataTypes = {"string", "number", "boolean", "date"};
        JComboBox<String> dataTypeCombo = new JComboBox<>(dataTypes);
        dataTypeCombo.setSelectedIndex(0);
        formPanel.add(dataTypeCombo);
        
        formPanel.add(new JLabel("是否必填: "));
        JCheckBox requiredCheckBox = new JCheckBox();
        formPanel.add(requiredCheckBox);
        
        formPanel.add(new JLabel("说明: "));
        JTextField descriptionField = new JTextField();
        formPanel.add(descriptionField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fieldName = fieldNameField.getText().trim();
                if (fieldName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "字段名称不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try {
                    TaskDetail detail = new TaskDetail(
                        taskId,
                        fieldName,
                        fieldValueField.getText().trim(),
                        (String) dataTypeCombo.getSelectedItem(),
                        requiredCheckBox.isSelected(),
                        descriptionField.getText().trim()
                    );
                    
                    DatabaseManager.saveTaskDetail(detail);
                    showTaskDetails(taskId); // 刷新详情列表
                    dialog.dispose();
                    JOptionPane.showMessageDialog(TaskListPanel.this, "任务详情添加成功", "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "添加失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.setLayout(new BorderLayout());
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    // 编辑任务详情对话框
    private void showEditDetailDialog(int taskId, TaskDetail detail) {
        JDialog dialog = new JDialog();
        dialog.setTitle("编辑任务详情");
        dialog.setSize(500, 400);
        dialog.setModal(true);
        dialog.setLocationRelativeTo(this);
        
        JPanel formPanel = new JPanel(new GridLayout(6, 2));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        formPanel.add(new JLabel("字段名称: *"));
        JTextField fieldNameField = new JTextField(detail.getFieldName());
        formPanel.add(fieldNameField);
        
        formPanel.add(new JLabel("字段值: "));
        JTextField fieldValueField = new JTextField(detail.getFieldValue());
        formPanel.add(fieldValueField);
        
        formPanel.add(new JLabel("数据类型: "));
        String[] dataTypes = {"string", "number", "boolean", "date"};
        JComboBox<String> dataTypeCombo = new JComboBox<>(dataTypes);
        dataTypeCombo.setSelectedItem(detail.getDataType());
        formPanel.add(dataTypeCombo);
        
        formPanel.add(new JLabel("是否必填: "));
        JCheckBox requiredCheckBox = new JCheckBox();
        requiredCheckBox.setSelected(detail.isRequired());
        formPanel.add(requiredCheckBox);
        
        formPanel.add(new JLabel("说明: "));
        JTextField descriptionField = new JTextField(detail.getDescription());
        formPanel.add(descriptionField);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fieldName = fieldNameField.getText().trim();
                if (fieldName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "字段名称不能为空", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try {
                    detail.setFieldName(fieldName);
                    detail.setFieldValue(fieldValueField.getText().trim());
                    detail.setDataType((String) dataTypeCombo.getSelectedItem());
                    detail.setRequired(requiredCheckBox.isSelected());
                    detail.setDescription(descriptionField.getText().trim());
                    
                    DatabaseManager.saveTaskDetail(detail);
                    showTaskDetails(taskId); // 刷新详情列表
                    dialog.dispose();
                    JOptionPane.showMessageDialog(TaskListPanel.this, "任务详情更新成功", "成功", JOptionPane.INFORMATION_MESSAGE);
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "更新失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.setLayout(new BorderLayout());
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    // 按钮渲染器类
    class ButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton editButton;
        private JButton deleteButton;
        
        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
            editButton = new JButton("编辑");
            deleteButton = new JButton("删除");
            editButton.setPreferredSize(new Dimension(60, 25));
            deleteButton.setPreferredSize(new Dimension(60, 25));
            add(editButton);
            add(deleteButton);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            return this;
        }
    }
    
    // 按钮编辑器类
    class ButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton editButton;
        private JButton deleteButton;
        private int currentRow;
        private JTable table;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            editButton = new JButton("编辑");
            deleteButton = new JButton("删除");
            editButton.setPreferredSize(new Dimension(60, 25));
            deleteButton.setPreferredSize(new Dimension(60, 25));
            
            editButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                    editTaskAtRow(currentRow);
                }
            });
            
            deleteButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                    deleteTaskAtRow(currentRow);
                }
            });
            
            panel.add(editButton);
            panel.add(deleteButton);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.table = table;
            currentRow = row;
            
            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
            } else {
                panel.setBackground(table.getBackground());
            }
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return null;
        }
    }
    
    // 任务详情按钮编辑器类
    class DetailButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton editButton;
        private JButton deleteButton;
        private int currentRow;
        private int taskId;
        private DefaultTableModel tableModel;
        
        public DetailButtonEditor(JCheckBox checkBox, int taskId, DefaultTableModel tableModel) {
            super(checkBox);
            this.taskId = taskId;
            this.tableModel = tableModel;
            
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            editButton = new JButton("编辑");
            deleteButton = new JButton("删除");
            editButton.setPreferredSize(new Dimension(50, 25));
            deleteButton.setPreferredSize(new Dimension(50, 25));
            
            editButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                    editDetailAtRow(currentRow);
                }
            });
            
            deleteButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                    deleteDetailAtRow(currentRow);
                }
            });
            
            panel.add(editButton);
            panel.add(deleteButton);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
            } else {
                panel.setBackground(table.getBackground());
            }
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            return null;
        }
        
        private void editDetailAtRow(int row) {
            try {
                String fieldName = (String) tableModel.getValueAt(row, 0);
                List<TaskDetail> details = DatabaseManager.getTaskDetailsByTaskId(taskId);
                
                for (TaskDetail detail : details) {
                    if (detail.getFieldName().equals(fieldName)) {
                        showEditDetailDialog(taskId, detail);
                        break;
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(TaskListPanel.this, "获取详情失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
        
        private void deleteDetailAtRow(int row) {
            try {
                String fieldName = (String) tableModel.getValueAt(row, 0);
                List<TaskDetail> details = DatabaseManager.getTaskDetailsByTaskId(taskId);
                
                int confirm = JOptionPane.showConfirmDialog(TaskListPanel.this, "确定要删除选中的详情项吗？", "确认删除", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    for (TaskDetail detail : details) {
                        if (detail.getFieldName().equals(fieldName)) {
                            DatabaseManager.deleteTaskDetail(detail.getId());
                            showTaskDetails(taskId); // 刷新详情列表
                            JOptionPane.showMessageDialog(TaskListPanel.this, "删除成功", "成功", JOptionPane.INFORMATION_MESSAGE);
                            break;
                        }
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(TaskListPanel.this, "删除失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void editTaskAtRow(int row) {
        int taskId = (Integer) tableModel.getValueAt(row, 0);
        try {
            Task task = DatabaseManager.getTaskById(taskId);
            if (task != null) {
                showAddTaskDialog(task); // 使用新增界面进行编辑
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "获取任务信息失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showAddTaskDialog() {
        showAddTaskDialog(null); // 无参数调用，用于新增任务
    }
    
    private void showAddTaskDialog(Task task) {
        // 创建对话框
        JDialog dialog = new JDialog();
        dialog.setTitle(task == null ? "新增任务" : "编辑任务");
        dialog.setSize(400, 300);
        dialog.setModal(true);
        dialog.setLocationRelativeTo(this);
        
        // 添加表单组件
        JPanel formPanel = new JPanel(new GridLayout(6, 2));
        formPanel.add(new JLabel("设备号: *"));
        JTextField deviceIdField = new JTextField();
        formPanel.add(deviceIdField);
        
        formPanel.add(new JLabel("定时任务cron: *"));
        JTextField cronField = new JTextField();
        formPanel.add(cronField);
        
        formPanel.add(new JLabel("说明:"));
        JTextField descField = new JTextField();
        formPanel.add(descField);
        
        formPanel.add(new JLabel("任务类型: *"));
        JTextField typeField = new JTextField();
        formPanel.add(typeField);
        
        formPanel.add(new JLabel("任务名称: *"));
        JTextField nameField = new JTextField();
        formPanel.add(nameField);
        
        // 如果是编辑模式，填充任务数据
        if (task != null) {
            deviceIdField.setText(task.getDeviceId());
            cronField.setText(task.getCronExpression());
            descField.setText(task.getDescription());
            typeField.setText(task.getTaskType());
            nameField.setText(task.getTaskName());
        }
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveButton = new JButton("保存");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 验证必填字段
                String deviceId = deviceIdField.getText().trim();
                String cron = cronField.getText().trim();
                String taskType = typeField.getText().trim();
                String taskName = nameField.getText().trim();
                
                if (deviceId.isEmpty() || cron.isEmpty() || taskType.isEmpty() || taskName.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "带*的字段为必填项", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try {
                    if (task == null) {
                        // 新增模式
                        Task newTask = new Task(
                            deviceId,
                            cron,
                            descField.getText().trim(),
                            taskType,
                            taskName
                        );
                        DatabaseManager.saveTask(newTask);
                        JOptionPane.showMessageDialog(TaskListPanel.this, 
                            "任务添加成功", "成功", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        // 编辑模式
                        task.setDeviceId(deviceId);
                        task.setCronExpression(cron);
                        task.setDescription(descField.getText().trim());
                        task.setTaskType(taskType);
                        task.setTaskName(taskName);
                        DatabaseManager.saveTask(task);
                        JOptionPane.showMessageDialog(TaskListPanel.this, 
                            "任务更新成功", "成功", JOptionPane.INFORMATION_MESSAGE);
                    }
                    loadTasks();
                    dialog.dispose();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(TaskListPanel.this, 
                        "操作失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        dialog.setLayout(new BorderLayout());
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
    
    private void deleteTaskAtRow(int row) {
        int taskId = (Integer) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, 
            "确定要删除选中的任务吗？", "确认删除", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                DatabaseManager.deleteTask(taskId);
                loadTasks();
                JOptionPane.showMessageDialog(this, "删除成功", 
                    "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "删除失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void loadTasks() {
        tableModel.setRowCount(0);
        try {
            List<Task> tasks = DatabaseManager.getAllTasks();
            for (Task task : tasks) {
                Object[] rowData = {
                    task.getId(),
                    task.getDeviceId(),
                    task.getCronExpression(),
                    task.getDescription(),
                    task.getTaskType(),
                    task.getTaskName(),
                    task.isEnabled() ? "启用" : "禁用",
                    "操作"  // 操作列占位符
                };
                tableModel.addRow(rowData);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "加载任务失败: " + e.getMessage(), 
                "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // 注意：现在编辑和删除功能通过表格中的操作列按钮实现，这些方法保留为兼容旧代码
    
    /**
     * 开启所有定时任务
     */
    private void startAllTasks() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "确定要开启所有定时任务吗？", "确认开启", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // 1. 更新所有任务的enabled状态为true
                List<Task> tasks = DatabaseManager.getAllTasks();
                for (Task task : tasks) {
                    task.setEnabled(true);
                    DatabaseManager.saveTask(task);
                }
                
                // 2. 重启TaskScheduler以重新加载所有任务
                TaskScheduler.getInstance().stop();
                TaskScheduler.getInstance().start();
                
                // 3. 刷新表格显示
                loadTasks();
                
                JOptionPane.showMessageDialog(this, "所有定时任务已成功开启", 
                    "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "开启定时任务失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * 关闭所有定时任务
     */
    private void stopAllTasks() {
        int confirm = JOptionPane.showConfirmDialog(this, 
            "确定要关闭所有定时任务吗？", "确认关闭", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // 1. 更新所有任务的enabled状态为false
                List<Task> tasks = DatabaseManager.getAllTasks();
                for (Task task : tasks) {
                    task.setEnabled(false);
                    DatabaseManager.saveTask(task);
                }
                
                // 2. 停止TaskScheduler
                TaskScheduler.getInstance().stop();
                
                // 3. 刷新表格显示
                loadTasks();
                
                JOptionPane.showMessageDialog(this, "所有定时任务已成功关闭", 
                    "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "关闭定时任务失败: " + e.getMessage(), 
                    "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}