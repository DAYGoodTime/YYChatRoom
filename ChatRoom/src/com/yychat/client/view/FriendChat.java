package com.yychat.client.view;


import com.yychat.client.service.AvatarService;
import com.yychat.client.service.MessageService;
import com.yychat.common.model.ServiceResponse;
import com.yychat.client.util.ImageIconUtil;
import com.yychat.common.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FriendChat extends JFrame implements KeyListener {
    protected JButton sendButton = new JButton("发送");
    protected JTextPane messageArea = new JTextPane(); // 使用JTextPane替代JTextArea以支持组件插入
    protected JTextField messageInputField;

    protected User sender;
    protected User receiver;

    // 头像相关字段
    private ImageIcon senderAvatar = null; // 发送者头像（当前用户）
    private ImageIcon receiverAvatar = null; // 接收者头像（对方用户）

    public FriendChat(User sender, User receiver) {
        this.sender = sender;
        this.receiver = receiver;
        //初始化UI
        initUI();
        // 加载头像
        loadAvatars();
        //初始化监听器
        initListener();
    }

    //初始化UI
    public void initUI() {
        // 设置文本区域为支持多色显示和组件插入
        messageArea.setEditable(false);
        messageArea.setBackground(Color.WHITE);
        messageArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        messageArea.setContentType("text/html"); // 支持HTML样式
        JScrollPane scrollPane = new JScrollPane(messageArea);
        this.add(scrollPane, BorderLayout.CENTER);

        messageInputField = new JTextField(15);
        messageInputField.addKeyListener(this);

        JPanel sendPanel = new JPanel();
        sendPanel.add(messageInputField);
        sendPanel.add(sendButton);
        this.add(sendPanel, BorderLayout.SOUTH);

        this.setSize(480, 360);
        this.setLocationRelativeTo(null);
        this.setTitle("与 " + receiver.getUserName() + " 的聊天界面");
        this.setIconImage(ImageIconUtil.getWindowIcon().getImage());

        // 在setVisible(true)之前添加
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setResizable(true);

        // 优化文本区域滚动
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.setVisible(true);
    }

    //初始化监听器
    public void initListener() {
        sendButton.addActionListener(e -> {
            String msg = messageInputField.getText();
            if (msg.trim().isEmpty()) return;

            // 使用新的格式化方法显示发送的消息
            appendSendMessage(msg, sender.getUserName());
            messageInputField.setText("");
            ServiceResponse<?> response = MessageService.getInstance()
                    .sendPlainTextMessageToUser(sender, receiver, msg);
            if (!response.isSuccess()) {
                // 错误消息使用红色标签显示
                JLabel errorLabel = new JLabel("消息发送失败: " + response.getMessage());
                errorLabel.setForeground(Color.RED);
                messageArea.setCaretPosition(messageArea.getDocument().getLength());
                messageArea.insertComponent(errorLabel);
                messageArea.replaceSelection("\n");
            }
        });
        sendButton.setForeground(Color.blue);
    }

    /**
     * 格式化发送消息显示
     *
     * @param message 消息内容
     * @param sender  发送者
     */
    private void appendSendMessage(String message, String sender) {
        // 创建消息面板：左侧头像 + 右侧内容和时间
        JPanel messagePanel = new JPanel(new BorderLayout(10, 5));

        // 左侧：发送者头像
        JLabel avatarLabel = createAvatarLabel(senderAvatar);
        messagePanel.add(avatarLabel, BorderLayout.WEST);

        // 右侧：时间和消息内容
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);

        // 顶部：时间 + 发送者名称
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setOpaque(false);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // 时间标签（灰色）
        JLabel timeLabel = new JLabel("[" + currentTime + "]");
        timeLabel.setForeground(Color.GRAY);
        timeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 10));

        // 发送者标签（蓝色）
        JLabel senderLabel = new JLabel("我");
        senderLabel.setForeground(Color.BLUE);
        senderLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        headerPanel.add(timeLabel);
        headerPanel.add(senderLabel);

        // 消息内容标签（蓝色）
        JLabel messageLabel = new JLabel(message);
        messageLabel.setForeground(Color.BLUE);
        messageLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        rightPanel.add(headerPanel, BorderLayout.NORTH);
        rightPanel.add(messageLabel, BorderLayout.CENTER);

        messagePanel.add(rightPanel, BorderLayout.CENTER);

        // 添加到文本区域（使用更安全的方式）
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
        messageArea.insertComponent(messagePanel);

        // 添加分隔符
        messageArea.replaceSelection("\n");

        // 自动滚动到底部
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    /**
     * 格式化接收消息显示
     *
     * @param message 消息对象
     */
    private void appendReceiveMessage(Message message) {
        // 创建消息面板：左侧头像 + 右侧内容和时间
        JPanel messagePanel = new JPanel(new BorderLayout(10, 5));

        // 左侧：发送者头像
        JLabel avatarLabel = createAvatarLabel(receiverAvatar);
        messagePanel.add(avatarLabel, BorderLayout.WEST);

        // 右侧：时间和消息内容
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);

        // 顶部：时间 + 发送者名称
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setOpaque(false);

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // 时间标签（灰色）
        JLabel timeLabel = new JLabel("[" + currentTime + "]");
        timeLabel.setForeground(Color.GRAY);
        timeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 10));

        // 发送者标签（绿色）
        JLabel senderLabel = new JLabel(message.getSender());
        senderLabel.setForeground(Color.GREEN);
        senderLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        headerPanel.add(timeLabel);
        headerPanel.add(senderLabel);

        // 消息内容标签（黑色）
        JLabel messageLabel = new JLabel(message.getJson().getStr("content"));
        messageLabel.setForeground(Color.BLACK);
        messageLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        rightPanel.add(headerPanel, BorderLayout.NORTH);
        rightPanel.add(messageLabel, BorderLayout.CENTER);

        messagePanel.add(rightPanel, BorderLayout.CENTER);

        // 添加到文本区域（使用更安全的方式）
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
        messageArea.insertComponent(messagePanel);

        // 添加分隔符
        messageArea.replaceSelection("\n");

        // 自动滚动到底部
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            sendButton.doClick();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    public void append(Message message) {
        appendReceiveMessage(message);
    }

    /**
     * 加载发送者和接收者的头像
     */
    private void loadAvatars() {
        AvatarService avatarService = AvatarService.getInstance();
        // 加载当前用户头像
        senderAvatar = avatarService.loadUserAvatar(sender.getUserName(), sender.getAvatarPath());
        // 加载对方用户头像
        receiverAvatar = avatarService.loadUserAvatar(receiver.getUserName(), receiver.getAvatarPath());
    }

    /**
     * 创建头像标签
     */
    private JLabel createAvatarLabel(ImageIcon avatar) {
        JLabel avatarLabel = new JLabel();
        if (avatar != null) {
            avatarLabel.setIcon(avatar);
        } else {
            // 如果头像加载失败，显示占位符
            avatarLabel.setText("头像");
            avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
            avatarLabel.setPreferredSize(new Dimension(AvatarService.AVATAR_SIZE, AvatarService.AVATAR_SIZE));
            avatarLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            avatarLabel.setBackground(Color.LIGHT_GRAY);
            avatarLabel.setOpaque(true);
        }
        return avatarLabel;
    }
}
