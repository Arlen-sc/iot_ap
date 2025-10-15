package com.iot.plc.model;

/**
 * 烧录结果数据模型类
 */
public class BurnResultData {
    private String barcode;
    private boolean success;
    private String message;

    public BurnResultData(String barcode, boolean success, String message) {
        this.barcode = barcode;
        this.success = success;
        this.message = message;
    }

    public String getBarcode() {
        return barcode;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}