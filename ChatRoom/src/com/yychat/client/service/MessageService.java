package com.yychat.client.service;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.digest.MD5;
import cn.hutool.json.JSONObject;
import com.yychat.client.ClientMain;
import com.yychat.common.model.*;
import com.yychat.common.util.ThumbnailGenerator;

import java.util.Optional;

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
        ServiceResponse<Message> serviceResponse = new ServiceResponse<>(Message.builder());
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
            return serviceResponse.error(e.getLocalizedMessage());
        }
        return serviceResponse.success(message);
    }

    public ServiceResponse<Message> sendFileMessageToUser(
            User sender,
            User receiver,
            String textContent,
            byte[] fileContent,
            String fileName
    ) {
        ServiceResponse<Message> serviceResponse = new ServiceResponse<>(Message.builder());
        JSONObject json = new JSONObject();
        json.set("chat_type", ChatMessageType.UserChatFile.getCode());
        json.set("content", textContent);
        json.set("file_name", fileName);
        json.set("file_size", fileContent.length);
        String fileMd5 = MD5.create().digestHex(fileContent);
        json.set("file_md5", fileMd5);
        // 检查是否为图片文件，如果是则生成缩略图信息
        wrapperImage(fileName, fileContent, json);

        Message message = Message.builder()
                .setMessageType(MessageType.COMMON_CHAT_MESSAGE)
                .setSender(sender.getUserName())
                .setReceiver(receiver.getUserName())
                .setJsonMessage(json);
        try {
            //先进行文件上传，以防接受者需要下载的时候还没上传完成
            Message fileMessage = Message.builder()
                    .setMessageType(Message.TCP_FILE_UPLOAD)
                    .setSender(sender.getUserName())
                    .setReceiver(SystemUser.Server.getStr())
                    .setJsonMessage(new JSONObject().set("file_md5", fileMd5))
                    .setAttachment(fileContent, byte[].class, AttachmentType.MESSAGE_FILE);
            Optional<Message> fileUploadResponse = ClientMain.getTCPConnection().sendMessage(fileMessage);
            if (!fileUploadResponse.isPresent()) {
                return serviceResponse.error("无法上传文件");
            }
            //发送普通消息，附带附件需要的信息
            ClientMain.getUDPConnection().sendChatMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error(e.getLocalizedMessage());
        }
        return serviceResponse.success(message);

    }

    /**
     * 发送群组消息
     */
    public ServiceResponse<Message> sendPlainTextMessageToGroup(User sender, Group group, String content) {
        ServiceResponse<Message> serviceResponse = new ServiceResponse<>(Message.builder());
        JSONObject json = new JSONObject();
        json.set("chat_type", ChatMessageType.GroupChatPainText.getCode());
        json.set("content", content);
        json.set("group_id", group.getGroupId());
        Message message = Message.builder()
                .setMessageType(MessageType.GROUP_CHAT_MESSAGE)
                .setSender(sender.getUserName())
                .setReceiver(group.getGroupName())
                .setJsonMessage(json);
        try {
            ClientMain.getUDPConnection().sendChatMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error(e.getLocalizedMessage());
        }
        return serviceResponse.success(message);
    }

    /**
     * 发送群组文件消息
     */
    public ServiceResponse<Message> sendFileMessageToGroup(
            User sender,
            Group group,
            String textContent,
            byte[] fileContent,
            String fileName
    ) {
        ServiceResponse<Message> serviceResponse = new ServiceResponse<>(Message.builder());
        JSONObject json = new JSONObject();
        json.set("chat_type", ChatMessageType.GroupChatFile.getCode());
        json.set("content", textContent);
        json.set("file_name", fileName);
        json.set("file_size", fileContent.length);
        json.set("group_id", group.getGroupId());
        String fileMd5 = MD5.create().digestHex(fileContent);
        json.set("file_md5", fileMd5);
        // 检查是否为图片文件，如果是则生成缩略图信息
        wrapperImage(fileName, fileContent, json);

        Message message = Message.builder()
                .setMessageType(MessageType.GROUP_CHAT_MESSAGE)
                .setSender(sender.getUserName())
                .setReceiver(group.getGroupName())
                .setJsonMessage(json);
        try {
            //先进行文件上传，以防接受者需要下载的时候还没上传完成
            Message fileMessage = Message.builder()
                    .setMessageType(Message.TCP_FILE_UPLOAD)
                    .setSender(sender.getUserName())
                    .setReceiver(SystemUser.Server.getStr())
                    .setJsonMessage(new JSONObject().set("file_md5", fileMd5))
                    .setAttachment(fileContent, byte[].class, AttachmentType.MESSAGE_FILE);
            Optional<Message> fileUploadResponse = ClientMain.getTCPConnection().sendMessage(fileMessage);
            if (!fileUploadResponse.isPresent()) {
                return serviceResponse.error("无法上传文件");
            }
            //发送普通消息，附带附件需要的信息
            ClientMain.getUDPConnection().sendChatMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error(e.getLocalizedMessage());
        }
        return serviceResponse.success(message);

    }

    /**
     * 检查文件是否为支持的图片格式
     */
    private boolean isImageFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.matches(Constant.IMAGE_REX);
    }

    private void wrapperImage(String fileName, byte[] fileContent, JSONObject json) {

        boolean isImage = isImageFile(fileName);
        json.set("is_image", isImage);
        if (isImage) {
            // 生成图片缩略图
            ThumbnailGenerator.ThumbnailResult thumbnailResult = ThumbnailGenerator.generateThumbnailFromBytes(fileContent);
            if (thumbnailResult.isSuccess()) {
                // 将缩略图数据Base64编码后添加到JSON中
                String thumbnailBase64 = Base64.encode(thumbnailResult.getThumbnailData());
                json.set("thumbnail_data", thumbnailBase64);
                json.set("thumbnail_width", thumbnailResult.getThumbnailWidth());
                json.set("thumbnail_height", thumbnailResult.getThumbnailHeight());
                json.set("original_width", thumbnailResult.getOriginalWidth());
                json.set("original_height", thumbnailResult.getOriginalHeight());
            } else {
                System.out.println("缩略图生成失败: " + thumbnailResult.getErrorMessage());
                // 即使缩略图生成失败，文件传输仍可继续
            }
        }
    }

    public ServiceResponse<byte[]> downloadFileFromServer(String md5) {
        ServiceResponse<byte[]> serviceResponse = new ServiceResponse<>(new byte[0]);
        Message message = Message.builder()
                .setMessageType(MessageType.TCP_FILE_DOWNLOAD)
                .setSender(ClientMain.getCurrentUserName())
                .setReceiver(SystemUser.Server.getStr())
                .setJsonMessage(new JSONObject().set("file_md5", md5))
                .setAttachmentType(AttachmentType.MESSAGE_FILE);
        Optional<Message> response = ClientMain.getTCPConnection().sendMessage(message);
        if (!response.isPresent()) {
            return serviceResponse.error("无法下载文件");
        }
        return serviceResponse.success(response.get().getAttachment(byte[].class));
    }


}
