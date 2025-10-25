package com.iot.plc.ui.AutoProcessPanelService;

import com.iot.plc.logger.Logger;
import com.iot.plc.service.ConfigService;
import com.iot.plc.service.NetworkService;
import com.iot.plc.enumx.TcpServiceEnum;
import com.iot.plc.util.HexUtils;
import java.util.List;

public class PlcService {
    private static final PlcService instance = new PlcService();
    
    private PlcService() {
    }
    
    public static PlcService getInstance() {
        return instance;
    }
    
    /**
     * 记录日志
     */
    private void log(String message) {
        Logger.getInstance().info(message);
    }
    
    /**
     * 处理条码数据
     * @param hexString 条码数据:从plc获取数据，数据格式：hex。
     */
    public List<String> doBarcodeProcess(String hexString) {
        log("[doBarcodeProcess] 条码数据: " + hexString);
        try {
            ConfigService configService = ConfigService.getInstance();
            // 条码指令前缀，用于判断是否是条码指令
            String plcBarcodePrefix = configService.getConfigValueByKey("plc.tcp.barcode.prefix");
             //获取条码分割符
            String barcodeDelimiter = ConfigService.getInstance().getConfigValueByKey("plc.tcp.barcode.delimiter");
            //获取条码字节长度
            int barcodeLength = Integer.parseInt(configService.getConfigValueByKey("plc.tcp.barcode.length"));
            // 提取条码数据,把value中的plcBarcodePrefix替换为空字符串
            List<String> barcodes = HexUtils.hexToStringByReverseMultiple(hexString, plcBarcodePrefix, barcodeDelimiter, barcodeLength);
            log("[doBarcodeProcess] 获取到条码数量: " + barcodes.size());
            // 保存条码数据到列表
            return barcodes;
        } catch (Exception e) {
            log("[doBarcodeProcess] 保存条码数据到数据库失败: " + e.getMessage());
        }
        return null;
    }
    public void sendPlcBeginCommand(){
        ConfigService configService = ConfigService.getInstance();
        //获取开始指令
        String beginCommand = configService.getConfigValueByKey("plc.tcp.begin.command");
        //发送开始指令
        NetworkService networkService = NetworkService.getInstance();
        networkService.sendData(beginCommand, TcpServiceEnum.PLC);
        // log("[PLC] 已发送开始指令: " + beginCommand);
    }

    /**
     * 处理PLC开始指令
     * 1.判断信息是否是：plc.tcp.begin.command配置的接收指令
     * 2.如果是，发送next指令
     * @param value
     */
    public void doPlcBeginCommand(String value) {
        ConfigService configService = ConfigService.getInstance();
       //判断信息是否是：plc.tcp.begin.command配置的接收指令
        String plcBeginCommand = configService.getConfigValueByKey("plc.tcp.begin.command");
        if (value.contains(plcBeginCommand)) {
            log("[PLC] 收到开始指令: " + value);
            // 发送next指令
            String nextCommand = configService.getConfigValueByKey("plc.tcp.next.command");
            //发送next指令
            NetworkService networkService = NetworkService.getInstance();
            networkService.sendData(nextCommand, TcpServiceEnum.PLC);
            log("[PLC] 已发送next指令: " + nextCommand);
        }
    }


    /**
     * 处理PLC数量响应
     * 1.判断信息是否是：plc.qty.query.response配置的接收指令
     * 2.如果是，提取条码数量，更新到配置服务，应用条码数量，发送next指令
     * @param value
     */
    public Integer doPlcQtyResponse(String value){
        ConfigService configService = ConfigService.getInstance();
        String receiveCommand = configService.getConfigValueByKey("plc.qty.query.response");
        if (value.contains(receiveCommand)) {
            log("[PLC] 收到数量响应: " + value+";前缀："+receiveCommand);
            // 提取条码数量,把value中的receiveCommand替换为空字符串
            String qty = value.replaceAll(receiveCommand, "").trim();
            log("[PLC] PLC条码数量: " + qty);
            //把条码数量更新到配置服务
            int intQty = Integer.parseInt(qty);
            return intQty;
            // log("[PLC] 已将预期条码数量更新为当前条码数量: " + intQty);
            // //获取配置服务中的next指令
            // String nextCommand = configService.getConfigValueByKey("plc.tcp.next.command");
            // //发送next指令
            // NetworkService networkService = NetworkService.getInstance();
            // networkService.sendData(nextCommand,TcpServiceEnum.PLC);
            // log("[PLC] 已发送next指令: " + nextCommand);
            // return;
        }else{
            return null;
        }
    }
}
