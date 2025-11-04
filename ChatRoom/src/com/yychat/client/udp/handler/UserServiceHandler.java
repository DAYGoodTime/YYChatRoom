package com.yychat.client.udp.handler;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.yychat.client.ClientMain;
import com.yychat.client.service.UserService;
import com.yychat.client.view.MainWindow;
import com.yychat.client.view.listpanel.FriendListPanel;
import com.yychat.common.model.Message;
import com.yychat.common.model.User;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class UserServiceHandler {

    private static UserService userService = UserService.getInstance();

    public static void handleResponseOnlineFriends(Message message) {
        try {
            MainWindow mainWindow = ClientMain.getMainWindow();
            if (mainWindow != null && message.isJsonMessage()) {
                JSONArray list = message.getJson().getJSONArray("list");
                System.out.println("收到在线好友列表: " + list.toJSONString(0));
                if (!list.isEmpty()) {
                    final FriendListPanel friendListPanel = mainWindow.getFriendListPanel();
                    list.toList(String.class)
                            .forEach(friend -> friendListPanel.changeFriendIconStatus(friend, true));
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
        MainWindow mainWindow = ClientMain.getMainWindow();
        if (mainWindow != null) {
            System.out.println("新好友上线通知: " + sender);
            mainWindow.getFriendListPanel().changeFriendIconStatus(sender, true);
        }
    }

    public static void handleAddNewFriendResponse(Message message) {
        if (!message.isJsonMessage() || message.getJson().getBool("success", null) == null) {
            System.out.println("添加好友响应格式错误");
        }
        MainWindow mainWindow = ClientMain.getMainWindow();
        if (mainWindow != null && message.getJson().getBool("success", false)) {
            JOptionPane.showMessageDialog(mainWindow, "添加好友成功！");
            mainWindow.getFriendListPanel().addNewFriend(message.getJson().getBean("friend",User.class));
        } else {
            JOptionPane.showMessageDialog(mainWindow, message.getJson().getStr("message", ""), "添加好友失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void handleFriendListResponse(Message message) {
        MainWindow mainWindow = ClientMain.getMainWindow();
        if (mainWindow != null && message.isJsonMessage()) {
            System.out.println("收到好友列表: " + message.getJson().getJSONArray("list").toJSONString(0));
            new Thread(() -> {
                //初始化的时候不要影响原线程
                List<User> friendList = message.getJson().getJSONArray("list").toList(User.class);
                if(friendList == null) friendList = new ArrayList<>();
                mainWindow.getFriendListPanel().setFriendList(friendList);
            }).start();
        }
    }

    public static void handelUnknownFriendsResponse(Message message) {
        try {
            MainWindow mainWindow = ClientMain.getMainWindow();
            if (mainWindow != null && message.isJsonMessage()) {
                JSONArray list = message.getJson().getJSONArray("list");
                System.out.println("收到陌生人列表: " + list.toJSONString(0));
                if (!list.isEmpty()) {
                    mainWindow.getStrangerListPanel().updateStrangerPanel(list.toList(User.class));
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
        MainWindow mainWindow = ClientMain.getMainWindow();
        if (mainWindow != null) {
            System.out.println("好友下线通知: " + sender);
            mainWindow.getFriendListPanel().changeFriendIconStatus(sender, false);
        }
    }

    public static void handleRemoveFriendResponse(Message message) {
        if (!message.isJsonMessage() || message.getJson().getBool("success", null) == null) {
            System.out.println("删除好友响应格式错误");
            return;
        }

        JSONObject json = message.getJson();
        boolean success = json.getBool("success", false);
        String removedFriend = json.getStr("removedFriend", "");
        String messageText = json.getStr("message", "");

        MainWindow mainWindow = ClientMain.getMainWindow();

        if (success) {
            // 删除成功，更新UI
            if (mainWindow != null && !removedFriend.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    mainWindow.getFriendListPanel().removeFriendFromList(removedFriend);
                    JOptionPane.showMessageDialog(mainWindow,
                        "已成功删除好友：" + removedFriend,
                        "删除好友",
                        JOptionPane.INFORMATION_MESSAGE);
                });
            }
            System.out.println("删除好友成功: " + removedFriend);
        } else {
            // 删除失败，显示错误信息
            if (mainWindow != null) {
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(mainWindow,
                        messageText,
                        "删除好友失败",
                        JOptionPane.ERROR_MESSAGE)
                );
            }
            System.out.println("删除好友失败: " + messageText);
        }
    }
}
