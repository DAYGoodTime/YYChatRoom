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
            while (running) {
                Object obj = ois.readObject();
                if (obj instanceof Message) {
                    Message message = (Message) obj;
                    handleMessage(message);
                }
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
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
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("发送响应失败");
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