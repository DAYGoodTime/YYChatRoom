package com.yychat.client.view.chat.group;

import com.yychat.client.ClientMain;
import com.yychat.client.view.MainWindow;
import com.yychat.client.view.chat.BaseChatFrame;
import com.yychat.client.view.chat.ChatMessageWindow;
import com.yychat.common.model.Group;
import com.yychat.common.model.Message;

import javax.swing.*;

/**
 * 群聊窗口
 * 继承 BaseChatFrame，实现新的群聊布局：头部 + 消息区 + 成员列表
 */
public class GroupChat extends BaseChatFrame implements ChatMessageWindow {

    private Group group;
    protected GroupChatPanel groupChatPanel;

    public GroupChat(String title, Group group) {
        super(title, ClientMain.getCurrentUser(),620,480);
        this.group = group;
        // 创建群聊面板
        this.groupChatPanel = new GroupChatPanel(sender,this);

        initializeFrame();
        setupLayout();

        // 将聊天窗口存储到GroupChat的map中
        MainWindow.getGroupChatMap().put(group.getGroupName(), this);

        // 尝试加载历史信息
        groupChatPanel.updateChatMessages();

        // 自动滚动到底部（显示最新消息）
        SwingUtilities.invokeLater(() -> {
            this.setVisible(true);
            groupChatPanel.messageArea.setCaretPosition(groupChatPanel.messageArea.getDocument().getLength());
        });
    }

    public Group getGroup() {
        return group;
    }
    public void setGroup(Group group) {
        this.group = group;
    }

    @Override
    protected JPanel createChatPanel() {
        return groupChatPanel;
    }

    @Override
    public void appendMessage(Message message, boolean received) {
        groupChatPanel.appendMessage(message,received);
    }
}