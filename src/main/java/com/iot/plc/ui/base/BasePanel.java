package com.iot.plc.ui.base;

import javafx.scene.layout.Pane;

/**
 * 基础面板类，所有UI面板的父类
 */
public abstract class BasePanel extends Pane {
    
    /**
     * 初始化UI组件
     */
    protected abstract void initComponents();
    
    /**
     * 加载数据
     */
    protected abstract void loadData();
    
    /**
     * 刷新面板
     */
    public abstract void refresh();
}