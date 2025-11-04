package com.yychat.client.view.chat;

import cn.hutool.core.io.FileUtil;
import com.yychat.client.service.MessageService;
import com.yychat.common.model.*;

import java.io.File;

/**
 * 好友聊天面板组件
 * 继承 BaseChatPanel，实现具体的好友聊天逻辑
 */
public class FriendChatPanel extends BaseChatPanel {

    private User receiver;
    protected FriendChat parent;

    public FriendChatPanel(User sender, User receiver, FriendChat parent) {
        super(sender, parent);
        this.receiver = receiver;
        this.parent = parent;
    }

    @Override
    protected ServiceResponse<Message> sendTextMessage(String text) {
        return MessageService.getInstance()
                .sendPlainTextMessageToUser(sender, receiver, text);
    }

    @Override
    protected void sendFileMessage(File file, String message) {
        if (selectedFile == null) return;
        try {
            ServiceResponse<Message> response = MessageService.getInstance().sendFileMessageToUser(
                    sender, receiver, message, FileUtil.readBytes(file), file.getName());
            if (!response.isSuccess()) {
                appendErrorMessage("文件发送失败: " + response.getMessage());
                return;
            }
            Message responseMessage = response.getData();
            appendMessage(responseMessage, false);
            // 清除文件选择
            clearSelectedFile();
        } catch (Exception e) {
            // 显示发送失败消息
            appendErrorMessage("文件发送失败: " + e.getMessage());
        }
    }

    @Override
    protected ServiceResponse<Page<ChatMessage>> loadMessageFromHistory() {
        return MessageService.getInstance().queryUserMessageHistory(
                sender.getUserName(),
                receiver.getUserName(),
                ++chatHistoryIndex,
                chatHistoryPageSize
        );
    }
}