package com.yychat.view;

import com.yychat.control.AvatarFileManager;
import com.yychat.control.DBUtil;
import com.yychat.control.YYchatServerUDP;

/**
 * UDP服务器的启动类（增强版 - 支持头像功能）
 */
public class StartServerUDP {
    public static void main(String[] args) {
        System.out.println("=== YYChatRoom 服务器启动 ===");

        // 1. 检查数据库连接
        System.out.println("1. 检查数据库连接...");
        if(!DBUtil.connectDB()) {
            System.err.println("数据库连接失败，服务器启动终止");
            return;
        }
        System.out.println("✓ 数据库连接成功");

        // 2. 初始化头像文件管理系统
        System.out.println("2. 初始化头像文件管理系统...");
        AvatarFileManager.initializeAvatarDirectories();
        System.out.println("✓ 头像文件管理系统初始化完成");

        // 3. 启动UDP服务器
        System.out.println("3. 启动聊天室服务器...");
        YYchatServerUDP serverUDP = new YYchatServerUDP();
        serverUDP.startServer();
        System.out.println("✓ 服务器启动成功");

        // 保持主线程运行
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}