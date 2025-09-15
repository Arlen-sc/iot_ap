package com.iot.plc.service;

import com.iot.plc.model.ProgramResult;
import com.iot.plc.logger.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * EMS服务类
 * 用于处理与EMS系统的通信，将烧录结果发送到EMS系统
 */
public class EmsService {
    private static final Logger logger = LoggerFactory.getLogger(EmsService.class.getName());
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    
    // 单例模式
    private static EmsService instance;
    
    // EMS系统API地址
    private String emsApiUrl;
    
    // 线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);
    
    private EmsService() {
        // 私有构造函数
    }
    
    public static synchronized EmsService getInstance() {
        if (instance == null) {
            instance = new EmsService();
        }
        return instance;
    }
    
    /**
     * 初始化EMS服务
     * @param apiUrl EMS系统API地址
     */
    public void init(String apiUrl) {
        this.emsApiUrl = apiUrl;
        logger.info("EMS service initialized with API URL: " + apiUrl);
    }
    
    /**
     * 发送烧录结果到EMS系统
     * @param result 烧录结果
     */
    /**
     * 解析数据
     * @param rawData 原始数据
     * @return 解析后的JSON字符串
     */
    public String parseData(String rawData) {
        return "{\"status\":\"success\",\"parsed\":\"" + rawData + "\"}";
    }

    public void sendProgramResult(ProgramResult result) {
        if (emsApiUrl == null || emsApiUrl.isEmpty()) {
            logger.warning("EMS API URL not configured");
            return;
        }
        
        // 使用线程池异步发送
        executorService.submit(() -> {
            try {
                // 转换为JSON
                String json = gson.toJson(result);
                
                // 发送HTTP请求
                URL url = new URL(emsApiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                
                // 设置超时
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                // 发送请求体
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                
                // 获取响应
                int responseCode = conn.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // 读取响应
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        logger.info("EMS response: " + response.toString());
                    }
                    
                    logger.info("Program result sent to EMS successfully");
                } else {
                    logger.warning("Failed to send program result to EMS. Response code: " + responseCode);
                    
                    // 读取错误响应
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        logger.warning("EMS error response: " + response.toString());
                    }
                    
                    // 重试逻辑
                    scheduleRetry(result);
                }
            } catch (Exception e) {
                logger.severe("Error sending program result to EMS: " + e.getMessage());
                
                // 重试逻辑
                scheduleRetry(result);
            }
        });
    }
    
    /**
     * 计划重试发送
     * @param result 烧录结果
     */
    private void scheduleRetry(ProgramResult result) {
        executorService.submit(() -> {
            try {
                logger.info("Scheduling retry in 5 seconds...");
                Thread.sleep(5000);
                sendProgramResult(result);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * 关闭服务
     */
    public void shutdown() {
        executorService.shutdown();
    }
}