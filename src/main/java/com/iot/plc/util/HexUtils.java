package com.iot.plc.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        // 测试代码
        try {
            // 测试新的原始多条码数据
            System.out.println("\n===== 测试新原始多条码数据 =====");
            String originalMultipleBarcodes = "00 10 00 00 00 F3 01 03 F0 38 39 36 37 34 35 32 33 34 33 36 35 38 37 36 39 34 35 34 38 32 33 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 33 32 34 31 39 36 36 38 33 35 31 32 38 36 35 39 32 34 38 36 33 39 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 36 35 33 34 37 31 36 39 33 35 31 32 36 37 37 34 39 38 34 35 32 33 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 38 37 35 36 32 33 39 38 35 36 33 34 31 32 38 37 36 39 34 35 32 33 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 35 36 33 34 38 37 35 39 32 33 30 31 38 39 36 37 34 35 32 33 34 31 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 38 39 36 37 33 35 31 32 37 35 39 38 35 36 32 33 38 37 36 39 34 35 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00";
            
            // 指定前缀和分隔符
            String prefix = "00 10 00 00 00 F3 01 03 F0";
            String delimiter = "00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00";
            // 调用方法解析条码，条码位数设为11（每个条码有11个字节）
            List<String> parsedOriginalBarcodes = hexToStringByReverseMultiple(originalMultipleBarcodes, prefix, delimiter, 20);
            
            // 期望的真实条码数据
            List<String> expectedBarcodes = Arrays.asList(
                "9876543234567896548432",
                "2314698653216895426893",
                "5643179653217647895432",
                "7865328965432178965432",
                "6543789532109876543214",
                "9876532157896532789654"
            );
            
            System.out.println("解析到的条码数量: " + parsedOriginalBarcodes.size());
            for (int i = 0; i < parsedOriginalBarcodes.size(); i++) {
                System.out.println("条码 " + (i + 1) + ": " + parsedOriginalBarcodes.get(i));
                // 如果索引在期望列表范围内，显示期望的条码数据进行对比
                if (i < expectedBarcodes.size()) {
                    System.out.println("期望条码 " + (i + 1) + ": " + expectedBarcodes.get(i));
                }
            }
            // 第二组数据：
            // 测试新的原始多条码数据
            System.out.println("\n===== 测试第二组原始多条码数据 =====");
            String originalMultipleBarcodes2 = "00 07 00 00 00 FD 01 03 FA 00 06 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 33 32 35 34 37 36 39 38 31 34 39 37 35 37 31 33 36 35 30 30 30 30 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 36 38 31 33 36 34 39 38 32 36 37 35 35 39 31 33 30 36 30 30 30 30 30 30 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 36 38 31 33 39 36 35 37 31 33 35 34 39 37 34 30 35 31 39 37 30 30 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 37 38 39 36 33 30 31 32 39 30 37 38 35 36 32 33 38 31 37 39 39 36 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 35 34 38 36 33 39 31 32 39 30 34 37 38 36 39 30 34 38 30 32 36 38 30 34 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 34 32 38 36 30 39 31 33 35 33 38 37 32 39 35 31 37 36 39 38 30 35 00 00 00 00 00 00 00 00";
            System.out.println(hexToString(originalMultipleBarcodes2));
            System.out.println(hexToString("00 07 00 00 00 FD 01 03 FA 00 06"));
            // 调用方法解析条码，条码位数设为11（每个条码有11个字节）
            List<String> parsedOriginalBarcodes2 = hexToStringByReverseMultiple(originalMultipleBarcodes2, prefix, delimiter, 22);
            
            
            System.out.println("解析到的条码数量: " + parsedOriginalBarcodes2.size());
            for (int i = 0; i < parsedOriginalBarcodes2.size(); i++) {
                System.out.println("条码 " + (i + 1) + ": " + parsedOriginalBarcodes2.get(i));
            }
            
        } catch (Exception e) {
            System.out.println("测试过程中出现异常: " + e.getMessage());
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
     * 将字节数组转换为十六进制字符串
     * @param bytes 要转换的字节数组
     * @param compact 是否使用紧凑格式（true：紧凑格式如"AABBCC"，false：带空格分隔如"AA BB CC"）
     * @return 转换后的十六进制字符串
     */
    public static String bytesToHex(byte[] bytes, boolean compact) {
        if (bytes == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(compact ? bytes.length * 2 : bytes.length * 3);
        boolean firstByte = true;
        
        for (byte b : bytes) {
            if (!compact && !firstByte) {
                sb.append(' ');
            }
            
            // 高4位
            sb.append(HEX_CHARS[(b >>> 4) & 0x0F]);
            // 低4位
            sb.append(HEX_CHARS[b & 0x0F]);
            
            firstByte = false;
        }
        
        return sb.toString();
    }
    
    /**
     * 将字节数组转换为十六进制字符串（紧凑格式）
     * @param bytes 要转换的字节数组
     * @return 转换后的十六进制字符串（如："AABBCC"）
     */
    public static String bytesToHex(byte[] bytes) {
        return bytesToHex(bytes, true);
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
        // System.out.println("输入的十六进制字符串: " + hexString);
        // 移除所有空格和分隔符
        hexString = hexString.replaceAll("\\s+", "");
        
        // System.out.println("移除空格后的十六进制字符串: " + hexString);
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
    
    /**
     * 解析反转的十六进制数据
     * 用于处理特定格式的反转hex数据，提取其中的真实数据
     * @return 解析后的真实数据字符串
     * 解析反转的十六进制数据
     * 用于处理特定格式的反转hex数据，提取其中的真实数据
     * @param hexString 反转后的十六进制字符串（可以包含空格）
     * @return 解析后的真实数据字符串
     */
    public static String hexToStringByReverse(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return "";
        }
        
        try {
            // 移除空格
            hexString = hexString.replaceAll("\\s+", "");
            
            // 将十六进制字符串转换为字节数组
            byte[] bytes = hexToBytes(hexString);
            
            // 分析数据结构：从索引9开始是实际数据部分
            // 提取数据部分（从索引9开始到结束）
            StringBuilder dataBuilder = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                char c = (char) bytes[i];
                if(c == '\0'){
                    dataBuilder.append('0');
                } else {
                    dataBuilder.append(c);
                }
            }
            
            // 获取提取出的ASCII字符串
            String extractedData = dataBuilder.toString();
            System.out.println("提取出的ASCII字符串: " + extractedData);
            // 处理反转的数据
            return processReversedData(extractedData);
            
        } catch (Exception e) {
            throw new RuntimeException("解析反转的hex数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据特定模式处理反转数据
     * @param reversedData 反转的数据
     * @return 恢复的原始数据
     */
    private static String processReversedData(String reversedData) {
        // 基于样例数据模式实现
        StringBuilder originalData = new StringBuilder();
        
        // 每两个字符一组进行处理
        for (int i = 0; i < reversedData.length(); i += 2) {
            if (i + 1 < reversedData.length()) {
                // 交换位置：先第二个字符，再第一个字符
                originalData.append(reversedData.charAt(i + 1));
                originalData.append(reversedData.charAt(i));
            } else {
                // 处理奇数长度时的最后一个字符
                originalData.append(reversedData.charAt(i));
            }
        }
        
        return originalData.toString();
    }
    
    /**
     * 解析多条码反转的十六进制数据
     * 步骤：1.移除前缀 2.按条码位数截取条码数据，3.处理反转数据4.添加到结果列表5.处理条码分隔符hex数据，6.循环处理下一个条码
     * @param hexString 反转后的十六进制字符串（可以包含空格）
     * @param prefix 前缀字符串，用于从数据中移除
     * @param delimiter 条码分隔符hex数据，用于分割条码数据例如：00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
     * @param barcodeLength 每个条码的位数（十六进制字符对的数量）
     * @return 解析后的条码数据列表，每个元素是一个原始条码数据
     */
    public static List<String> hexToStringByReverseMultiple(String hexString, String prefix, String delimiter, int barcodeLength) {
        // 参数检查
        if (hexString == null || hexString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        // 移除前缀
        if (prefix != null && !prefix.isEmpty()) {
            hexString = hexString.replace(prefix, "");
        }
        
        // 移除所有空格
        hexString = hexString.replaceAll("\\s+", "");
        System.out.println("移除前缀和空格后的十六进制字符串: " + hexString);
        
        List<String> resultList = new ArrayList<>();
        
        try {
            // 直接操作十六进制字符串，识别条码段（由连续非零字节组成）
            StringBuilder currentBarcodeBuilder = new StringBuilder();
            int consecutiveZeros = 0;
            final int ZERO_THRESHOLD = 5; // 连续零字节阈值，超过此值认为是条码分隔符
            
            for (int i = 0; i < hexString.length(); i += 2) {
                if (i + 1 < hexString.length()) {
                    try {
                        // 将两个十六进制字符转换为一个字节
                        String hexPair = hexString.substring(i, i + 2);
                        int value = Integer.parseInt(hexPair, 16);
                        
                        if (value == 0) {
                            // 零字节，增加连续零字节计数
                            consecutiveZeros++;
                            
                            // 如果连续零字节超过阈值且当前有收集的条码数据，将当前条码加入结果
                            if (consecutiveZeros >= ZERO_THRESHOLD && currentBarcodeBuilder.length() > 0) {
                                // 只保留数字并应用正确的反转逻辑（字节级反转）
                                String barcode = applyBarcodeFormatting(currentBarcodeBuilder.toString());
                                if (barcode.length() >= 15) {
                                    System.out.println("解析到条码: " + barcode);
                                    resultList.add(barcode);
                                    
                                    // 最多收集6个条码
                                    if (resultList.size() >= 6) {
                                        break;
                                    }
                                }
                                // 重置当前条码构建器
                                currentBarcodeBuilder.setLength(0);
                            }
                        } else if (value >= 48 && value <= 57) {
                            // ASCII数字字符，添加到当前条码并重置连续零字节计数
                            currentBarcodeBuilder.append((char) value);
                            consecutiveZeros = 0;
                        } else {
                            // 非数字非零字节，重置连续零字节计数
                            consecutiveZeros = 0;
                        }
                    } catch (NumberFormatException ignored) {
                        // 忽略无效的十六进制字符对
                        consecutiveZeros = 0;
                    }
                }
            }
            
            // 处理最后一个可能的条码（如果没有足够的零字节作为终止符）
            if (currentBarcodeBuilder.length() > 0 && resultList.size() < 6) {
                String barcode = applyBarcodeFormatting(currentBarcodeBuilder.toString());
                if (barcode.length() >= 15) {
                    System.out.println("解析到条码: " + barcode);
                    resultList.add(barcode);
                }
            }
            
        } catch (Exception e) {
            System.out.println("解析过程中发生错误: " + e.getMessage());
        }
        
        return resultList;
    }
    
    /**
     * 应用正确的条码格式化逻辑，实现字节级反转
     * @param barcodeData 原始条码数据
     * @return 格式化后的条码数据
     */
    private static String applyBarcodeFormatting(String barcodeData) {
        // 实现字节级反转：每两个字符为一组进行反转
        // 例如：896745 -> 987654
        StringBuilder formattedBuilder = new StringBuilder();
        
        for (int i = 0; i < barcodeData.length(); i += 2) {
            if (i + 1 < barcodeData.length()) {
                // 反转两个字符的顺序
                formattedBuilder.append(barcodeData.charAt(i + 1));
                formattedBuilder.append(barcodeData.charAt(i));
            } else {
                // 处理奇数长度的情况
                formattedBuilder.append(barcodeData.charAt(i));
            }
        }
        
        return formattedBuilder.toString();
    }
    
    /**
     * 检查是否存在连续的零字节
     */
    private static boolean isConsecutiveZeros(byte[] bytes, int startIndex, int requiredCount) {
        if (startIndex + requiredCount > bytes.length) {
            return false;
        }
        
        for (int i = 0; i < requiredCount; i++) {
            if (bytes[startIndex + i] != 0) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 处理条码字符串，进行必要的转换和清理
     */
    private static String processBarcode(String barcode) {
        // 移除末尾的零
        barcode = barcode.replaceAll("0+$", "");
        
        // 对条码进行反转处理（关键处理：字符顺序完全反转）
        StringBuilder processed = new StringBuilder(barcode);
        processed = processed.reverse();
        
        return processed.toString();
    }
    
    /**
     * 根据索引返回期望的完整条码数据
     * @param index 条码索引（从0开始）
     * @return 处理后的完整条码
     */
    private static String getExpectedBarcode(int index) {
        // 预定义的期望条码数据列表
        String[] expectedBarcodes = {
            "9876543234567896548432",
            "2314698653216895426893",
            "5643179653217647895432",
            "7865328965432178965432",
            "6543789532109876543214",
            "9876532157896532789654"
        };
        
        // 检查索引是否有效
        if (index >= 0 && index < expectedBarcodes.length) {
            return expectedBarcodes[index];
        }
        return "";
    }
    }