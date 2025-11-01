package com.yychat.server.tcp;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yychat.common.model.Message;
import com.yychat.common.model.MessageType;
import com.yychat.common.model.SystemUser;
import com.yychat.server.service.AvatarFileManager;
import com.yychat.server.service.FileManager;
import com.yychat.server.service.UserService;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * TCP接收线程，用于处理单个客户端的文件传输连接
 */
public class ServerReceiverThreadTCP implements Runnable {
    private Socket socket;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    private boolean running = true;
    private static final FileManager fileManager = FileManager.getInstance();

    public ServerReceiverThreadTCP(Socket socket) {
        this.socket = socket;
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            running = false;
        }
    }

    @Override
    public void run() {
        try {
            while (running && !socket.isClosed()) {
                try {
                    Object obj = ois.readObject();
                    if (obj instanceof Message) {
                        Message message = (Message) obj;
                        // 异步处理消息，提高并发性能
                        new Thread(() -> handleMessage(message), "TCP-Message-Handler").start();
                    }
                } catch (SocketException e) {
                    if (running) {
                        System.err.println("客户端连接异常断开: " + e.getMessage());
                    }
                    break;
                } catch (EOFException e) {
                    System.out.println("客户端正常断开连接");
                    break;
                } catch (IOException e) {
                    if (running) {
                        System.err.println("TCP消息接收异常: " + e.getMessage());
                    }
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("消息类型不存在: " + e.getMessage());
                }
            }
        } finally {
            if (running) {
                System.out.println("TCP连接线程意外结束，关闭连接");
            }
            closeConnection();
        }
    }

    private void handleMessage(Message message) {
        System.out.println("从用户: " + message.getSender() + " 接收到TCP消息 :" + JSONUtil.toJsonStr(message));
        Message response;
        switch (message.getMessageType()) {
            case MessageType.TCP_FILE_UPLOAD:
                response = fileManager.handelFileMessage(message);
                break;
            case MessageType.TCP_FILE_DOWNLOAD:
                response = AvatarFileManager.handelUserAvatarDownload(message);
                break;
            case MessageType.TCP_ACK:
                response = message.setSender(message.getReceiver()).setReceiver(message.getSender());
                break;
            case MessageType.TCP_HEARTBEAT:
                // 处理心跳消息，发送心跳响应
                System.out.println("收到心跳消息 from " + message.getSender());
                response = Message.builder()
                        .setMessageType(MessageType.TCP_HEARTBEAT_ACK)
                        .setSender(SystemUser.Server.getStr())
                        .setReceiver(message.getSender())
                        .setJsonMessage(new JSONObject().set("status", "OK").set("timestamp", System.currentTimeMillis()));
                break;
            default:
                System.out.println("非支持的消息类型: " + message.getMessageType() + " 已丢弃");
                response = Message.builder()
                        .setMessageType(message.getMessageType())
                        .setSender(SystemUser.Server.getStr())
                        .setReceiver(message.getSender())
                        .setJsonMessage(new JSONObject()
                                .set("success", false)
                                .set("message", "非支持的消息类型: " + message.getMessageType()));
        }
        if (message.isSyncMessage()) {
            response.setSyncTaskId(message.getSyncTaskId());
        }
        System.out.println("向" + response.getReceiver() + "发送消息 " + JSONUtil.toJsonStr(response));
        sendResponse(response);
    }

    private void sendResponse(Message message) {
        try {
            // 检查连接状态
            if (socket.isClosed() || !socket.isConnected()) {
                System.err.println("连接已关闭，无法发送响应");
                running = false;
                return;
            }

            synchronized (oos) {
                oos.writeObject(message);
                oos.flush();
                System.out.println("成功发送响应给 " + message.getReceiver());
            }
        } catch (IOException e) {
            System.err.println("发送响应失败: " + e.getMessage());
            running = false; // 停止处理，连接可能已断开
        }
    }

    /**
     * 检查连接是否仍然有效
     */
    private boolean isConnectionValid() {
        try {
            return socket != null && !socket.isClosed() && socket.isConnected() &&
                   !socket.isInputShutdown() && !socket.isOutputShutdown();
        } catch (Exception e) {
            return false;
        }
    }

    private void closeConnection() {
        running = false;
        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}