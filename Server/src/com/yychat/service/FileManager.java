package com.yychat.service;

import cn.hutool.json.JSONObject;
import com.yychat.control.AvatarFileManager;
import com.yychat.model.FileMessage;
import com.yychat.model.Message;
import com.yychat.model.MessageType;
import com.yychat.model.Receiver;

public class FileManager {

    private static final FileManager instance = new FileManager();

    public static FileManager getInstance() {
        return instance;
    }

    protected FileManager() {
    }

    public Message handelFileMessage(Message message){
        if (!message.hasAttachment()){return logError("非附件消息，已丢弃",message.getSender());}
        if (!message.isJsonMessage()
                || message.getJson().getStr("username") == null
                || message.getJson().getStr("filename") == null
        ){
            return logError("附件信息丢失，已丢弃",message.getSender());
        }

        try{message.getAttachment(FileMessage.class);}
        catch (ClassCastException | IllegalArgumentException e)
        {return logError("附件无法获取，已丢弃",message.getSender());}

        switch (message.getAttachmentType()){
            case IMAGE_AVATAR:
                return processAvatarUpload(message);
            default:
                return logError("未知的附件类型 :" + message.getAttachmentType(),message.getSender());
        }
    }

    private Message processAvatarUpload(Message message){
        String username = message.getJson().getStr("username");
        String fileName = message.getJson().getStr("filename");
        FileMessage attachment = message.getAttachment(FileMessage.class);
        String path = AvatarFileManager.saveUserAvatarFromBytes(username,attachment.getFileContent(),fileName);
        if(path == null){ return logError("保存头像失败",message.getSender()); }
        Message response = new Message(MessageType.TCP_FILE_UPLOAD_ACK, Receiver.Server,message.getSender());
        JSONObject json = new JSONObject().set("success", true);
        json.set("username", username);
        json.set("filename", path);
        response.setJsonMessage(json);
        return response;

    }


    private static Message logError(String message,String sender){
        Message response = new Message(MessageType.TCP_FILE_UPLOAD_ACK, Receiver.Server,sender);
        response.setJsonMessage(new JSONObject().set("success",false).set("message",message));
        System.out.println(message);
        return response;
    }
}
