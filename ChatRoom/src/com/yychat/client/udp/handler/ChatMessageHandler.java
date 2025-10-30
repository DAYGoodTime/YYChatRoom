package com.yychat.client.udp.handler;

import com.yychat.client.ClientMain;
import com.yychat.common.model.ChatMessageType;
import com.yychat.common.model.ServiceResponse;
import com.yychat.client.service.UserService;
import com.yychat.client.view.FriendChat;
import com.yychat.client.view.FriendList;
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
            System.out.println("收到来自 " + sender + " 的消息: " + message.getJson().getStr("content"));
            int typeCode = message.getJson().getInt("chat_type", -1);
            if (!ChatMessageType.UserChatPainText.equals(ChatMessageType.fromCode(typeCode))) {
                System.out.println("不支持的消息类型");
                return;
            }
            // 查找或创建聊天窗口
            FriendChat chat = FriendList.getFriendChat(chatKey);
            if (chat != null) {
                chat.append(message);
            } else {
                // 在EDT线程中创建聊天窗口
                SwingUtilities.invokeLater(() -> {
                    try {
                        // 创建新的聊天窗口
                        FriendChat newChat = new FriendChat(receiver, sender);
                        // 将聊天窗口存储到FriendList的map中
                        FriendList.getFriendChatMap().put(chatKey, newChat);
                        // 显示消息
                        newChat.append(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("处理聊天消息出错");
            e.printStackTrace();
        }
    }
}
