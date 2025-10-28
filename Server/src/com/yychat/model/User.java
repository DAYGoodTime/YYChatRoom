package com.yychat.model;

import java.io.Serializable;

public class User implements Serializable {
    String userName;
    String password;

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

    public User(String name, String password) {
        this.userName = name;
        this.password = password;
    }
}
