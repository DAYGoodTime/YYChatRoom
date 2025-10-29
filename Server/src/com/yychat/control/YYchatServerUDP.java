package com.yychat.control;

import com.yychat.model.Message;
import com.yychat.model.MessageType;
import com.yychat.model.User;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UDP版本的服务器类，替代原有的TCP架构
 * 基于无连接的UDP协议，支持无状态消息传输
 */
public class YYchatServerUDP implements Runnable {
    private static final HashMap<String, InetSocketAddress> userAddressMap = new HashMap<>();
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private DatagramSocket datagramSocket;
    private volatile boolean isRunning = false;
    private Thread serverThread;
    private static final int PORT = 5678;

    public void startServer() {
        if (isRunning) {
            System.out.println("服务器已经在运行中了");
            return;
        }
        isRunning = true;
        serverThread = new Thread(this);
        serverThread.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopServer();
        }));
    }

    public void stopServer() {
        if (!isRunning) {
            System.out.println("服务器没有运行中");
            return;
        }
        isRunning = false;
        try {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        threadPool.shutdown();
        System.out.println("UDP服务器已关闭");
    }

    @Override
    public void run() {
        try {
            datagramSocket = new DatagramSocket(PORT);
            System.out.println("UDP服务器启动成功，正在监听" + PORT + "端口...");

            while (isRunning) {
                try {
                    // 创建接收数据包的缓冲区
                    byte[] buffer = new byte[1024 * 64]; // 64KB缓冲区
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    // 接收数据包
                    datagramSocket.receive(packet);

                    // 处理数据包
                    handleClient(packet);
                } catch (java.net.SocketException e) {
                    if (!isRunning) {
                        System.out.println("UDP服务器通信已关闭");
                        break;
                    }
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            if (isRunning) {
                e.printStackTrace();
            }
        } finally {
            stopServer();
            System.out.println("UDP服务器主线程已关闭");
        }
    }

    private void handleClient(DatagramPacket packet) {
        try {
            // 反序列化消息和用户对象
            byte[] data = packet.getData();
            ByteArrayInputStream bis = new ByteArrayInputStream(data, 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bis);

            Message message = (Message) ois.readObject();


            InetSocketAddress clientAddress = (InetSocketAddress) packet.getSocketAddress();

            System.out.println("接收来自 " + clientAddress + " 的消息: " + message.getMessageType());
            System.out.println("消息内容: " + message.getContent());


            message.setSender("Server");
            message.setReceiver(message.getSender());
            // 处理登录请求
            if (message.getMessageType().equals(MessageType.USER_LOGIN_REQUEST)) {
                User user = (User) ois.readObject();
                boolean loginSuccess = DBUtil.loginValidate(user.getUserName(), user.getPassword());
                if (loginSuccess) {
                    System.out.println("密码验证通过!");
                    message.setMessageType(MessageType.LOGIN_VALIDATE_SUCCESS);

                    // 保存用户地址映射
                    userAddressMap.put(user.getUserName(), clientAddress);

                    // 发送响应消息
                    sendMessageToClient(clientAddress, message);

                    // 启动处理线程
                    threadPool.execute(new ServerReceiverThreadUDP(packet));

                    System.out.println("用户 " + user.getUserName() + " 登录成功，地址: " + clientAddress);
                } else {
                    System.out.println("密码验证失败!");
                    message.setMessageType(MessageType.LOGIN_VALIDATE_FAILURE);
                    sendMessageToClient(clientAddress, message);
                }
            }
            // 处理注册请求
            else if (message.getMessageType().equals(MessageType.USER_SIGNUP_REQUEST)) {
                User user = (User) ois.readObject();
                int signupSuccess = -1;
                if (DBUtil.hasUser(user.getUserName())) {
                    // 用户名已存在
                    System.out.println("当前用户名已被注册！");
                } else {
                    signupSuccess = DBUtil.addNewUser(user);
                }

                if (signupSuccess == 1) {
                    message.setMessageType(MessageType.USER_SIGNUP_SUCCESS);
                } else {
                    message.setMessageType(MessageType.USER_SIGNUP_FAILURE);
                }
                sendMessageToClient(clientAddress, message);
            } else {
                // 对于其他消息类型，直接交给接收线程处理
                threadPool.execute(new ServerReceiverThreadUDP(packet));
            }

            ois.close();
            bis.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessageToClient(InetSocketAddress clientAddress, Message message) {
        try {
            // 序列化消息
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.flush();

            byte[] data = bos.toByteArray();

            // 创建数据包并发送
            DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress);
            datagramSocket.send(packet);

            System.out.println("向 " + clientAddress + " 发送消息: " + message.getMessageType());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static InetSocketAddress getUserAddress(String userName) {
        return userAddressMap.get(userName);
    }

    public static HashMap<String, InetSocketAddress> getUserAddressMap() {
        return userAddressMap;
    }

    public static void removeUser(String userName) {
        userAddressMap.remove(userName);
    }
}