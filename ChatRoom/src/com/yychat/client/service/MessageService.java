package com.yychat.client.service;

import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONObject;
import com.yychat.client.ClientMain;
import com.yychat.common.model.*;
import com.yychat.common.util.StringUtil;

import java.util.UUID;

public class MessageService {

    private static final MessageService instance = new MessageService();

    public static MessageService getInstance() {
        return instance;
    }

    public ServiceResponse<Message> sendPlainTextMessageToUser(
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

    public ServiceResponse<Message> sendFileMessageToUser(
            User sender,
            User receiver,
            String textContent,
            byte[] fileContent,
            String fileName
    ) {
        JSONObject json = new JSONObject();
        json.set("chat_type", ChatMessageType.UserChatFile.getCode());
        json.set("content", textContent);
        json.set("file_name", fileName);
        json.set("file_size", fileContent.length);
        json.set("file_md5", MD5.create().digestHex(fileContent));
        Message message = Message.builder()
                .setMessageType(MessageType.COMMON_CHAT_MESSAGE)
                .setSender(sender.getUserName())
                .setReceiver(receiver.getUserName())
                .setJsonMessage(json);
        try {
            //发送普通消息，附带附件需要的信息
            ClientMain.getUDPConnection().sendChatMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceResponse.error(e.getLocalizedMessage());
        }
        return ServiceResponse.success(message);

    }
}
