package com.yychat.common.model;

import cn.hutool.json.JSONObject;

import java.time.LocalDateTime;
import java.util.Objects;

public class ChatMessage {
    private Long id;
    private String senderName;
    private String receiver;//id or name
    private JSONObject content;
    private LocalDateTime time;

    public ChatMessage() {
    }

    public ChatMessage(Long id, String senderName, String receiver, JSONObject content, LocalDateTime time) {
        this.id = id;
        this.senderName = senderName;
        this.receiver = receiver;
        this.content = content;
        this.time = time;
    }

    public Long getId() {
        return id;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getReceiver() {
        return receiver;
    }

    public JSONObject getContent() {
        return content;
    }

    public LocalDateTime getTime() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ChatMessage)) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(id, that.id) && Objects.equals(senderName, that.senderName) && Objects.equals(receiver, that.receiver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, senderName, receiver);
    }

    public static ChatMessage fromJSONObject(JSONObject jsonObject) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.id = jsonObject.getLong("id");
        chatMessage.senderName = jsonObject.getStr("senderName");
        chatMessage.receiver = jsonObject.getStr("receiver");
        chatMessage.content = jsonObject.getJSONObject("content");
        chatMessage.time = jsonObject.getLocalDateTime("time",null);
        return chatMessage;
    }
}
