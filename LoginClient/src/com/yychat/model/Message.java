package com.yychat.model;

import cn.hutool.json.JSONObject;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

public class Message implements Serializable, MessageType {
    private static final long serialVersionUID = 1L;

    String MessageType;
    private String sender;
    private String receiver;
    private String content;
    private LocalDateTime time;
    private boolean isJsonMessage = false;
    private JSONObject json;
    private byte[] avatarData;       // 头像二进制数据
    private String avatarFileName;   // 头像文件名
    public String getMessageType() {
        return MessageType;
    }
    public void setMessageType(String messageType) {
        this.MessageType = messageType;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setJsonMessage(JSONObject json) {
        isJsonMessage = true;
        this.json = json;
    }

    public JSONObject getJson() {
        return json;
    }

    public boolean isJsonMessage() {
        return isJsonMessage;
    }

    // 头像相关getter和setter方法
    public byte[] getAvatarData() {
        return avatarData;
    }

    public void setAvatarData(byte[] avatarData) {
        this.avatarData = avatarData;
    }

    public String getAvatarFileName() {
        return avatarFileName;
    }

    public void setAvatarFileName(String avatarFileName) {
        this.avatarFileName = avatarFileName;
    }

    /**
     * 获取文件数据（头像文件的二进制数据）
     */
    public byte[] getFileData() {
        return avatarData;
    }

    /**
     * 设置文件数据（头像文件的二进制数据）
     */
    public void setFileData(byte[] fileData) {
        this.avatarData = fileData;
    }
}
