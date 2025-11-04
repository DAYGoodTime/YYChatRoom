package com.yychat.server.view;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yychat.server.service.AvatarFileManager;
import com.yychat.server.tcp.YYChatTCPServer;
import com.yychat.server.udp.YYChatUDPServer;
import com.yychat.server.util.DBUtil;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 服务器的启动类
 */
public class StartServer {

    private static YYChatUDPServer udpServer;
    private static YYChatTCPServer tcpServer;

    public static YYChatUDPServer getUDPServer() {return udpServer;}
    public static YYChatTCPServer getTCPServer() {return tcpServer;}

    private static ServerConfig serverConfig = new ServerConfig();

    public static ServerConfig getServerConfig() {return serverConfig;}

    public static void main(String[] args) {
        System.out.println("=== YYChatRoom 服务器启动 ===");
        // 0. 检查配置文件
        System.out.println("0. 检查配置文件");
        checkConfig();
        // 1. 检查数据库连接
        System.out.println("1. 检查数据库连接...");
        if(!DBUtil.connectDB()) {
            System.err.println("数据库连接失败，服务器启动终止");
            return;
        }
        // 2. 初始化头像文件管理系统
        System.out.println("2. 初始化头像文件管理系统...");
        AvatarFileManager.initializeAvatarDirectories();

        // 3. 启动UDP服务器
        // 启动TCP服务器用于文件传输
        tcpServer = new YYChatTCPServer();
        new Thread(tcpServer::start).start();
        System.out.println("✓ TCP服务器启动成功 (端口 3457)");

        System.out.println("3. 启动聊天室服务器...");
        udpServer = new YYChatUDPServer();
        udpServer.startServer();
        System.out.println("✓ 服务器启动成功");
        // 保持主线程运行
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("服务端正在关闭...");
            tcpServer.stop();
            udpServer.stopServer();
        }));
    }

    public static void checkConfig(){
        File config =  new File("config-server.json");
        if(!config.exists()){
            System.out.println("文件不存在，正在创建默认配置文件");
            FileUtil.touch(config);
            FileUtil.writeUtf8String(JSONUtil.toJsonStr(serverConfig),config);
        }
        try{
            serverConfig = JSONUtil.readJSONObject(config, StandardCharsets.UTF_8).toBean(ServerConfig.class);
        }catch (Exception e){
            System.out.println("无法读取配置，使用默认配置");
        }
    }
}