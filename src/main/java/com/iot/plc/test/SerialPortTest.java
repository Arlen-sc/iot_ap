package com.iot.plc.test;

import com.iot.plc.service.SerialPortService;
import jssc.SerialPortException;
import java.util.Scanner;

/**
 * COM端口测试类
 * 用于验证SerialPortService中的端口有效性检查功能
 */
public class SerialPortTest {
    
    public static void main(String[] args) {
        System.out.println("===== COM端口有效性测试工具 ======");
        
        // 获取SerialPortService实例
        SerialPortService portService = SerialPortService.getInstance();
        
        // 列出所有可用的COM端口
        String[] availablePorts = portService.getAvailablePorts();
        
        if (availablePorts.length == 0) {
            System.out.println("未发现可用的COM端口");
            return;
        }
        
        System.out.println("可用COM端口列表：");
        for (int i = 0; i < availablePorts.length; i++) {
            System.out.println((i + 1) + ". " + availablePorts[i]);
        }
        
        Scanner scanner = new Scanner(System.in);
        System.out.print("请选择要测试的COM端口编号（输入数字）: ");
        int choice = scanner.nextInt();
        
        if (choice < 1 || choice > availablePorts.length) {
            System.out.println("无效的选择");
            return;
        }
        
        String selectedPort = availablePorts[choice - 1];
        System.out.println("正在测试COM端口: " + selectedPort);
        
        // 使用默认参数测试端口
        int baudRate = 9600;
        int dataBits = 8;
        int stopBits = 1;
        int parity = 0; // 无校验
        
        // 测试端口有效性
        boolean portValid = portService.initSerialPort("test_device", selectedPort, baudRate, dataBits, stopBits, parity);
        
        if (portValid) {
            System.out.println("测试结果: COM端口" + selectedPort + " 有效！");
        } else {
            System.out.println("测试结果: COM端口" + selectedPort + " 无效或无法正常通信！");
        }
        
        // 关闭测试端口
        portService.closeSerialPort("test_device");
        System.out.println("测试完成，已关闭端口连接");
        scanner.close();
    }
}