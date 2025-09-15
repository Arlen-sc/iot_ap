package com.iot.plc.logger;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * 日志工厂类
 * 用于统一管理日志
 */
public class LoggerFactory {
    private static final String LOG_FOLDER = "logs";
    private static final int LOG_SIZE_LIMIT = 5 * 1024 * 1024; // 5MB
    private static final int LOG_FILE_COUNT = 10;
    
    static {
        // 创建日志目录
        File logDir = new File(LOG_FOLDER);
        if (!logDir.exists()) {
            logDir.mkdirs();
        }
    }
    
    /**
     * 获取日志记录器
     * @param name 日志记录器名称
     * @return 日志记录器
     */
    public static Logger getLogger(String name) {
        Logger logger = Logger.getLogger(name);
        
        try {
            // 设置日志级别
            logger.setLevel(Level.INFO);
            
            // 获取当前日期
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            
            // 创建文件处理器
            FileHandler fileHandler = new FileHandler(
                    LOG_FOLDER + File.separator + date + "_" + name.replace(".", "_") + ".log",
                    LOG_SIZE_LIMIT,
                    LOG_FILE_COUNT,
                    true
            );
            
            // 设置格式化器
            fileHandler.setFormatter(new SimpleFormatter());
            
            // 添加处理器
            logger.addHandler(fileHandler);
            
            // 不使用父处理器
            logger.setUseParentHandlers(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return logger;
    }
}