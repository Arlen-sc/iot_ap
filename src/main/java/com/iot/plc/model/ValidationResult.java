package com.iot.plc.model;

/**
 * 验证结果类
 * 用于存储条码数量与产品数量的验证结果
 */
public class ValidationResult {
    private boolean isValid;      // 验证结果
    private String message;        // 结果消息
    private int expectedCount;     // 期望数量
    private int actualCount;      // 实际数量
    
    public ValidationResult(boolean isValid, String message, int expectedCount, int actualCount) {
        this.isValid = isValid;
        this.message = message;
        this.expectedCount = expectedCount;
        this.actualCount = actualCount;
    }
    
    public boolean isValid() {
        return isValid;
    }
    
    public void setValid(boolean valid) {
        isValid = valid;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public int getExpectedCount() {
        return expectedCount;
    }
    
    public void setExpectedCount(int expectedCount) {
        this.expectedCount = expectedCount;
    }
    
    public int getActualCount() {
        return actualCount;
    }
    
    public void setActualCount(int actualCount) {
        this.actualCount = actualCount;
    }
    
    @Override
    public String toString() {
        return "ValidationResult{" +
                "isValid=" + isValid +
                ", message='" + message + '\'' +
                ", expectedCount=" + expectedCount +
                ", actualCount=" + actualCount +
                '}';
    }
}