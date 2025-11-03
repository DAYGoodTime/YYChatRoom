package com.yychat.client.view.chat;


import cn.hutool.core.io.FileUtil;
import com.yychat.client.service.MessageService;
import com.yychat.client.view.MainWindow;
import com.yychat.common.model.*;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;

public class FriendChat extends BaseChat {
    protected User receiver;

    public FriendChat(User sender, User receiver,String chatKey) {
        super("与 " + receiver.getUserName() + " 的聊天界面",sender,chatKey);
        this.receiver = receiver;
        // 将聊天窗口存储到FriendList的map中
        MainWindow.getFriendChatMap().put(chatKey, this);
        //尝试加载历史信息
        loadMessageFromHistory();
        updateChatMessages();
        // 自动滚动到底部（显示最新消息）
        SwingUtilities.invokeLater(() -> {
            messageArea.setCaretPosition(messageArea.getDocument().getLength());
        });
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
    protected void loadMessageFromHistory() {
        ServiceResponse<Page<ChatMessage>> response = MessageService.getInstance().queryUserMessageHistory(
                sender.getUserName(),
                receiver.getUserName(),
                ++chatHistoryIndex,
                chatHistoryPageSize
        );
        if(!response.isSuccess()){
            JOptionPane.showMessageDialog(this,"消息加载失败",response.getMessage(),JOptionPane.WARNING_MESSAGE);
            return;
        }
        Page<ChatMessage> responsePage = response.getData();
        this.chatHistoryIndex = responsePage.getIndex();
        this.chatHistoryPageSize = responsePage.getPageSize();
        this.total = responsePage.getTotal();
        //Set应该可以合并消息
        ChatMessage[] old = chatHistory.toArray(new ChatMessage[]{});
        chatHistory.clear();
        //先添加最旧的，再添加新的
        chatHistory.addAll(responsePage.getList());
        chatHistory.addAll(Arrays.asList(old));
    }

}
