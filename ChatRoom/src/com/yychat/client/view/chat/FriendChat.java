package com.yychat.client.view.chat;

import com.yychat.client.view.MainWindow;
import com.yychat.common.model.Message;
import com.yychat.common.model.User;

import javax.swing.*;

/**
 * 好友聊天窗口
 * 继承 BaseChatFrame，使用简单的好友聊天布局
 */
public class FriendChat extends BaseChatFrame implements ChatMessageWindow {

    protected User receiver;
    protected FriendChatPanel friendChatPanel;

    public FriendChat(User sender, User receiver, String chatKey) {
        super("与 " + receiver.getUserName() + " 的聊天界面", sender);
        this.receiver = receiver;
        // 创建好友聊天面板
        this.friendChatPanel = new FriendChatPanel(sender, receiver,this);
        // 初始化窗口
        initializeFrame();
        setupLayout();
        // 将聊天窗口存储到FriendList的map中
        MainWindow.getFriendChatMap().put(chatKey, this);
        // 尝试加载历史信息
        friendChatPanel.updateChatMessages();
        // 自动滚动到底部（显示最新消息）
        SwingUtilities.invokeLater(() -> {
            friendChatPanel.messageArea.setCaretPosition(friendChatPanel.messageArea.getDocument().getLength());
        });

    }

    @Override
    protected JPanel createChatPanel() {
        return friendChatPanel;
    }


    @Override
    public void appendMessage(Message message, boolean received) {
        friendChatPanel.appendMessage(message,received);
    }
}