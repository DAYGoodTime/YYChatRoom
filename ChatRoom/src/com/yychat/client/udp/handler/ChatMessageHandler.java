package com.yychat.client.udp.handler;

import com.yychat.client.ClientMain;
import com.yychat.client.view.chat.GroupChat;
import com.yychat.common.model.Group;
import com.yychat.common.model.ServiceResponse;
import com.yychat.client.service.UserService;
import com.yychat.client.view.chat.FriendChat;
import com.yychat.client.view.MainWindow;
import com.yychat.common.model.Message;
import com.yychat.common.model.User;

import javax.swing.*;
import java.util.Optional;

public class ChatMessageHandler {

    public static void handleChatMessage(Message message) {
        try {
            UserService userService = UserService.getInstance();
            User receiver = ClientMain.getCurrentUser();
            ServiceResponse<User> optionalReceiver = userService.queryUserInfoByUsername(message.getSender());
            User sender = Optional.ofNullable(optionalReceiver).map(ServiceResponse::getData).orElse(new User(message.getReceiver(), null));
            String chatKey = receiver.getUserName() + "to" + sender.getUserName();
            System.out.println("收到来自 " + sender.getUserName() + " 的消息: " + message.getJson().getStr("content"));
            // 查找或创建聊天窗口
            FriendChat chat = MainWindow.getFriendChat(chatKey);
            if (chat == null) {
                // 创建新的聊天窗口
                chat = new FriendChat(receiver, sender,chatKey);
            }
            final FriendChat finalChat = chat;
            chat.highlightChatWindow();
            SwingUtilities.invokeLater(() -> finalChat.appendMessage(message, true));
        } catch (Exception e) {
            System.out.println("处理聊天消息出错");
            e.printStackTrace();
        }
    }

    public static void handleGroupChatMessage(Message message) {
        System.out.println("接收到 " + message.getSender() + " 发给群组 " + message.getReceiver() + "的消息 :" + message.getJson().toJSONString(0));
        try {
            GroupChat chat = MainWindow.getGroupChat(message.getReceiver());
            if (chat == null) {
                String title = "群聊:  " + message.getReceiver();
                Group group = message.getJson().getBean("group_info", Group.class);
                chat = new GroupChat(title, group);
                // 将聊天窗口保存
                MainWindow.getGroupChatMap().put(group.getGroupName(), chat);
            }
            final GroupChat chatInstance = chat;
            chat.highlightChatWindow();
            SwingUtilities.invokeLater(() -> chatInstance.appendMessage(message, true));
        } catch (Exception e) {
            System.out.println("处理聊天消息出错");
            e.printStackTrace();
        }
    }

    public static void handleUserChatHistoryMessage(Message message) {

    }
    public static void handleGroupChatHistoryMessage(Message message) {

    }
}
