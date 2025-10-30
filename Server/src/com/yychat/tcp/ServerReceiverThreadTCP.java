package com.yychat.tcp;

import cn.hutool.json.JSONObject;
import com.yychat.model.Message;
import com.yychat.model.MessageType;
import com.yychat.model.Receiver;
import com.yychat.service.FileManager;

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
        } catch (EOFException | ClassNotFoundException e) {
            System.out.println("TCP客户端断开: " + socket.getInetAddress());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeConnection();
        }
    }

    private void handleMessage(Message message) {
        System.out.println("接收到TCP文件消息，从用户: " + message.getSender());
        Message response;
        switch (message.getMessageType()) {
            case MessageType.TCP_FILE_UPLOAD:
                response = fileManager.handelFileMessage(message);
                break;
            default:
                System.out.println("非支持的消息类型: " + message.getMessageType() + " 已丢弃");
                response = new Message(MessageType.TCP_ACK, Receiver.Server, message.getSender());
                response.setJsonMessage(new JSONObject()
                        .set("success", false)
                        .set("message", "非支持的消息类型: " + message.getMessageType()));
        }
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