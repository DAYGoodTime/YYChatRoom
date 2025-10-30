package com.yychat.server.udp;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import com.yychat.common.model.*;
import com.yychat.server.service.UserService;
import com.yychat.server.udp.handler.UserServiceHandler;
import com.yychat.server.util.DBUtil;

import java.io.*;
import java.net.*;
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
    private final YYChatUDPServer serverThread;
    private UserService userService;
    private UserServiceHandler userServiceHandler;

    // 重载构造函数，用于直接处理接收到的数据包
    public ServerReceiverThreadUDP(DatagramPacket packet, YYChatUDPServer serverThread) {
        this.clientAddress = (InetSocketAddress) packet.getSocketAddress();
        this.serverThread = serverThread;
        userService = new UserService(this.clientAddress);
        userServiceHandler = new UserServiceHandler(userService, clientAddress);
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
        // 由于无连接特性，此线程主要用于处理后续消息
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                // 接收逻辑在这里实现
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        // 清理资源
        stop();
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
                    userService.handelUserLoginReq(message);
                    break;
                case MessageType.USER_SIGNUP_REQUEST:
                    userService.handelUserSignupReq(message);
                    break;
                case MessageType.COMMON_CHAT_MESSAGE:
                    handleChatMessage(message);
                    break;
                case MessageType.REQUEST_USER_INFO:
                    userServiceHandler.handelReqUserInfo(message);
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
                case MessageType.REQUEST_UNK_USER_LIST:
                    handelRequestUnkUsers(message);
                    break;
                case MessageType.REQUEST_AVATAR_PATH:
                    handleRequestAvatar(message);
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
        //TODO chat
        JSONObject json = message.getJson();
        String content = json.getStr("content");
        System.out.println(message.getSender()
                + " 对 " + message.getReceiver()
                + " 说: " + content);

        // 记录聊天消息到数据库
        DBUtil.insertChatMessage(message.getSender(), message.getReceiver(), content, message.getTime());

        // 获取接收方的地址
        InetSocketAddress receiverAddress = serverThread.getUserAddress(message.getReceiver());

        if (receiverAddress != null) {
            // 转发消息给接收方
            serverThread.sendMessageToClient(receiverAddress, message, null);
            System.out.println("消息已转发给 " + message.getReceiver());
        } else {
            System.out.println(message.getReceiver() + " 不在线上");
        }
    }

    private void handleUserExit(Message message) {
        System.out.println(message.getSender() + " 退出登录");
        serverThread.removeUser(message.getSender());
        stop();
    }

    private void handleRequestOnlineFriends(Message message) {
        //TODO send result as user obj
        Set<String> onlineFriendSet = serverThread.getUserAddressMap().keySet();
        List<String> allFriends = DBUtil.getAllFriends(message.getSender(), FriendType.NORMAL.getCode());
        List<String> onlineFriendList = allFriends.stream().filter(onlineFriendSet::contains).collect(Collectors.toList());
        Message response = Message.builder()
                .setReceiver(message.getSender())
                .setSender(SystemUser.Server.getStr())
                .setMessageType(MessageType.REQUEST_ONLINE_FRIENDS)
                .setJsonMessage(new JSONObject().set("list", onlineFriendList));
        serverThread.sendMessageToClient(clientAddress, response, null);
    }

    private void handleNewOnlineFriend(Message message) {
        Set<String> onlineFriendSet = serverThread.getUserAddressMap().keySet();
        for (String friendName : onlineFriendSet) {
            if (friendName.equals(message.getSender())) continue;//跳过自己
            InetSocketAddress friendAddress = serverThread.getUserAddress(friendName);
            if (friendAddress != null && !friendName.equals(message.getSender())) {
                message.setReceiver(friendName);
                serverThread.sendMessageToClient(friendAddress, message, null);
            }
        }
    }

    private void handleRequestFriendList(Message message) {
        //TODO send result as user obj
        List<String> allFriends = DBUtil.getAllFriends(message.getSender(), 1);
        Message response = Message.builder()
                .setJsonMessage(new JSONObject().set("list", allFriends))
                .setMessageType(MessageType.REQUEST_FRIEND_LIST)
                .setReceiver(message.getSender())
                .setSender(SystemUser.Server.getStr());
        serverThread.sendMessageToClient(clientAddress, response, null);
    }

    private void handleAddNewFriend(Message message) {
        String sender = message.getSender();
        String targetUser = message.getContent();
        Message response = Message.builder()
                .setReceiver(sender)
                .setSender(SystemUser.Server.getStr());
        JSONObject json = new JSONObject();
        json.set("success", false);
        boolean success = true;
        if (!DBUtil.hasUser(targetUser)) {
            json.set("message", "用户不存在");
            success = false;
        }
        if (DBUtil.isUsersFriend(sender, targetUser, FriendType.NORMAL) && success) {
            json.set("message", "已添加该用户为好友了");
            success = false;
        }
        if (success) {
            // 添加好友关系
            DBUtil.insertIntoFriend(sender, targetUser, FriendType.NORMAL);
            System.out.println("用户 " + sender + " 成功添加好友 " + targetUser);
            json.set("success", true);
        }
        response.setJsonMessage(json);
        serverThread.sendMessageToClient(clientAddress, response, null);
    }

    private void handelRequestUnkUsers(Message message) {
        //TODO send result as user obj
        List<String> unknownUsers = DBUtil.getUnknowUsers(message.getSender());
        Message response = Message.builder()
                .setJsonMessage(new JSONObject().set("list", unknownUsers))
                .setMessageType(MessageType.REQUEST_UNK_USER_LIST)
                .setReceiver(message.getSender())
                .setSender(SystemUser.Server.getStr());
        serverThread.sendMessageToClient(clientAddress, response, null);
    }

    /**
     * 处理获取头像请求
     */
    private void handleRequestAvatar(Message message) {
        String userName = message.getJson().getStr("username", "");
        String avatarPath = DBUtil.getUserAvatar(userName);
        Message response = Message.builder()
                .setMessageType(MessageType.REQUEST_AVATAR_PATH)
                .setReceiver(message.getSender())
                .setSender(SystemUser.Server.getStr())
                .setJsonMessage(new JSONObject()
                        .set("avatarPath", avatarPath)
                        .set("userName", userName)
                );
        serverThread.sendMessageToClient(clientAddress, response, message);
    }

    public void stop() {
        this.isRunning = false;
        Thread.currentThread().interrupt();
    }
}