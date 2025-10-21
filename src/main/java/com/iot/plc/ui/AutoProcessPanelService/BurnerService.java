package com.iot.plc.ui.AutoProcessPanelService;

import com.iot.plc.logger.Logger;
import com.iot.plc.model.BurnResultData;
import com.iot.plc.model.ProgramResult;
import com.iot.plc.service.ConfigService;
import com.iot.plc.service.NetworkService;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.iot.plc.database.DatabaseManager;
import com.iot.plc.enumx.TcpServiceEnum;
import com.iot.plc.util.HexUtils;

public class BurnerService {
    private static final BurnerService instance = new BurnerService();
    
    private BurnerService() {
    }
    
    public static BurnerService getInstance() {
        return instance;
    }
    
    /**
     * 记录日志
     */
    private void log(String message) {
        Logger.getInstance().info(message);
    }
    /**
     * 发送指令给烧录机
     * @param sendToBurnerStr
     */
    public void sendBarcodeJsonToBurner(String sendToBurnerStr) {
        // 发送条码数据给烧录机
        NetworkService.getInstance().sendData(sendToBurnerStr, TcpServiceEnum.BURNER);
    }
    // /**
    //  * 处理烧录机返回的json数据
    //  * @param value
    //  */
    // public List<String> processJsonData(String value){
    //     ConfigService configService = ConfigService.getInstance();
    //     String jsonPrefix = configService.getConfigValueByKey("burner.tcp.json.prefix");
    //     if (!value.toLowerCase().contains(jsonPrefix)){return null;}
    //     // log("[烧录机] 收到数量响应: " + value+";前缀："+jsonPrefix);
    //     // 提取条码数量,把value中的jsonPrefix替换为空字符串
    //     String barcodes=value.toUpperCase().replaceAll(jsonPrefix.toUpperCase(), "").trim();
    //     log("[烧录机] 提取后的字符串: " + barcodes);
    //     try {
    //         barcodes=HexUtils.hexToString(barcodes);
    //         saveProgramResult(barcodes);
    //         //给PLC发送指令：完成：plc.tcp.complete.command
    //         pushOkToPlc();
    //         //给burner发送指令：结束：burner.tcp.complete.command
    //         pushOkToBurner();
    //         return barcodes;
    //     }catch (Exception e) {
    //         log("[烧录机] 处理条码数据时出错: " + e.getMessage());
    //     }
    // }

    /**
     * 给PLC发送指令：完成：plc.tcp.complete.command
     */
    private void pushOkToPlc(){
        String completeCommand = ConfigService.getInstance().getConfigValueByKey("plc.tcp.complete.command");
        log("[PLC]完成指令："+completeCommand);
        NetworkService.getInstance().sendData(completeCommand, TcpServiceEnum.PLC);
    }
    /**
     * 给burner发送指令：完成：burner.tcp.complete.command
     */
    private void pushOkToBurner(){
        String completeCommand = ConfigService.getInstance().getConfigValueByKey("burner.tcp.complete.command");
        log("[烧录机]完成指令："+completeCommand);
        NetworkService.getInstance().sendData(completeCommand, TcpServiceEnum.BURNER);
    }

    /**
     * 保存烧录结果到数据库
     * @param barcodes
     * @throws Exception
     */
    private List<ProgramResult> saveProgramResult(String barcodes) throws Exception {
        // 清理可能的无效字符
        barcodes = barcodes.trim();
        // 解析JSON字符串为条码列表
        JSONArray jsonArray = JSONArray.parseArray(barcodes);
        List<ProgramResult> programResultList = new ArrayList<>();
        // 从配置文件中获取设备ID
        String deviceId = ConfigService.getInstance().getConfigValueByKey("plc.device.id");
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String site = jsonObject.getString("site");
            String code = jsonObject.getString("code");
            int result = jsonObject.getIntValue("result");
            log("[烧录机] 站点: " + site + ", 条码: " + code + ", 结果: " + result);
            //把数据保存到当前页面烧录结果中，并保存到数据库
            boolean success = result == 1; // 假设1表示成功，0表示失败
            String message = success ? "烧录成功" : "烧录失败";
            
            // // 保存到当前页面烧录结果中
            // BurnResultData burnResultData = new BurnResultData(code, success, message);
            // burnResultDataList.add(burnResultData);
            // log("[数据操作] 添加烧录结果到界面显示: 条码=" + code + ", 状态=" + (success ? "成功" : "失败"));
            
            // 创建ProgramResult对象并保存到数据库
            try {
                //
                ProgramResult programResult = new ProgramResult(code, String.valueOf(result), deviceId);
                // 使用统一的时间戳
                programResult.setRem(message);
                DatabaseManager.saveProgramResult(programResult);
                programResultList.add(programResult);
                log("[数据库操作] 成功保存烧录结果到数据库: 条码=" + code);
            } catch (Exception e) {
                log("[数据库错误] 保存烧录结果到数据库失败: " + e.getMessage());
                throw new RuntimeException("保存烧录结果到数据库失败", e);
            }
        }
        return programResultList;
    }
}
