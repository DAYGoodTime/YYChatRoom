package com.yychat.server.service;

import cn.hutool.json.JSONObject;
import com.yychat.common.model.FileMessage;
import com.yychat.common.model.Message;
import com.yychat.common.model.SystemUser;


public class FileManager {

    private static final FileManager instance = new FileManager();

    public static FileManager getInstance() {
        return instance;
    }

    protected FileManager() {
    }

    public Message handelFileMessage(Message message) {
        if (!message.hasAttachment()) {
            return logError("非附件消息，已丢弃", message);
        }
        if (!message.isJsonMessage()
                || message.getJson().getStr("username") == null
                || message.getJson().getStr("filename") == null
        ) {
            return logError("附件信息丢失，已丢弃", message);
        }

        try {
            message.getAttachment(FileMessage.class);
        } catch (ClassCastException | IllegalArgumentException e) {
            return logError("附件无法获取，已丢弃", message);
        }

        switch (message.getAttachmentType()) {
            case IMAGE_AVATAR:
                return processAvatarUpload(message);
            default:
                return logError("未知的附件类型 :" + message.getAttachmentType(), message);
        }
    }

    private Message processAvatarUpload(Message message) {
        String username = message.getJson().getStr("username");
        String fileName = message.getJson().getStr("filename");
        FileMessage attachment = message.getAttachment(FileMessage.class);
        String path = AvatarFileManager.saveUserAvatarFromBytes(username, attachment.getFileContent(), fileName);
        if (path == null) {
            return logError("保存头像失败", message);
        }
        Message response = Message.builder().setMessageType(message.getMessageType())
                .setSender(SystemUser.Server.getStr())
                .setReceiver(message.getSender());
        JSONObject json = new JSONObject().set("success", true);
        json.set("username", username);
        json.set("filename", path);
        response.setJsonMessage(json);
        return response;

    }

    private static Message logError(String message, Message request) {
        Message response = Message.builder()
                .setMessageType(request.getMessageType())
                .setSender(request.getSender())
                .setJsonMessage(new JSONObject().set("success", false).set("message", message));
        System.out.println(message);
        return response;
    }
}
