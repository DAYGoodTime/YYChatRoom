package com.yychat.client.udp.handler;

import cn.hutool.json.JSONArray;
import com.yychat.client.ClientMain;
import com.yychat.client.service.UserService;
import com.yychat.client.view.friendlist.FriendList;
import com.yychat.common.model.Message;
import com.yychat.common.model.User;

import javax.swing.*;

public class UserServiceHandler {

    private static UserService userService = UserService.getInstance();

    public static void handleResponseOnlineFriends(Message message) {
        try {
            FriendList friendList = ClientMain.getFriendList();
            if (friendList != null && message.isJsonMessage()) {
                JSONArray list = message.getJson().getJSONArray("list");
                System.out.println("收到在线好友列表: " + list.toJSONString(0));
                if (!list.isEmpty()) {
                    friendList.activeOnlineFriendIcon(list.toList(String.class));
                }
            } else {
                System.out.println("未找到好友列表窗口");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handleNewOnlineFriend(Message message) {
        String sender = message.getSender();
        FriendList friendList = ClientMain.getFriendList();
        if (friendList != null) {
            System.out.println("新好友上线通知: " + sender);
            friendList.changeFriendIconStatus(sender,true);
        }
    }

    public static void handleAddNewFriendResponse(Message message) {
        if (!message.isJsonMessage() || message.getJson().getBool("success", null) == null) {
            System.out.println("添加好友响应格式错误");
        }
        FriendList friendList = ClientMain.getFriendList();
        if (friendList != null && message.getJson().getBool("success", false)) {
            JOptionPane.showMessageDialog(friendList, "添加好友成功！");
            friendList.addNewFriend(message.getContent());
        } else {
            JOptionPane.showMessageDialog(friendList, message.getJson().getStr("message", ""), "添加好友失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void handleFriendListResponse(Message message) {
        FriendList friendList = ClientMain.getFriendList();
        if (friendList != null && message.isJsonMessage()) {
            System.out.println("收到好友列表: " + message.getJson().getJSONArray("list").toJSONString(0));
            new Thread(() -> {
                //初始化的时候不要影响原线程
                friendList.setFriendList(message.getJson().getJSONArray("list").toList(User.class));
            }).start();
        }
    }

    public static void handelUnknownFriendsResponse(Message message) {
        try {
            FriendList friendList = ClientMain.getFriendList();
            if (friendList != null && message.isJsonMessage()) {
                JSONArray list = message.getJson().getJSONArray("list");
                System.out.println("收到陌生人列表: " + list.toJSONString(0));
                if (!list.isEmpty()) {
                    friendList.updateStrangerPanel(list.toList(User.class), false);
                }
            } else {
                System.out.println("未找到陌生人窗口");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void handelFriendOffline(Message message) {
        String sender = message.getSender();
        FriendList friendList = ClientMain.getFriendList();
        if (friendList != null) {
            System.out.println("好友下线通知: " + sender);
            friendList.changeFriendIconStatus(sender,false);
        }
    }
}
