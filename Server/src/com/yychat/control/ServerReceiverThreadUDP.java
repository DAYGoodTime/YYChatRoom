package com.yychat.control;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yychat.model.FriendType;
import com.yychat.model.Message;
import com.yychat.model.MessageType;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * UDP版本的服务器接收线程，处理无连接UDP消息
 * 由于UDP是无连接的，此线程主要处理特定用户的消息流
 */
public class ServerReceiverThreadUDP implements Runnable {
    private InetSocketAddress clientAddress;
    private DatagramSocket datagramSocket;
    private volatile boolean isRunning = true;

    public ServerReceiverThreadUDP(InetSocketAddress clientAddress) {
        this.clientAddress = clientAddress;
        try {
            this.datagramSocket = new DatagramSocket(); // 创建独立的socket用于发送响应
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 重载构造函数，用于直接处理接收到的数据包
    public ServerReceiverThreadUDP(DatagramPacket packet) {
        this.clientAddress = (InetSocketAddress) packet.getSocketAddress();
        try {
            this.datagramSocket = new DatagramSocket();
            // 处理接收到的数据包
            processReceivedPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // 对于UDP，由于无连接特性，此线程主要用于处理后续消息
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                // UDP接收逻辑在这里实现
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        // 清理资源
        cleanup();
    }

    private void processReceivedPacket(DatagramPacket packet) {
        try {
            // 反序列化接收到的消息
            byte[] data = packet.getData();
            ByteArrayInputStream bis = new ByteArrayInputStream(data, 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bis);

            Message message = (Message) ois.readObject();

            System.out.println("处理来自 " + clientAddress + " 的消息: " + JSONUtil.toJsonStr(message));

            switch (message.getMessageType()) {
                case MessageType.USER_LOGIN_REQUEST:

                case MessageType.COMMON_CHAT_MESSAGE:
                    handleChatMessage(message);
                    break;

                case MessageType.EXIT:
                    handleUserExit(message);
                    break;

                case MessageType.REQUEST_ONLINE_FRIENDS:
                    handleRequestOnlineFriends(message);
                    break;

                case MessageType.NEW_ONLINE_FRIEND:
                    handleNewOnlineFriend(message);
                    break;

                case MessageType.REQUEST_FRIEND_LIST:
                    handleRequestFriendList(message);
                    break;

                case MessageType.USER_ADD_NEW_FRIEND:
                    handleAddNewFriend(message);
                    break;

                case MessageType.IS_FRIEND_ONLINE:
                    handleIsFriendOnline(message);
                    break;

                default:
                    System.out.println("未处理的消息类型: " + message.getMessageType());
                    break;
            }
            ois.close();
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleChatMessage(Message message) {
        System.out.println(message.getSender()
                + " 对 " + message.getReceiver()
                + " 说: " + message.getContent());

        // 记录聊天消息到数据库
        message.setTime(LocalDateTime.now());
        DBUtil.insertChatMessage(message.getSender(), message.getReceiver(),
                message.getContent(), message.getTime());

        // 获取接收方的地址
        InetSocketAddress receiverAddress = YYchatServerUDP.getUserAddress(message.getReceiver());

        if (receiverAddress != null) {
            // 转发消息给接收方
            sendMessageToClient(receiverAddress, message);
            System.out.println("消息已转发给 " + message.getReceiver());
        } else {
            System.out.println(message.getReceiver() + " 不在线上");
        }
    }

    private void handleUserExit(Message message) {
        System.out.println(message.getSender() + " 退出登录");
        YYchatServerUDP.removeUser(message.getSender());
        this.isRunning = false;
        Thread.currentThread().interrupt();
    }

    private void handleRequestOnlineFriends(Message message) {
        Set<String> onlineFriendSet = YYchatServerUDP.getUserAddressMap().keySet();
        List<String> allFriends = DBUtil.getAllFriends(message.getSender(), FriendType.NORMAL.getCode());
        List<String> onlineFriendList = allFriends.stream().filter(onlineFriendSet::contains).collect(Collectors.toList());

        Message responseMessage = new Message();
        responseMessage.setReceiver(message.getSender());
        responseMessage.setSender("Server");
        responseMessage.setMessageType(MessageType.RESPONSE_ONLINE_FRIENDS);
        responseMessage.setJsonMessage(new JSONObject().set("list",onlineFriendList));

        sendMessageToClient(clientAddress, responseMessage);
    }

    private void handleNewOnlineFriend(Message message) {
        message.setMessageType(MessageType.NEW_ONLINE_TO_ALL_FRIENDS);
        Set<String> onlineFriendSet = YYchatServerUDP.getUserAddressMap().keySet();

        for (String friendName : onlineFriendSet) {
            InetSocketAddress friendAddress = YYchatServerUDP.getUserAddress(friendName);
            if (friendAddress != null && !friendName.equals(message.getSender())) {
                message.setReceiver(friendName);
                sendMessageToClient(friendAddress, message);
            }
        }
    }

    private void handleRequestFriendList(Message message) {
        Message responseMessage = new Message();

        List<String> allFriends = DBUtil.getAllFriends(message.getSender(), 1);
        responseMessage.setJsonMessage(new JSONObject().set("list",allFriends));
        responseMessage.setMessageType(MessageType.RESPONSE_FRIEND_LIST);
        responseMessage.setReceiver(message.getSender());
        responseMessage.setSender("Server");
        sendMessageToClient(clientAddress, responseMessage);
    }

    private void handleAddNewFriend(Message message) {
        String sender = message.getSender();
        String targetUser = message.getContent();

        Message responseMessage = new Message();
        responseMessage.setReceiver(sender);
        responseMessage.setSender("Server");

        if (DBUtil.hasUser(targetUser)) {
            if (DBUtil.isUsersFriend(sender, targetUser, 1)) {
                responseMessage.setMessageType(MessageType.USER_ADD_NEW_FRIEND_FAILURE_ALREADY_FRIEND);
            } else {
                // 添加好友关系
                DBUtil.insertIntoFriend(sender, targetUser, 1);
                responseMessage.setMessageType(MessageType.USER_ADD_NEW_FRIEND_SUCCESS);
                System.out.println("用户 " + sender + " 成功添加好友 " + targetUser);
            }
        } else {
            responseMessage.setMessageType(MessageType.USER_ADD_NEW_FRIEND_FAILURE_NO_USER);
        }

        sendMessageToClient(clientAddress, responseMessage);
    }

    private void handleIsFriendOnline(Message message) {
        Set<String> onlineFriendSet = YYchatServerUDP.getUserAddressMap().keySet();
        Message responseMessage = new Message();

        if (onlineFriendSet.contains(message.getContent())) {
            responseMessage.setMessageType(MessageType.IS_FRIEND_ONLINE_SUCCESS);
        } else {
            responseMessage.setMessageType(MessageType.IS_FRIEND_ONLINE_FAILURE);
        }

        responseMessage.setReceiver(message.getSender());
        responseMessage.setSender("Server");
        sendMessageToClient(clientAddress, responseMessage);
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
            if (datagramSocket == null || datagramSocket.isClosed()) {
                datagramSocket = new DatagramSocket();
            }
            datagramSocket.send(packet);

            System.out.println("向 " + clientAddress + " 发送消息: " + JSONUtil.toJsonStr(message));

            oos.close();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        this.isRunning = false;
        cleanup();
    }

    private void cleanup() {
        try {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}