package com.iot.plc.service;

import com.iot.plc.database.DatabaseManager;
import com.iot.plc.model.BarcodeInfo;
import com.iot.plc.model.DeviceResult;
import com.iot.plc.model.ProgramCommand;
import com.iot.plc.model.ProgramResult;
import com.iot.plc.logger.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 上位机通信服务类
 * 用于处理与上位机的通信，包括发送烧录指令和接收烧录结果
 */
public class UpperComputerService {
    private static final Logger logger = LoggerFactory.getLogger(UpperComputerService.class.getName());
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
    
    // 单例模式
    private static UpperComputerService instance;
    
    // 上位机连接信息
    private String host;
    private int port;
    
    // Netty相关
    private Channel channel;
    private EventLoopGroup group;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    
    // 烧录结果队列
    private final BlockingQueue<ProgramResult> resultQueue = new LinkedBlockingQueue<>();
    
    // 烧录结果监听器集合
    private final Set<Consumer<ProgramResult>> programResultListeners = new CopyOnWriteArraySet<>();
    
    // EMS服务
    private EmsService emsService;
    
    private UpperComputerService() {
        // 私有构造函数
    }
    
    public static synchronized UpperComputerService getInstance() {
        if (instance == null) {
            instance = new UpperComputerService();
        }
        return instance;
    }
    
    /**
     * 初始化连接
     * @param host 主机地址
     * @param port 端口
     * @return 连接结果
     */
    public String init(String host, int port) {
        this.host = host;
        this.port = port;
        
        // 初始化EMS服务
        emsService = EmsService.getInstance();
        
        // 启动结果处理线程
        startResultProcessor();
        
        return connect();
    }
    
    /**
     * 连接上位机
     * @return 是否成功
     */
    public String connect() {
        if (connected.get()) {
            logger.info("Already connected to upper computer");
            return "{\"status\":\"success\"}";
        }
        
        try {
            group = new NioEventLoopGroup();
            
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            // 添加心跳检测
                            pipeline.addLast(new IdleStateHandler(0, 0, 60, TimeUnit.SECONDS));
                            
                            // 添加解码器
                            pipeline.addLast(new JsonObjectDecoder());
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));
                            
