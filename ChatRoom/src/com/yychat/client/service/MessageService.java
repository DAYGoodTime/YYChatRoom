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
        return ServiceResponse.success(message);
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
        String fileMd5 = MD5.create().digestHex(fileContent);
        json.set("file_md5", fileMd5);

        // 检查是否为图片文件，如果是则生成缩略图信息
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
                //从result处获取
                json.set("original_width", thumbnailResult.getOriginalWidth());
                json.set("original_height", thumbnailResult.getOriginalHeight());
                json.set("image_format", thumbnailResult.getFormat());
            } else {
                System.out.println("缩略图生成失败: " + thumbnailResult.getErrorMessage());
                // 即使缩略图生成失败，文件传输仍可继续
            }
        }

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
                return ServiceResponse.error("无法上传文件");
            }
            //发送普通消息，附带附件需要的信息
            ClientMain.getUDPConnection().sendChatMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
            return ServiceResponse.error(e.getLocalizedMessage());
        }
        return ServiceResponse.success(message);

    }

    /**
     * 检查文件是否为支持的图片格式
     */
    private boolean isImageFile(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return false;
        }

        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".jpg") ||
               lowerFileName.endsWith(".jpeg") ||
               lowerFileName.endsWith(".png") ||
               lowerFileName.endsWith(".gif") ||
               lowerFileName.endsWith(".bmp");
    }

    public ServiceResponse<byte[]> downloadFileFromServer(String md5){
        Message message = Message.builder()
                .setMessageType(MessageType.TCP_FILE_DOWNLOAD)
                .setSender(ClientMain.getCurrentUserName())
                .setReceiver(SystemUser.Server.getStr())
                .setJsonMessage(new JSONObject().set("file_md5", md5))
                .setAttachmentType(AttachmentType.MESSAGE_FILE);
        Optional<Message> response = ClientMain.getTCPConnection().sendMessage(message);
        if(!response.isPresent()) {
            return ServiceResponse.error("无法下载文件");
        }
        return ServiceResponse.success(response.get().getAttachment(byte[].class));
    }
}
