package com.yychat.view;

import com.yychat.api.Client;

import javax.swing.*;

public class ClientMain {

    private static Client client;

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
            System.out.println("UDP客户端正在关闭...");
        }));
        // 启动UDP登录界面
        SwingUtilities.invokeLater(()->client = new ClientLoginUDP());
    }
}