                            // 添加业务处理器
                            pipeline.addLast(new UpperComputerHandler());
                        }
                    });
            
            // 连接服务器
            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();
            
            // 添加连接关闭监听器
            channel.closeFuture().addListener((ChannelFutureListener) future1 -> {
                connected.set(false);
                logger.info("Connection to upper computer closed");
                
                // 尝试重连
                scheduleReconnect();
            });
            
            connected.set(true);
            logger.info("Connected to upper computer at " + host + ":" + port);
            return "{\"status\":\"success\"}";
        } catch (Exception e) {
            logger.severe("Failed to connect to upper computer: " + e.getMessage());
            
            // 关闭资源
            shutdown();
            
            // 尝试重连
            scheduleReconnect();
            
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 关闭连接
     */
    public void shutdown() {
        if (channel != null) {
            channel.close();
            channel = null;
        }
        
        if (group != null) {
            group.shutdownGracefully();
            group = null;
        }
        
        connected.set(false);
    }
    
    /**
     * 获取连接状态
     * @return 是否已连接
     */
    public boolean isConnected() {
        return connected.get();
    }
    
    /**
     * 计划重连
     */
    private void scheduleReconnect() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                logger.info("Scheduling reconnect in 5 seconds...");
                Thread.sleep(5000);
                connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        executor.shutdown();
    }
    
    /**
     * 发送烧录指令
     * @param deviceId 设备ID
     * @param barcodes 条码列表
     * @return 是否成功
     */
    /**
     * 从PLC读取数据
     * @param deviceId 设备ID
     * @return 读取到的数据
     */
    public String readPlcData(String deviceId) {
        return "{\"status\":\"success\",\"data\":\"sample data from PLC\"}";
    }

    /**
     * 发送烧录指令
     * @param deviceId 设备ID
     * @param barcodes 条码列表
     * @return 操作结果JSON字符串
     */
    public String sendProgramCommand(String deviceId, List<String> barcodes) {
        if (!connected.get()) {
            logger.warning("Not connected to upper computer");
            return "{\"status\":\"error\",\"message\":\"Not connected to upper computer\"}";
        }
        
        try {
            // 创建烧录指令
            List<BarcodeInfo> barcodeInfos = new ArrayList<>();
            for (String barcode : barcodes) {
                BarcodeInfo info = new BarcodeInfo(barcode, deviceId);
                barcodeInfos.add(info);
            }
            ProgramCommand command = new ProgramCommand(barcodeInfos, UUID.randomUUID().toString());
            command.setCommand("PROGRAM");
            
            // 转换为JSON
            String json = gson.toJson(command);
            
            // 发送指令
            ChannelFuture future = channel.writeAndFlush(json);
            future.addListener((ChannelFutureListener) future1 -> {
                if (future1.isSuccess()) {
                    logger.info("Program command sent successfully: " + json);
                } else {
                    logger.severe("Failed to send program command: " + future1.cause().getMessage());
                }
            });
            
            return "{\"status\":\"success\",\"message\":\"Command sent successfully\"}";
        } catch (Exception e) {
            logger.severe("Error sending program command: " + e.getMessage());
            return "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 启动结果处理线程
     */
    private void startResultProcessor() {
        Thread processor = new Thread(() -> {
            while (true) {
                try {
                    // 从队列中获取烧录结果
                    ProgramResult result = resultQueue.take();
                    
                    // 处理烧录结果
                    processProgramResult(result);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.severe("Result processor interrupted: " + e.getMessage());
                    break;
                } catch (Exception e) {
                    logger.severe("Error processing program result: " + e.getMessage());
                }
            }
        });
        
        processor.setDaemon(true);
        processor.start();
    }
    
    /**
     * 处理烧录结果
     * @param result 烧录结果
     */
    private void processProgramResult(ProgramResult result) {
        logger.info("Processing program result: " + gson.toJson(result));
        
        try {
            // 保存烧录结果到数据库
            for (DeviceResult deviceResult : result.getResults()) {
                DatabaseManager.saveProgramResult(
                        result.getBatchId(),
                        deviceResult.getDeviceId(),
                        deviceResult.getBarcode(),
                        deviceResult.isSuccess(),
                        deviceResult.getErrorMessage(),
                        LocalDateTime.now()
                );
            }
            
            // 发送结果到EMS
            emsService.sendProgramResult(result);
            
            // 通知所有监听器
            notifyProgramResultListeners(result);
            
            logger.info("Program result processed successfully");
        } catch (Exception e) {
            logger.severe("Error saving program result: " + e.getMessage());
        }
    }
    
    /**
     * 添加烧录结果监听器
     * @param listener 结果监听器
     */
    public void addProgramResultListener(Consumer<ProgramResult> listener) {
        if (listener != null) {
            programResultListeners.add(listener);
        }
    }
    
    /**
     * 移除烧录结果监听器
     * @param listener 要移除的结果监听器
     */
    public void removeProgramResultListener(Consumer<ProgramResult> listener) {
        if (listener != null) {
            programResultListeners.remove(listener);
        }
    }
    
    /**
     * 通知所有烧录结果监听器
     * @param result 烧录结果
     */
    private void notifyProgramResultListeners(ProgramResult result) {
        for (Consumer<ProgramResult> listener : programResultListeners) {
            try {
                listener.accept(result);
            } catch (Exception e) {
                logger.severe("Error notifying program result listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * 上位机通信处理器
     */
    private class UpperComputerHandler extends SimpleChannelInboundHandler<String> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
            logger.info("Received message from upper computer: " + msg);
            
            try {
                // 解析烧录结果
                ProgramResult result = gson.fromJson(msg, ProgramResult.class);
                
                // 添加到结果队列
                resultQueue.offer(result);
                
                logger.info("Program result added to queue");
            } catch (Exception e) {
                logger.severe("Error parsing program result: " + e.getMessage());
            }
        }
        
        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.severe("Exception in upper computer handler: " + cause.getMessage());
            ctx.close();
        }
        
        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            logger.info("Connection to upper computer lost");
            connected.set(false);
            
            // 尝试重连
            scheduleReconnect();
        }
    }
}