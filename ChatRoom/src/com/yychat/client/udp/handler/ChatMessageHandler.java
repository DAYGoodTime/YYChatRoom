package com.yychat.client.udp.handler;

import com.yychat.client.ClientMain;
import com.yychat.common.model.ServiceResponse;
import com.yychat.client.service.UserService;
import com.yychat.client.view.FriendChat;
import com.yychat.client.view.friendlist.FriendList;
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
            FriendChat chat = FriendList.getFriendChat(chatKey);
            if(chat==null){
                // 创建新的聊天窗口
                 chat = new FriendChat(receiver, sender);
                // 将聊天窗口存储到FriendList的map中
                FriendList.getFriendChatMap().put(chatKey, chat);
            }
            final FriendChat finalChat = chat;
            SwingUtilities.invokeLater(()-> finalChat.appendSendMessage(message,true));
        } catch (Exception e) {
            System.out.println("处理聊天消息出错");
            e.printStackTrace();
        }
    }
}
