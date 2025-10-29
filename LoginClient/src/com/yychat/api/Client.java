package com.yychat.api;

import com.yychat.view.FriendList;

import javax.swing.*;
import java.util.Map;

public interface Client {
    JFrame getMainWindow();
    MessageThread getMessageThread();
    Connection getConnection();
    Map<String, FriendList> getFriendList();
}
