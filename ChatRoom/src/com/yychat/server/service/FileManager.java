package com.yychat.server.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.yychat.common.model.*;

import java.io.File;


public class FileManager {

    private static final FileManager instance = new FileManager();

    public static FileManager getInstance() {
        return instance;
    }

    protected FileManager() {
    }

    public Message handelFileUploadMessage(Message message) {
        switch (message.getAttachmentType()) {
            case IMAGE_AVATAR:
                return processAvatarUpload(message);
            case MESSAGE_FILE:
                return processMessageFileUpload(message);
            default:
                return logError("未知的附件类型 :" + message.getAttachmentType(), message);
        }
    }

    public Message handelFileDownLoadMessage(Message message) {
        switch (message.getAttachmentType()) {
            case IMAGE_AVATAR:
                return AvatarFileManager.handelUserAvatarDownload(message);
            case MESSAGE_FILE:
                return processMessageFileDownload(message);
            case GROUP_AVATAR:
                return AvatarFileManager.handelGroupAvatarDownload(message);
            default:
                return logError("未知的附件类型 :" + message.getAttachmentType(), message);
        }
    }

    private Message processAvatarUpload(Message message) {
        String username = message.getJson().getStr("username");
        String fileName = message.getJson().getStr("filename");
        boolean isDefault = message.getJson().getBool("is_default");
        String path;
        if (isDefault) {
            path = AvatarFileManager.saveUserAvatarWithDefaultAvatar(username, fileName);
        } else {
            byte[] attachment = message.getAttachment(byte[].class);
            path = AvatarFileManager.saveUserAvatarFromBytes(username, attachment, fileName);
        }
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

    private Message processMessageFileUpload(Message message) {
        if (!message.isJsonMessage() || message.getJson().getStr("file_md5") == null)
            return logError("文件md5不存在", message);
        String fileMd5 = message.getJson().getStr("file_md5");
        byte[] fileContent = message.getAttachment(byte[].class);
        File uploadFile = new File(Constant.SERVER_USER_MESSAGE_FILE_PATH + fileMd5);
        if (!uploadFile.exists()) {
            FileUtil.touch(uploadFile);
            FileUtil.writeBytes(fileContent,uploadFile);
        }
        //如果已存在则不用再保存
        return Message.builder()
                .setMessageType(message.getMessageType())
                .setSender(SystemUser.Server.getStr())
                .setReceiver(message.getSender())
                .setJsonMessage(new JSONObject().set("success", true));
    }

    private Message processMessageFileDownload(Message message) {
        if (!message.isJsonMessage() || message.getJson().getStr("file_md5") == null)
            return logError("文件md5不存在", message);
        String fileMd5 = message.getJson().getStr("file_md5");
        File donwloadFile = new File(Constant.SERVER_USER_MESSAGE_FILE_PATH + fileMd5);
        if (!donwloadFile.exists()) {
            return logError("文件不存在",message);
        }
        return Message.builder()
                .setMessageType(message.getMessageType())
                .setSender(SystemUser.Server.getStr())
                .setReceiver(message.getSender())
                .setJsonMessage(new JSONObject().set("success", true))
                .setAttachment(FileUtil.readBytes(donwloadFile),byte[].class, AttachmentType.MESSAGE_FILE);
    }
}
