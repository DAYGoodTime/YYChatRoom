package com.yychat.view;

import com.yychat.control.DBUtil;
import com.yychat.control.YYchatServerUDP;

/**
 * UDP服务器的启动类
 */
public class StartServerUDP {
    public static void main(String[] args) {
        System.out.println("检查数据库连接");
        if(!DBUtil.connectDB()) return;
        System.out.println("启动UDP聊天室服务器...");
        YYchatServerUDP serverUDP = new YYchatServerUDP();
        serverUDP.startServer();

        // 保持主线程运行
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}