package com.iot.plc.model;

import javafx.beans.property.SimpleStringProperty;

/**
 * 处理日志表格模型类
 */
public class ProcessLogTableModel {
    private final SimpleStringProperty time;
    private final SimpleStringProperty type;
    private final SimpleStringProperty content;
    private final SimpleStringProperty status;

    public ProcessLogTableModel(String time, String type, String content, String status) {
        this.time = new SimpleStringProperty(time);
        this.type = new SimpleStringProperty(type);
        this.content = new SimpleStringProperty(content);
        this.status = new SimpleStringProperty(status);
    }

    public String getTime() {
        return time.get();
    }

    public void setTime(String time) {
        this.time.set(time);
    }

    public SimpleStringProperty timeProperty() {
        return time;
    }

    public String getType() {
        return type.get();
    }

    public void setType(String type) {
        this.type.set(type);
    }

    public SimpleStringProperty typeProperty() {
        return type;
    }

    public String getContent() {
        return content.get();
    }

    public void setContent(String content) {
        this.content.set(content);
    }

    public SimpleStringProperty contentProperty() {
        return content;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }
}