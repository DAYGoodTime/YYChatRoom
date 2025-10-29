package com.yychat.model;

public enum Receiver {
    Server("Server"),;
    private String str;
    Receiver(String str) {
        this.str = str;
    }

    public String getStr() {
        return str;
    }
}
