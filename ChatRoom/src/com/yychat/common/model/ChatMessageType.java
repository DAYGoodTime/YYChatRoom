package com.yychat.common.model;

public enum ChatMessageType {
    UserChatPainText(0, "UserChatPainText"),
    ;
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

    public static ChatMessageType fromCode(int code) {
        for (ChatMessageType type : ChatMessageType.values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        return null;
    }
}
