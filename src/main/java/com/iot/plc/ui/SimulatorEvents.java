package com.iot.plc.ui;

import com.iot.plc.model.BarcodeData;
import javafx.application.Platform;

import java.util.Random;
import java.util.function.Supplier;
import java.util.List;
import com.iot.plc.ui.AutoProcessPanel;

/**
 * 模拟事件管理器 - 集中管理所有模拟点击事件
 */
public class SimulatorEvents {
    private final Random random = new Random();
    
    // 依赖项供应商
    private final Supplier<String> deviceIdSupplier;
    private final Supplier<List<BarcodeData>> barcodeDataListSupplier;
    private final Supplier<List<String>> currentBarcodesSupplier;
    private final Supplier<String> comPortSupplier;
    private final Supplier<String> expectedBarcodeCountSupplier;
    private final Supplier<Boolean> processStartedSupplier;
    private final Supplier<Boolean> barcodeVerifiedSupplier;
    private final Runnable resetProcessRunnable;
    private final Supplier<String> actualBarcodeCountSupplier;
    private final Setter<String> actualBarcodeCountSetter;
    private final Setter<String> currentStatusSetter;
    private final Setter<Boolean> barcodeVerifiedSetter;
    private final Setter<Boolean> waitingForStartCommandSetter;
    
    // 日志记录器
    private final LogWriter logWriter;
    
    /**
     * 函数式接口 - 设置值
     */
    @FunctionalInterface
    public interface Setter<T> {
        void set(T value);
    }
    
    /**
     * 函数式接口 - 记录日志
     */
    @FunctionalInterface
    public interface LogWriter {
        void log(String message);
    }
    
    /**
     * 构造函数
     */
    public SimulatorEvents(
            Supplier<String> deviceIdSupplier,
            Supplier<List<BarcodeData>> barcodeDataListSupplier,
            Supplier<List<String>> currentBarcodesSupplier,
            Supplier<String> comPortSupplier,
            Supplier<String> expectedBarcodeCountSupplier,
            Supplier<Boolean> processStartedSupplier,
            Supplier<Boolean> barcodeVerifiedSupplier,
            Runnable resetProcessRunnable,
            Supplier<String> actualBarcodeCountSupplier,
            Setter<String> actualBarcodeCountSetter,
            Setter<String> currentStatusSetter,
            Setter<Boolean> barcodeVerifiedSetter,
            Setter<Boolean> waitingForStartCommandSetter,
            LogWriter logWriter) {
        
        this.deviceIdSupplier = deviceIdSupplier;
        this.barcodeDataListSupplier = barcodeDataListSupplier;
        this.currentBarcodesSupplier = currentBarcodesSupplier;
        this.comPortSupplier = comPortSupplier;
        this.expectedBarcodeCountSupplier = expectedBarcodeCountSupplier;
        this.processStartedSupplier = processStartedSupplier;
        this.barcodeVerifiedSupplier = barcodeVerifiedSupplier;
        this.resetProcessRunnable = resetProcessRunnable;
        this.actualBarcodeCountSupplier = actualBarcodeCountSupplier;
        this.actualBarcodeCountSetter = actualBarcodeCountSetter;
        this.currentStatusSetter = currentStatusSetter;
        this.barcodeVerifiedSetter = barcodeVerifiedSetter;
        this.waitingForStartCommandSetter = waitingForStartCommandSetter;
        this.logWriter = logWriter;
    }
    
    /**
     * 模拟扫描条码
     */
    public void simulateBarcodeScan() {
        // 确保在JavaFX应用线程中执行UI操作
        Platform.runLater(() -> {
            logWriter.log("[操作] 用户点击了'模拟扫描'按钮");
            if (!processStartedSupplier.get()) {
                logWriter.log("[操作结果] 请先启动流程");
                return;
            }

            // 生成随机条码
            String randomBarcode = "BAR-" + System.currentTimeMillis() + "-" + random.nextInt(1000);

            // 创建条码数据对象
            String portValue = comPortSupplier.get() != null ? comPortSupplier.get() : "未知端口";
            BarcodeData barcodeData = new BarcodeData(deviceIdSupplier.get(), randomBarcode, portValue);

            // 添加到缓存
            barcodeDataListSupplier.get().add(barcodeData);
            currentBarcodesSupplier.get().add(randomBarcode);

            // 更新实际条码数量显示
            actualBarcodeCountSetter.set(String.valueOf(barcodeDataListSupplier.get().size()));

            logWriter.log("[操作结果] 扫描到条码: " + randomBarcode);
            logWriter.log("[数据状态] 当前条码数量: " + barcodeDataListSupplier.get().size() + ", 预期条码数量: " + expectedBarcodeCountSupplier.get());
        });
    }
    
    /**
     * 模拟PLC产品数量处理
     */
    public void simulatePlcProductCount() {
        Platform.runLater(() -> {
            logWriter.log("[操作] 用户点击了'模拟PLC数量'按钮");
            if (!processStartedSupplier.get() || barcodeVerifiedSupplier.get()) {
                logWriter.log("[操作结果] 请先启动流程并扫描条码，或条码已经验证通过");
                return;
            }

            // 使用用户设置的预期数量
            int actualCount = barcodeDataListSupplier.get().size();
            String expectedCountStr = expectedBarcodeCountSupplier.get();

            try {
                int count = Integer.parseInt(expectedCountStr);
                logWriter.log("[PLC数据] 接收到PLC产品数量: " + count);

                // 验证条码数量
                if (actualCount == count) {
                    barcodeVerifiedSetter.set(true);
                    currentStatusSetter.set("验证通过");
                    logWriter.log("[验证结果] 条码数量验证通过: " + actualCount + " = " + count);
                    waitingForStartCommandSetter.set(true);
                    logWriter.log("[流程状态] 等待PLC开始指令...");
                } else {
                    logWriter.log("[验证结果] 错误: 条码数量不匹配! 实际: " + actualCount + " 预期: " + count);
                    currentStatusSetter.set("异常");
                    logWriter.log("[流程状态] 进入异常状态，准备重置流程...");
                    resetProcessRunnable.run(); // 重置但保持流程运行状态
                }
            } catch (NumberFormatException e) {
                logWriter.log("[验证结果] 预期条码数量格式错误，请输入有效的数字");
                currentStatusSetter.set("异常");
                logWriter.log("[流程状态] 进入异常状态，准备重置流程...");
                resetProcessRunnable.run(); // 重置但保持流程运行状态
            }
        });
    }
    
    /**
     * 模拟PLC开始指令处理
     */
    public void simulatePlcStartCommand() {
        Platform.runLater(() -> {
            logWriter.log("[操作] 用户点击了'模拟PLC开始'按钮");
            // 这里可以添加PLC开始指令的模拟逻辑
        });
    }
}