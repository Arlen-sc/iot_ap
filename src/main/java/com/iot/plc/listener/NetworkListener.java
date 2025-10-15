package com.iot.plc.listener;

import com.iot.plc.enumx.*;

// 网络监听器接口
public interface NetworkListener {
// 新版方法，支持服务类型区分
        default void onDataReceived(String data, TcpServiceEnum serviceType) {
            // 默认实现为空，避免调用已移除的旧版方法
        }
        
        default void onDataReceived(byte[] data, TcpServiceEnum serviceType) {
            // 默认实现为空，避免调用已移除的旧版方法
        }
        
        default void onConnectionStatusChanged(boolean connected, TcpServiceEnum serviceType) {
            // 默认实现为空，避免调用已移除的旧版方法
        }
        
        default void onConnectionCountChanged(int count, TcpServiceEnum serviceType) {
            // 默认实现为空，避免调用已移除的旧版方法
        }
        
        // 保留日志相关方法作为抽象方法
        void onLogReceived(String logMessage);
        void onLog(String message); // 兼容扫码机日志方法
        
        // 旧版方法，改为默认实现
        default void onDataReceived(String data) {
            // 默认实现为空，不做任何操作
        }
        
        default void onDataReceived(byte[] data) {
            // 默认实现为空，不做任何操作
        }
        
        default void onConnectionStatusChanged(boolean connected) {
            // 默认实现为空，不做任何操作
        }
        
        default void onConnectionCountChanged(int count) {
            // 默认实现为空，不做任何操作
        }
}
