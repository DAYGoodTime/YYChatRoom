package com.yychat.common.model;

public enum SystemUser {
    Server("Server"),;
    private String str;
    SystemUser(String str) {
        this.str = str;
    }

    public String getStr() {
        return str;
    }
}
