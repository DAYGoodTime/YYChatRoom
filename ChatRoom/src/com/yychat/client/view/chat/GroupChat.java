package com.yychat.client.view.chat;

import cn.hutool.core.io.FileUtil;
import com.yychat.client.ClientMain;
import com.yychat.client.service.MessageService;
import com.yychat.common.model.Group;
import com.yychat.common.model.Message;
import com.yychat.common.model.ServiceResponse;

import java.io.File;

public class GroupChat extends BaseChat {

    private final Group group;

    public GroupChat(String title, Group group) {
        super(title, ClientMain.getCurrentUser());
        this.group = group;
    }

    // 实现BaseChat抽象方法：发送文本消息
    @Override
    protected ServiceResponse<Message> sendTextMessage(String text) {
        return MessageService.getInstance().sendPlainTextMessageToGroup(sender,group,text);
    }

    // 实现BaseChat抽象方法：发送文件消息
    @Override
    protected void sendFileMessage(File file, String message) {
        if (selectedFile == null) return;
        try {
            ServiceResponse<Message> response = MessageService.getInstance().sendFileMessageToGroup(
                    sender, group, message, FileUtil.readBytes(file), file.getName());
            if (!response.isSuccess()) {
                appendErrorMessage("文件发送失败: " + response.getMessage());
                return;
            }
            Message responseMessage = response.getData();
            appendSendMessage(responseMessage, false);
            // 清除文件选择
            clearSelectedFile();
        } catch (Exception e) {
            // 显示发送失败消息
            appendErrorMessage("文件发送失败: " + e.getMessage());
        }
    }
}
