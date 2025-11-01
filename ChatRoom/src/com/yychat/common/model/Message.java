package com.yychat.common.model;

import cn.hutool.json.JSONObject;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Message implements Serializable, MessageType {
    private static final long serialVersionUID = 1L;

    private String MessageType;
    private String sender;
    private String receiver;
    @Deprecated
    private String content;
    private final LocalDateTime time;
    private boolean isJsonMessage = false;
    private boolean isSyncMessage = false;
    private String syncTaskId;
    private JSONObject json;
    private boolean attachment;
    private Object attachmentObject;
    private AttachmentType attachmentType;
    private Class<?> attachmentClass;

    private Message() {
        this.time = LocalDateTime.now();
    }

    public static Message builder() {
        return new Message();
    }

    // Special Getter
    public <T> T getAttachment(Class<T> clz) throws IllegalArgumentException, ClassCastException {
        if (!attachment) throw new IllegalArgumentException("非附件消息");
        if (this.attachmentClass.equals(clz)) {
            return clz.cast(attachmentObject);
        }
        throw new IllegalArgumentException("附件类型不一致");
    }

    // Special Setter
    public <T> Message setAttachment(T attachment, Class<T> clz, AttachmentType type) {
        this.attachment = true;
        this.attachmentObject = attachment;
        this.attachmentClass = clz;
        this.attachmentType = type;
        return this;
    }

    // Setter
    public Message setSyncTaskId(String syncTaskId) {
        this.isSyncMessage = true;
        this.syncTaskId = syncTaskId;
        return this;
    }

    public Message setAttachmentType(AttachmentType attachmentType) {
        this.attachmentType = attachmentType;
        return this;
    }

    @Deprecated
    public Message setContent(String content) {
        this.content = content;
        return this;
    }

    public Message setReceiver(String receiver) {
        this.receiver = receiver;
        return this;
    }

    public Message setSender(String sender) {
        this.sender = sender;
        return this;
    }

    public Message setMessageType(String messageType) {
        MessageType = messageType;
        return this;
    }

    public Message setSyncMessage(boolean syncMessage) {
        isSyncMessage = syncMessage;
        return this;
    }

    public Message setJsonMessage(JSONObject json) {
        this.isJsonMessage = true;
        this.json = json;
        return this;
    }

    // Getter
    public String getMessageType() {
        return MessageType;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    @Deprecated
    public String getContent() {
        return content;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public boolean isJsonMessage() {
        return isJsonMessage;
    }

    public boolean isSyncMessage() {
        return isSyncMessage;
    }

    public JSONObject getJson() {
        return json;
    }

    public boolean hasAttachment() {
        return attachment;
    }

    public Object getAttachment() {
        return attachment;
    }

    public AttachmentType getAttachmentType() {
        return attachmentType;
    }

    public Class<?> getAttachmentClass() {
        return attachmentClass;
    }

    public String getSyncTaskId() {
        return syncTaskId;
    }
}
