package com.yychat.common.model;

public enum ChatMessageType {
    UnSupport(-1,"UnSupport"),
    UserChatPainText(0, "UserChatPainText"),
    UserChatFile(1, "UserChatFile"),
    GroupChatPainText(2,"GroupChatPainText"),;
    private final int code;
    private final String type;

    ChatMessageType(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public int getCode() {return code;}
    public String getType() {
        return type;
    }
    @Override
    public String toString() {
        return "{" +
                "code:" + code +
                ", type:" + type +
                "}";
    }

    public static ChatMessageType fromCode(Integer code) {
        for (ChatMessageType type : ChatMessageType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return ChatMessageType.UnSupport;
    }
}
