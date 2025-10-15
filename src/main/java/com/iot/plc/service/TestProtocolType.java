package com.iot.plc.service;

import com.iot.plc.enumx.ProtocolType;

/**
 * 测试ProtocolType类的可访问性
 */
public class TestProtocolType {
    public static void main(String[] args) {
        ProtocolType protocolType = ProtocolType.TCP_SERVER;
        System.out.println("ProtocolType: " + protocolType);
    }
}