package com.yychat.view;

import com.yychat.api.Client;
import com.yychat.control.YYchatClientConnectionUDP;
import com.yychat.model.User;

import javax.swing.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ClientMain {

    private static Client client;

    public static ExecutorService threadPool = Executors.newFixedThreadPool(10);
    public static String UserName;
    private static User currentUser;
    public static User getCurrentUser() {
        return currentUser;
    }
    public static void setCurrentUser(User currentUser) {
        ClientMain.currentUser = currentUser;
    }

    public static Client getClient() {
        if (client == null) {
            throw  new NullPointerException("Client is null");
        }
        return client;
    }

    public static void main(String[] args) {
//        boolean isUDP = Arrays.stream(args).anyMatch(s -> s.contains("udp"));
//        if(isUDP){
//            //TODO RunAsUDP
//        }
        // 设置关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            YYchatClientConnectionUDP connection = (YYchatClientConnectionUDP) getClient().getConnection();
            connection.close();
            System.out.println("UDP客户端正在关闭...");
        }));
        // 启动UDP登录界面
        SwingUtilities.invokeLater(()->client = new ClientLoginUDP());
    }
}
