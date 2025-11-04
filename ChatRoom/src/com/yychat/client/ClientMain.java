package com.yychat.client;

import com.yychat.client.service.UserService;
import com.yychat.client.tcp.TCPClient;
import com.yychat.client.udp.UDPClientConnection;
import com.yychat.client.view.ClientLogin;
import com.yychat.client.view.MainWindow;
import com.yychat.common.model.ServiceResponse;
import com.yychat.common.model.User;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientMain {

    public static ExecutorService backgroundThreadPool = Executors.newFixedThreadPool(10);

    private static User currentUser;

    private static MainWindow mainWindow;

    private static UDPClientConnection udpConnection;

    private static TCPClient tcpClient;

    private static List<User> friendList = new ArrayList<>();

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
        if (!tcpClient.connect()) {
            JOptionPane.showMessageDialog(new JPanel(), "无法启动,请检查服务器连接", "启动失败", JOptionPane.ERROR_MESSAGE);
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
            SwingUtilities.invokeLater(ClientLogin::new);
        }
    }

    /**
     * 执行快速登录操作
     *
     * @param username 用户名
     * @param password 密码
     */
    private static void performQuickLogin(String username, String password) {
        mainWindow = ClientMain.login(username, password, null);
        if (mainWindow == null) {
            System.err.println("快速登录失败");
            System.exit(-1);
        }
        System.out.println("快速登录成功");
    }

    public static MainWindow login(String name, String password, Component target) {
        UserService userService = UserService.getInstance();
        ServiceResponse<?> response = userService.loginByUserName(name, password);
        if (!response.isSuccess()) {
            if (target == null) {
                System.out.println("登录失败:" + response.getMessage());
            } else {
                JOptionPane.showMessageDialog(target, response.getMessage());
            }
            return null;
        }
        // 创建主窗口
        MainWindow window = new MainWindow();

        //请求好友列表
        userService.requestFriends();
        // 请求在线好友
        userService.requestOnlineFriends();
        //请求陌生人
        userService.requestUnknownFriends();
        // 通知服务器有新用户上线
        userService.broadcastNewFriendOnline();

        return window;
    }

    //Getter
    public static MainWindow getMainWindow() {
        return mainWindow;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static String getCurrentUserName() {
        return currentUser.getUserName();
    }

    public static UDPClientConnection getUDPConnection() {
        return udpConnection;
    }

    public static TCPClient getTCPConnection() {
        return tcpClient;
    }

    public static List<User> getFriendList() {
        return friendList;
    }

    //Setter
    public static void setCurrentUser(User currentUser) {
        ClientMain.currentUser = currentUser;
    }

    public static void setMainWindow(MainWindow mainWindow) {
        ClientMain.mainWindow = mainWindow;
    }

    public static void setFriendList(List<User> friendList) {
        ClientMain.friendList = friendList;
    }
}
