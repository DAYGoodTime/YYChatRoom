package com.yychat.model;

import java.io.Serializable;

public class User implements Serializable {
    String userName;
    String password;
    String avatarPath;

    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public User(String name, String password) {
        this.userName = name;
        this.password = password;
        this.avatarPath = "0.jpg"; // 默认头像
    }

    public User(String name, String password, String avatarPath) {
        this.userName = name;
        this.password = password;
        this.avatarPath = avatarPath;
    }
}
