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
    private boolean hasAttachment;
    private Object attachment;
    private AttachmentType attachmentType;
    private Class<?> attachmentClass;

    public Message(){}
    public Message(String messageType, String sender, String receiver) {
        MessageType = messageType;
        this.sender = sender;
        this.receiver = receiver;
    }
    public Message(String messageType, Receiver sender, String receiver) {
        MessageType = messageType;
        this.sender = sender.getStr();
        this.receiver = receiver;
    }

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

    public boolean hasAttachment() {return hasAttachment;}

    public <T> void setAttachment(T attachment,Class<T> clz,AttachmentType type){
        this.hasAttachment = true;
        this.attachment = attachment;
        this.attachmentClass = clz;
        this.attachmentType = type;
    }

    public AttachmentType getAttachmentType() {
        return attachmentType;
    }

    public <T> T getAttachment(Class<T> clz) throws IllegalArgumentException, ClassCastException {
        if(!hasAttachment) throw new IllegalArgumentException("非附件消息");
        if(this.attachmentClass.equals(clz)){
           return clz.cast(attachment);
        }
        throw new IllegalArgumentException("附件类型不一致");
    }

}
