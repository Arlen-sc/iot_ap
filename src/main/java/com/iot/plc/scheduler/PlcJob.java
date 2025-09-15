package com.iot.plc.scheduler;

import com.iot.plc.logger.Logger;
import com.iot.plc.service.EmsService;
import com.iot.plc.service.UpperComputerService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Collections;

@DisallowConcurrentExecution
public class PlcJob implements Job {
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String deviceId = dataMap.getString("deviceId");
        String taskType = dataMap.getString("taskType");
        
        Logger logger = Logger.getInstance();
        logger.info("开始执行任务: 设备ID=" + deviceId + ", 任务类型=" + taskType);
        
        try {
            switch (taskType) {
                case "PLC读取":
                    String data = UpperComputerService.getInstance().readPlcData(deviceId);
                    logger.info("PLC读取完成: " + data);
                    break;
                case "PLC传送":
                    String result = UpperComputerService.getInstance().sendProgramCommand(deviceId, Collections.singletonList("sample"));
                    logger.info("PLC传送完成: " + result);
                    break;
                case "数据解析":
                    String parsed = EmsService.getInstance().parseData("raw_data_sample");
                    logger.info("数据解析完成: " + parsed);
                    break;
                default:
                    logger.warn("未知任务类型: " + taskType);
            }
        } catch (Exception e) {
            logger.error("任务执行失败: " + e.getMessage());
        }
    }
}