package com.iot.plc.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();
    
    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException e) {
            System.err.println("无法加载配置文件: " + e.getMessage());
        }
    }
    
    public static String getPlcDefaultIp() {
        return properties.getProperty("plc.default.ip", "192.168.1.100");
    }
    
    public static int getPlcDefaultPort() {
        return Integer.parseInt(properties.getProperty("plc.default.port", "502"));
    }
    
    public static int getConnectionTimeout() {
        return Integer.parseInt(properties.getProperty("connection.timeout", "5000"));
    }
    
    public static int getReadTimeout() {
        return Integer.parseInt(properties.getProperty("read.timeout", "3000"));
    }
}