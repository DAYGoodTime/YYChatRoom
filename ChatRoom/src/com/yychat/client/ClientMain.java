package com.yychat.client;

import com.yychat.client.tcp.TCPClient;
import com.yychat.client.udp.UDPClientConnection;
import com.yychat.client.view.ClientLogin;
import com.yychat.client.view.FriendList;
import com.yychat.client.view.MyInfo;
import com.yychat.client.service.UserService;
import com.yychat.common.model.User;
import com.yychat.common.model.ServiceResponse;

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

    /**
     * 执行快速登录操作
     * @param username 用户名
     * @param password 密码
     */
    private static void performQuickLogin(String username, String password) {
        LoginWindow = new ClientLogin(false);
        LoginWindow.login(username, password);
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
        if(!tcpClient.connect()){
            JOptionPane.showMessageDialog(new JPanel(),"无法启动,请检查服务器连接","启动失败",JOptionPane.ERROR_MESSAGE);
            System.out.println("TCP启动失败，无法启动");
            System.exit(0);
        }

        // 检查命令行参数
        if (args.length >= 2) {
            String username = args[0];
            String password = args[1];
            System.out.println("检测到命令行参数，执行快速登录...");
            System.out.println("用户名: " + username);
            // 在Swing事件线程中执行快速登录
            SwingUtilities.invokeLater(() -> performQuickLogin(username, password));
        } else {
            // 启动登录界面
            System.out.println("启动登录界面");
            SwingUtilities.invokeLater(() -> LoginWindow = new ClientLogin(true));
        }
    }
}
