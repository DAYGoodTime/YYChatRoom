package com.yychat.client.view.chat;


import cn.hutool.core.io.FileUtil;
import com.yychat.client.service.AvatarService;
import com.yychat.client.service.MessageService;
import com.yychat.common.model.ChatMessageType;
import com.yychat.common.model.Message;
import com.yychat.common.model.ServiceResponse;
import com.yychat.common.model.User;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

public class FriendChat extends BaseChat {
    protected User receiver;

    // 头像相关字段
    private ImageIcon senderAvatar = null; // 发送者头像（当前用户）
    private ImageIcon receiverAvatar = null; // 接收者头像（对方用户）


    public FriendChat(User sender, User receiver) {
        super("与 " + receiver.getUserName() + " 的聊天界面",sender);
        this.receiver = receiver;
        // 加载头像
        loadAvatars();
    }

    // 实现BaseChat抽象方法：发送文本消息
    @Override
    protected ServiceResponse<Message> sendTextMessage(String text) {
        return MessageService.getInstance()
                .sendPlainTextMessageToUser(sender, receiver, text);
    }

    // 实现BaseChat抽象方法：发送文件消息
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
            appendSendMessage(responseMessage, false);
            // 清除文件选择
            clearSelectedFile();
        } catch (Exception e) {
            // 显示发送失败消息
            appendErrorMessage("文件发送失败: " + e.getMessage());
        }
    }

//    @Override
//    public void appendSendMessage(Message message, boolean received) {
//        // 创建消息面板：左侧头像 + 右侧内容和时间
//        JPanel messagePanel = new JPanel(new BorderLayout(10, 5));
//        // 左侧：发送|接受者者头像（始终显示在顶部）
//        JLabel avatarLabel = createAvatarLabel(received ? receiverAvatar : senderAvatar);
//        // 创建头像容器面板，确保头像始终在顶部
//        JPanel avatarPanel = new JPanel(new BorderLayout());
//        avatarPanel.setOpaque(false);
//        avatarPanel.add(avatarLabel, BorderLayout.NORTH);
//        messagePanel.add(avatarPanel, BorderLayout.WEST);
//        // 顶部：时间 + 发送者名称
//        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
//        headerPanel.setOpaque(false);
//        SimpleDateFormat sdf;
//        if(message.getTime().isBefore(LocalDate.now().atStartOfDay())){
//            sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        }else {
//            sdf = new SimpleDateFormat("HH:mm:ss");
//        }
//        String currentTime = sdf.format(new Date());
//        // 时间标签（灰色）
//        JLabel timeLabel = new JLabel("[" + currentTime + "]");
//        timeLabel.setForeground(Color.GRAY);
//        timeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 10));
//        // 发送者标签（蓝色）
//        JLabel senderLabel;
//        if (received) {
//            senderLabel = new JLabel(message.getSender());
//            senderLabel.setForeground(Color.GREEN);
//            senderLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
//        } else {
//            senderLabel = new JLabel("我");
//            senderLabel.setForeground(Color.BLUE);
//            senderLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
//        }
//
//        headerPanel.add(timeLabel);
//        headerPanel.add(senderLabel);
//
//        // 消息内容标签
//        Component messageLabel;
//        switch (ChatMessageType.fromCode(message.getJson().getInt("chat_type", -1))) {
//            case UserChatPainText:
//                messageLabel = appendTextMessage(message);
//                break;
//            case UserChatFile:
//                messageLabel = appendFileMessage(message, received);
//                break;
//            case UnSupport:
//            default:
//                messageLabel = new JLabel("不支持的消息类型");
//                messageLabel.setForeground(Color.RED);
//                messageLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
//                break;
//        }
//
//        // 右侧：时间和消息内容
//        JPanel rightPanel = new JPanel(new BorderLayout());
//        rightPanel.setOpaque(false);
//
//        rightPanel.add(headerPanel, BorderLayout.NORTH);
//        rightPanel.add(messageLabel, BorderLayout.CENTER);
//
//        messagePanel.add(rightPanel, BorderLayout.CENTER);
//
//        // 添加到文本区域（使用更安全的方式）
//        messageArea.setCaretPosition(messageArea.getDocument().getLength());
//        messageArea.insertComponent(messagePanel);
//
//        appendChangeLine();
//
//        // 自动滚动到底部
//        messageArea.setCaretPosition(messageArea.getDocument().getLength());
//    }

    /**
     * 加载发送者和接收者的头像
     */
    private void loadAvatars() {
        AvatarService avatarService = AvatarService.getInstance();
        // 加载当前用户头像
        senderAvatar = AvatarService.loadUserIcon(sender);
        // 加载对方用户头像
        receiverAvatar = AvatarService.loadUserIcon(receiver);
    }

}
