package com.yychat.model;

import java.io.Serializable;
import java.util.Date;

public class Message implements Serializable,MessageType {
    String MessageType;
    private String sender;
    private String receiver;
    private String content;
    Date time;
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

    public void setTime(Date time) {
        this.time = time;
    }

    public Date getTime() {
        return time;
    }
}
