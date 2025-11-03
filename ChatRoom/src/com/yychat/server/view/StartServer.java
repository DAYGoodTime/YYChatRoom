package com.yychat.server.view;

import com.yychat.server.service.AvatarFileManager;
import com.yychat.server.tcp.YYChatTCPServer;
import com.yychat.server.udp.YYChatUDPServer;
import com.yychat.server.util.DBUtil;

/**
 * 服务器的启动类
 */
public class StartServer {

    private static YYChatUDPServer udpServer;
    private static YYChatTCPServer tcpServer;

    public static YYChatUDPServer getUDPServer() {return udpServer;}
    public static YYChatTCPServer getTCPServer() {return tcpServer;}

    public static void main(String[] args) {
        System.out.println("=== YYChatRoom 服务器启动 ===");

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
}