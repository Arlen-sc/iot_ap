package com.iot.plc.util;

import java.io.UnsupportedEncodingException;

/**
 * 十六进制工具类
 * 提供字节数组、字符串与十六进制字符串之间的相互转换功能
 */
public class HexUtils {
    /**
     * 十六进制字符数组
     */
    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
    
    /**
     * 主方法，用于测试各种转换功能
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        try {
            String testStr1 = "1b1b1b1b1b0a1b0a62021741f1b8ffaa40656d732f5f74657374ff";
            String testStr2 = "5b7b2273697465223a223031222c22636f6465223a22344335413030303044453435222c22726573756c74223a307d2c7b2273697465223a223032222c22636f6465223a22344335413030303044453436222c22726573756c74223a317d2c7b2273697465223a223033222c22636f6465223a22344335413030303044453437222c22726573756c74223a317d2c7b2273697465223a223034222c22636f6465223a22344335413030303044453438222c22726573756c74223a317d5d";
            
            // 测试十六进制字符串转普通字符串
            System.out.println("原文:" + testStr1);
            System.out.println("十六进制转字符串:" + hexToString(testStr1));
            
            System.out.println("原文:" + testStr2);
            System.out.println("十六进制转字符串:" + hexToString(testStr2));
            
            // 测试普通字符串转十六进制
            String plainText = "Hello World";
            String hexText = stringToHex(plainText);
            System.out.println("普通字符串:" + plainText);
            System.out.println("字符串转十六进制:" + hexText);
            System.out.println("转回普通字符串:" + hexToString(hexText));
            
        } catch (Exception e) {
            System.err.println("转换过程中出现错误:");
            e.printStackTrace();
        }
    }
    
    // ---------------------------- 检查方法 ----------------------------
    
    /**
     * 检查字符是否为有效的十六进制字符
     * @param c 要检查的字符
     * @return 如果字符是十六进制字符则返回true，否则返回false
     */
    public static boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }
    
    // ---------------------------- 字节数组 <-> 十六进制字符串 ----------------------------
    
    /**
     * 将字节数组转换为十六进制字符串（紧凑格式）
     * @param bytes 要转换的字节数组
     * @return 转换后的十六进制字符串（如："AABBCC"）
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        
        for (byte b : bytes) {
            // 高4位
            sb.append(HEX_CHARS[(b >>> 4) & 0x0F]);
            // 低4位
            sb.append(HEX_CHARS[b & 0x0F]);
        }
        
        return sb.toString();
    }
    
    /**
     * 将字节数组的指定长度转换为十六进制字符串
     * @param data 要转换的字节数组
     * @param length 要转换的字节长度
     * @return 转换后的十六进制字符串
     */
    public static String bytesToHex(byte[] data, int length) {
        return bytesToHex(data, length, false);
    }
    
    /**
     * 将字节数组的指定长度转换为十六进制字符串
     * @param data 要转换的字节数组
     * @param length 要转换的字节长度
     * @param withSpaces 是否添加空格分隔
     * @return 转换后的十六进制字符串（如："AA BB CC"）
     */
    public static String bytesToHex(byte[] data, int length, boolean withSpaces) {
        if (data == null || length <= 0) {
            return "";
        }
        // 确保长度不超过数组长度
        length = Math.min(length, data.length);
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i > 0 && withSpaces) {
                sb.append(" ");
            }
            sb.append(String.format("%02X", data[i]));
        }
        return sb.toString();
    }
    
    /**
     * 将十六进制字符串转换为字节数组
     * @param hexString 十六进制字符串
     * @return 转换后的字节数组
     * @throws IllegalArgumentException 如果输入的十六进制字符串无效
     */
    public static byte[] hexToBytes(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return new byte[0];
        }
        
        // 移除所有空格和分隔符
        hexString = hexString.replaceAll("\\s+", "");
        
        // 检查长度是否为偶数
        if (hexString.length() % 2 != 0) {
            hexString = "0" + hexString; // 在前面补0
        }
        
        int len = hexString.length();
        byte[] bytes = new byte[len / 2];
        
        for (int i = 0; i < len; i += 2) {
            // 检查字符是否为有效十六进制字符
            if (!isHexChar(hexString.charAt(i)) || !isHexChar(hexString.charAt(i + 1))) {
                throw new IllegalArgumentException("无效的十六进制字符: " + hexString.substring(i, i + 2));
            }
            
            bytes[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                 + Character.digit(hexString.charAt(i + 1), 16));
        }
        return bytes;
    }
    
    // ---------------------------- 字符串 <-> 十六进制字符串 ----------------------------
    
    /**
     * 将普通字符串转换为十六进制字符串
     * @param text 普通字符串
     * @return 转换后的十六进制字符串
     */
    public static String stringToHex(String text) {
        if (text == null) {
            return null;
        }
        return bytesToHex(text.getBytes());
    }
    
    /**
     * 将普通字符串转换为十六进制字符串（指定编码）
     * @param text 普通字符串
     * @param charset 字符编码（如"UTF-8"）
     * @return 转换后的十六进制字符串
     * @throws UnsupportedEncodingException 如果指定的编码不支持
     */
    public static String stringToHex(String text, String charset) throws UnsupportedEncodingException {
        if (text == null) {
            return null;
        }
        return bytesToHex(text.getBytes(charset));
    }
    
    /**
     * 将十六进制字符串转换为普通字符串
     * @param hexString 十六进制字符串
     * @return 转换后的普通字符串
     */
    public static String hexToString(String hexString) {
        if (hexString == null) {
            return null;
        }
        return new String(hexToBytes(hexString));
    }
    
    /**
     * 将十六进制字符串转换为普通字符串（指定编码）
     * @param hexString 十六进制字符串
     * @param charset 字符编码（如"UTF-8"）
     * @return 转换后的普通字符串
     * @throws UnsupportedEncodingException 如果指定的编码不支持
     */
    public static String hexToString(String hexString, String charset) throws UnsupportedEncodingException {
        if (hexString == null) {
            return null;
        }
        return new String(hexToBytes(hexString), charset);
    }
    
    // ---------------------------- 字符串 <-> 字节数组 ----------------------------
    
    /**
     * 将字符串转换为字节数组（使用默认编码）
     * @param text 普通字符串
     * @return 转换后的字节数组
     */
    public static byte[] stringToBytes(String text) {
        if (text == null) {
            return null;
        }
        return text.getBytes();
    }
    
    /**
     * 将字符串转换为字节数组（指定编码）
     * @param text 普通字符串
     * @param charset 字符编码（如"UTF-8"）
     * @return 转换后的字节数组
     * @throws UnsupportedEncodingException 如果指定的编码不支持
     */
    public static byte[] stringToBytes(String text, String charset) throws UnsupportedEncodingException {
        if (text == null) {
            return null;
        }
        return text.getBytes(charset);
    }
    
    /**
     * 将字节数组转换为字符串（使用默认编码）
     * @param bytes 字节数组
     * @return 转换后的字符串
     */
    public static String bytesToString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }
    
    /**
     * 将字节数组转换为字符串（指定编码）
     * @param bytes 字节数组
     * @param charset 字符编码（如"UTF-8"）
     * @return 转换后的字符串
     * @throws UnsupportedEncodingException 如果指定的编码不支持
     */
    public static String bytesToString(byte[] bytes, String charset) throws UnsupportedEncodingException {
        if (bytes == null) {
            return null;
        }
        return new String(bytes, charset);
    }
    
    // ---------------------------- 特殊功能方法 ----------------------------
    
    /**
     * 将十六进制字符串转换为JSON字符串
     * @param hexString 十六进制字符串
     * @param prefix 前缀字符串，用于标识JSON字符串的开始
     * @return JSON字符串
     */
    public static String hexToJsonString(String hexString, String prefix) {
        // 先去掉前缀
        if (hexString != null && prefix != null) {
            hexString = hexString.replace(prefix, "");
        }
        
        try {
            return hexToString(hexString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8编码不支持", e);
        }
    }
}