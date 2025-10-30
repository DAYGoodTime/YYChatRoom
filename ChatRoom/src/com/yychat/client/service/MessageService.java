package com.yychat.client.service;

import cn.hutool.json.JSONObject;
import com.yychat.client.ClientMain;
import com.yychat.common.model.*;

public class MessageService {

    private static final MessageService instance = new MessageService();

    public static MessageService getInstance() {
        return instance;
    }

    public ServiceResponse<?> sendPlainTextMessageToUser(
            User sender,
            User receiver,
            String textContent
    ) {
        JSONObject json = new JSONObject();
        json.set("chat_type", ChatMessageType.UserChatPainText.getCode());
        json.set("content", textContent);
        Message message = Message.builder()
                .setMessageType(MessageType.COMMON_CHAT_MESSAGE)
                .setSender(sender.getUserName())
                .setReceiver(receiver.getUserName())
                .setJsonMessage(json);
        try {
            ClientMain.getUDPConnection().sendChatMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceResponse.error(e.getLocalizedMessage());
        }
        return ServiceResponse.success(null);
    }
}
