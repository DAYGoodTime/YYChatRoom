package com.yychat.model;

public enum FriendType {
    BLACKLIST(0,"blacklist"),
    NORMAL(1,"normal");


    private final int code;
    private final String type;
    FriendType(int code, String type) {
        this.code = code;
        this.type = type;
    }

    public int getCode() {
        return code;
    }

    public String getType() {
        return type;
    }
}
