package com.yychat.client;

import com.yychat.client.tcp.TCPClient;
import com.yychat.client.udp.UDPClientConnection;
import com.yychat.client.view.ClientLogin;
import com.yychat.client.view.FriendList;
import com.yychat.common.model.User;

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientMain {
    public static ExecutorService backgroundThreadPool = Executors.newFixedThreadPool(10);

    private static User currentUser;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static String getCurrentUserName() {
        return currentUser.getUserName();
    }

    public static void setCurrentUser(User currentUser) {
        ClientMain.currentUser = currentUser;
    }

    private static ClientLogin LoginWindow;

    public static ClientLogin getLoginWindow() {
        return LoginWindow;
    }

    public static FriendList getFriendList() {
        return getLoginWindow().getFriendList();
    }


    private static UDPClientConnection udpConnection;

    public static UDPClientConnection getUDPConnection() {
        return udpConnection;
    }

    private static TCPClient tcpClient;

    public static TCPClient getTCPConnection() {
        return tcpClient;
    }

    public static void main(String[] args) {
        System.out.println("初始化客户端中");
        System.out.println("建立UDP连接中");
        udpConnection = new UDPClientConnection();
        // 设置关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("客户端正在关闭...");
            UDPClientConnection connection = getUDPConnection();
            connection.close();
            getTCPConnection().shutdown();
        }));
        // 初始化TCP服务
        System.out.println("正在启动TCP服务");
        tcpClient = new TCPClient();
        tcpClient.connect();
        // 启动登录界面
        System.out.println("启动登录界面");
        SwingUtilities.invokeLater(() -> LoginWindow = new ClientLogin());
    }
}
