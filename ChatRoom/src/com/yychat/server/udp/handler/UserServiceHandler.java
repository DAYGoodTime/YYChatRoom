package com.yychat.server.udp.handler;

import cn.hutool.json.JSONObject;
import com.yychat.common.model.*;
import com.yychat.server.service.UserService;
import com.yychat.server.udp.ServerReceiverThreadUDP;
import com.yychat.server.udp.YYChatUDPServer;
import com.yychat.server.util.DBUtil;
import com.yychat.server.view.StartServer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class UserServiceHandler {

    private final UserService userService;
    private final InetSocketAddress clientAddress;
    private final YYChatUDPServer udpServer;
    private final ServerReceiverThreadUDP receiverThread;

    public UserServiceHandler(UserService userService, InetSocketAddress clientAddress, ServerReceiverThreadUDP receiverThread) {
        this.userService = userService;
        this.clientAddress = clientAddress;
        this.receiverThread = receiverThread;
        udpServer = StartServer.getUDPServer();
    }

    public void handelReqUserInfo(Message message) {
        ServiceResponse<User> optionalUser = userService.queryUserInfoByUserName(message.getJson().getStr("username", ""));
        Message response = Message.builder()
                .setMessageType(message.getMessageType())
                .setSender(SystemUser.Server.getStr())
                .setReceiver(message.getSender());
        JSONObject obj = new JSONObject();
        if (!optionalUser.isSuccess()) {
            obj.set("success", false);
            obj.set("message", optionalUser.getMessage());
        } else {
            obj.set("success", true);
            obj.set("data", optionalUser.getData());
        }
        response.setJsonMessage(obj);
        udpServer.sendMessageToClient(clientAddress, response, message);
    }

    public void handleUserExit(Message message) {
        System.out.println(message.getSender() + " 退出登录");
        udpServer.getUserAddressMap().remove(message.getSender());
        udpServer.broadcastMessage(message);
        receiverThread.stop();
    }

    public void handleRequestOnlineFriends(Message message) {
        Set<String> onlineUserSet = udpServer.getUserAddressMap().keySet();
        List<String> allFriends = DBUtil.getAllFriends(message.getSender(), FriendType.NORMAL.getCode());
        List<String> onlineFriendList = allFriends.stream()
                .filter(onlineUserSet::contains)
                .collect(Collectors.toList());
        Message response = Message.builder()
                .setReceiver(message.getSender())
                .setSender(SystemUser.Server.getStr())
                .setMessageType(MessageType.REQUEST_ONLINE_FRIENDS)
                .setJsonMessage(new JSONObject().set("list", onlineFriendList));
        udpServer.sendMessageToClient(clientAddress, response, null);
    }

    public void handleNewOnlineFriend(Message message) {
        udpServer.broadcastMessage(message);
    }

    public void handleRequestFriendList(Message message) {
        List<User> allFriends = DBUtil.getAllFriends(message.getSender(), 1).stream()
                .map(s -> userService.queryUserInfoByUserName(s).getData())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Message response = Message.builder()
                .setJsonMessage(new JSONObject().set("list", allFriends))
                .setMessageType(MessageType.REQUEST_FRIEND_LIST)
                .setReceiver(message.getSender())
                .setSender(SystemUser.Server.getStr());
        udpServer.sendMessageToClient(clientAddress, response, null);
    }

    public void handleAddNewFriend(Message message) {
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
        udpServer.sendMessageToClient(clientAddress, response, null);
    }

    public void handelRequestUnkUsers(Message message) {
        List<User> unknownUsers = DBUtil.getUnknowUsers(message.getSender());
        Message response = Message.builder()
                .setJsonMessage(new JSONObject().set("list", unknownUsers))
                .setMessageType(MessageType.REQUEST_UNK_USER_LIST)
                .setReceiver(message.getSender())
                .setSender(SystemUser.Server.getStr());
        udpServer.sendMessageToClient(clientAddress, response, null);
    }

    /**
     * 处理获取头像请求
     */
    public void handleRequestAvatar(Message message) {
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
        udpServer.sendMessageToClient(clientAddress, response, message);
    }
}
