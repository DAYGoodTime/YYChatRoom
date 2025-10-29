package com.yychat.control;

import com.yychat.api.Connection;
import com.yychat.model.Message;
import com.yychat.model.MessageType;
import com.yychat.model.User;

import java.io.*;
import java.net.*;

/**
 * UDP版本的客户端连接类，替代原有的TCP架构
 * 基于无连接的UDP协议进行通信
 */
public class YYchatClientConnectionUDP implements Connection {
    private static DatagramSocket datagramSocket;
    private static InetSocketAddress serverAddress;
    private static ClientReceiverThreadUDP receiveThread;
    private volatile boolean isConnected = false;

    public YYchatClientConnectionUDP() {
        try {
            // 创建UDP socket，客户端使用随机端口
            datagramSocket = new DatagramSocket();
            serverAddress = new InetSocketAddress("localhost", 5678);

            System.out.println("UDP客户端连接创建成功");
            System.out.println("本地端口: " + datagramSocket.getLocalPort());
            System.out.println("服务器地址: " + serverAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送消息到服务器（UDP方式）
     */
    private void sendMessageToServer(Message message, User user) {
        try {
            // 序列化Message和User对象
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.writeObject(user);
            oos.flush();

            byte[] data = bos.toByteArray();

            // 创建UDP数据包
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress);

            // 发送数据包
            datagramSocket.send(packet);

            System.out.println("发送消息到服务器: " + message.getMessageType());

            oos.close();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收服务器消息
     */
    private Message receiveMessageFromServer() {
        try {
            byte[] buffer = new byte[1024 * 64]; // 64KB缓冲区
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // 接收数据包
            datagramSocket.receive(packet);

            // 反序列化消息
            ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bis);

            Message message = (Message) ois.readObject();

            ois.close();
            bis.close();

            return message;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 登录验证（UDP方式）
     */
    public boolean loginValidate(User user) {
        boolean loginSuccess = false;

        try {
            Message message = new Message();
            message.setSender(user.getUserName());
            message.setReceiver("Server");
            message.setMessageType(MessageType.USER_LOGIN_REQUEST);

            // 发送登录请求
            sendMessageToServer(message, user);

            // 接收服务器响应
            Message responseMessage = receiveMessageFromServer();

            if (responseMessage != null && responseMessage.getMessageType().equals(MessageType.LOGIN_VALIDATE_SUCCESS)) {
                loginSuccess = true;
                isConnected = true;

                // 启动接收线程
                startReceiveThread();
                System.out.println("UDP登录验证成功");
            } else {
                System.out.println("UDP登录验证失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return loginSuccess;
    }

    /**
     * 用户注册（UDP方式）
     */
    public boolean userSignup(User user) {
        boolean result = false;

        try {
            Message signupMessage = new Message();
            signupMessage.setSender(user.getUserName());
            signupMessage.setReceiver("Server");
            signupMessage.setMessageType(MessageType.USER_SIGNUP_REQUEST);

            // 发送注册请求
            sendMessageToServer(signupMessage, user);

            // 接收服务器响应
            Message responseMessage = receiveMessageFromServer();

            if (responseMessage != null && responseMessage.getMessageType().equals(MessageType.USER_SIGNUP_SUCCESS)) {
                result = true;
                System.out.println("UDP注册成功");
            } else {
                System.out.println("UDP注册失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 发送聊天消息
     */
    public void sendChatMessage(Message message) {
        if (!isConnected) {
            System.out.println("客户端未连接，无法发送消息");
            return;
        }

        try {
            // 直接使用UDP发送
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.flush();

            byte[] data = bos.toByteArray();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress);
            datagramSocket.send(packet);

            System.out.println("发送聊天消息: " + message.getContent());

            oos.close();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 请求在线好友列表
     */
    public void requestOnlineFriends(String username) {
        if (!isConnected) {
            System.out.println("客户端未连接，无法请求好友列表");
            return;
        }

        try {
            Message message = new Message();
            message.setSender(username);
            message.setReceiver("Server");
            message.setMessageType(MessageType.REQUEST_ONLINE_FRIENDS);

            sendMessageToServer(message, null); // 不需要User对象
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 启动接收线程
     */
    private void startReceiveThread() {
        if (receiveThread == null || !receiveThread.isAlive()) {
            receiveThread = new ClientReceiverThreadUDP(datagramSocket);
            receiveThread.start();
            System.out.println("UDP消息接收线程已启动");
        }
    }

    /**
     * 停止接收线程
     */
    public void stopReceiveThread() {
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        isConnected = false;
        stopReceiveThread();

        try {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
            System.out.println("UDP客户端连接已关闭");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取Socket状态
     */
    public boolean isConnected() {
        return isConnected && datagramSocket != null && !datagramSocket.isClosed();
    }

    /**
     * 获取本地端口
     */
    public int getLocalPort() {
        return datagramSocket != null ? datagramSocket.getLocalPort() : -1;
    }
}