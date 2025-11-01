package com.yychat.client.view;


import cn.hutool.core.io.FileUtil;
import com.yychat.client.service.AvatarService;
import com.yychat.client.service.MessageService;
import com.yychat.common.model.ServiceResponse;
import com.yychat.client.util.ImageIconUtil;
import com.yychat.common.model.*;
import com.yychat.common.util.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FriendChat extends JFrame implements KeyListener {
    protected JButton sendButton = new JButton("发送");
    protected JButton sendFileButton = new JButton("上传文件");
    protected JTextPane messageArea = new JTextPane(); // 使用JTextPane替代JTextArea以支持组件插入
    protected JTextField messageInputField;

    protected User sender;
    protected User receiver;

    // 头像相关字段
    private ImageIcon senderAvatar = null; // 发送者头像（当前用户）
    private ImageIcon receiverAvatar = null; // 接收者头像（对方用户）

    // 文件选择器
    private JFileChooser fileChooser = new JFileChooser();
    // 文件选择显示面板
    private JPanel fileSelectionPanel = null;
    // 当前选择的文件
    private File selectedFile = null;

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

        // 创建主面板，使用BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 创建中心面板，包含聊天区域
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        centerPanel.add(createFileSelectionPanel(), BorderLayout.SOUTH); // 文件选择面板在聊天区域下方

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // 创建底部按钮面板
        JPanel sendPanel = new JPanel();
        messageInputField = new JTextField(15);
        messageInputField.addKeyListener(this);

        sendPanel.add(messageInputField);
        sendPanel.add(sendButton);
        sendPanel.add(sendFileButton);

        this.add(mainPanel, BorderLayout.CENTER);
        this.add(sendPanel, BorderLayout.SOUTH);

        this.setSize(480, 420); // 稍微增加高度以容纳文件选择区域
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
            messageInputField.setText("");
            // 发送文本消息
            if (selectedFile == null) {
                ServiceResponse<Message> response = MessageService.getInstance()
                        .sendPlainTextMessageToUser(sender, receiver, msg);
                if (!response.isSuccess()) {
                    // 错误消息使用红色标签显示
                    appendErrorMessage("消息发送失败: " + response.getMessage());
                    return;
                }
                appendSendMessage(response.getData(), false);
                return;
            }
            // 发送附带文件消息
            sendSelectedFile(msg);
        });
        sendButton.setForeground(Color.blue);

        // 发送文件按钮事件监听
        sendFileButton.addActionListener(e -> {
            // 设置文件选择器为只选择文件（不是目录）
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            // 显示文件选择对话框
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                // 用户选择了文件
                selectedFile = fileChooser.getSelectedFile();
                // 在文件选择面板中显示文件信息
                updateFileSelectionPanel();
            }
        });
    }

    public void appendSendMessage(Message message, boolean received) {
        // 创建消息面板：左侧头像 + 右侧内容和时间
        JPanel messagePanel = new JPanel(new BorderLayout(10, 5));
        // 左侧：发送者头像
        JLabel avatarLabel = createAvatarLabel(senderAvatar);
        messagePanel.add(avatarLabel, BorderLayout.WEST);
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
        JLabel senderLabel;
        if (received) {
            senderLabel = new JLabel(message.getSender());
            senderLabel.setForeground(Color.GREEN);
            senderLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        } else {
            senderLabel = new JLabel("我");
            senderLabel.setForeground(Color.BLUE);
            senderLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        }

        headerPanel.add(timeLabel);
        headerPanel.add(senderLabel);

        // 消息内容标签
        Component messageLabel;
        switch (ChatMessageType.fromCode(message.getJson().getInt("chat_type",-1))){
            case UserChatPainText:
                messageLabel = appendTextMessage(message);
                break;
            case UserChatFile:
                messageLabel = appendFileMessage(message);
                break;
            case UnSupport:
            default:
                messageLabel = new JLabel("不支持的消息类型");
                messageLabel.setForeground(Color.RED);
                messageLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
                break;
        }

        // 右侧：时间和消息内容
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);

        rightPanel.add(headerPanel, BorderLayout.NORTH);
        rightPanel.add(messageLabel, BorderLayout.CENTER);

        messagePanel.add(rightPanel, BorderLayout.CENTER);

        // 添加到文本区域（使用更安全的方式）
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
        messageArea.insertComponent(messagePanel);

        appendChangeLine();

        // 自动滚动到底部
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    private Component appendTextMessage(Message message) {
        String result;
        if (!message.isJsonMessage()) result = "[错误]未知消息";
        else result = message.getJson().getStr("content", "[错误]未知消息");
        JLabel messageLabel = new JLabel(result);
        messageLabel.setForeground(Color.BLUE);
        messageLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        return messageLabel;
    }
    private Component appendFileMessage(Message message) {
        String content = message.getJson().getStr("content","");
        String fileName = message.getJson().getStr("file_name","");
        String fileMd5 = message.getJson().getStr("file_md5","");
        int fileSizeLength = message.getJson().getInt("file_size",-1);
        // 创建主面板，垂直布局
        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
        filePanel.setOpaque(false);

        // 如果有消息内容，先显示消息内容
        if (!content.isEmpty()) {
            JLabel contentLabel = new JLabel(content);
            contentLabel.setForeground(Color.BLUE);
            contentLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            contentLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            filePanel.add(contentLabel);
        }

        // 创建文件信息显示面板
        JPanel fileInfoPanel = new JPanel(new BorderLayout(10, 0));
        fileInfoPanel.setBackground(Color.WHITE);
        fileInfoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(173, 216, 230), 1),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        // 左侧：文件图标
        JLabel fileIconLabel = new JLabel();
        try {
            // 加载文件图标
            ImageIcon fileIcon = new ImageIcon(Constant.UNKNOW_FILE_PATH);
            // 调整图标大小
            Image scaledImage = fileIcon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
            fileIconLabel.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
            // 如果图标加载失败，显示文字
            fileIconLabel.setText("文件");
            fileIconLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
            fileIconLabel.setForeground(new Color(100, 149, 237));
            fileIconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            fileIconLabel.setPreferredSize(new Dimension(32, 32));
            fileIconLabel.setBorder(BorderFactory.createLineBorder(new Color(173, 216, 230)));
        }

        // 右侧：文件名和下载按钮
        JPanel rightPanel = new JPanel(new BorderLayout(5, 0));
        rightPanel.setOpaque(false);

        // 文件名标签
        String fileSize = "(" + StringUtil.formatFileSize(fileSizeLength) + ")";
        JLabel fileNameLabel = new JLabel(fileName + fileSize);
        fileNameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        fileNameLabel.setForeground(new Color(70, 130, 180));

        // 下载按钮
        JButton downloadButton = new JButton("下载");
        downloadButton.setFont(new Font("微软雅黑", Font.PLAIN, 10));
        downloadButton.setPreferredSize(new Dimension(60, 25));
        downloadButton.setBackground(new Color(100, 149, 237));
        downloadButton.setForeground(Color.WHITE);
        downloadButton.setBorderPainted(false);
        downloadButton.setFocusPainted(false);

        // TODO: 稍后实现下载功能
        downloadButton.addActionListener(e -> {
            // 这里会实现下载功能
            System.out.println("下载文件: " + fileName + ", MD5: " + fileMd5);
        });

        // 组装右侧面板
        rightPanel.add(fileNameLabel, BorderLayout.CENTER);
        rightPanel.add(downloadButton, BorderLayout.EAST);

        // 组装文件信息面板
        fileInfoPanel.add(fileIconLabel, BorderLayout.WEST);
        fileInfoPanel.add(rightPanel, BorderLayout.CENTER);

        filePanel.add(fileInfoPanel);

        return filePanel;
    }

    private void appendChangeLine() {
        // 强制换行，确保每个消息面板独占一行
        try {
            messageArea.getDocument().insertString(messageArea.getDocument().getLength(), "\n", null);
        } catch (Exception e) {
            // 插入换行符失败时使用备选方案
            messageArea.replaceSelection("\n");
        }
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

    /**
     * 创建文件选择显示面板
     */
    private JPanel createFileSelectionPanel() {
        fileSelectionPanel = new JPanel(new BorderLayout());
        fileSelectionPanel.setBackground(new Color(240, 248, 255)); // 淡蓝色背景
        fileSelectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        fileSelectionPanel.setPreferredSize(new Dimension(0, 80)); // 固定高度
        fileSelectionPanel.setVisible(false); // 初始时不可见
        return fileSelectionPanel;
    }

    /**
     * 更新文件选择面板显示
     */
    private void updateFileSelectionPanel() {
        if (selectedFile != null) {
            // 清空面板
            fileSelectionPanel.removeAll();

            // 创建文件信息显示
            JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            infoPanel.setOpaque(false);

            // 文件图标（使用文字代替emoji，更好的兼容性）
            JLabel fileIconLabel = new JLabel("文件");
            fileIconLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
            fileIconLabel.setForeground(new Color(100, 149, 237)); // 矢车菊蓝色
            fileIconLabel.setBorder(BorderFactory.createLineBorder(new Color(173, 216, 230), 1)); // 淡蓝色边框

            // 文件名和大小
            String fileSize = StringUtil.formatFileSize(selectedFile.length());

            JLabel fileInfoLabel = new JLabel(selectedFile.getName() + " (" + fileSize + ")");
            fileInfoLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            fileInfoLabel.setForeground(new Color(70, 130, 180)); // 钢蓝色

            // 移除文件按钮
            JButton removeFileButton = new JButton("移除");
            removeFileButton.setFont(new Font("微软雅黑", Font.PLAIN, 10));
            removeFileButton.setPreferredSize(new Dimension(60, 25));
            removeFileButton.addActionListener(e -> {
                clearSelectedFile();
            });

            infoPanel.add(fileIconLabel);
            infoPanel.add(fileInfoLabel);
            infoPanel.add(removeFileButton);

            fileSelectionPanel.add(infoPanel, BorderLayout.CENTER);
            fileSelectionPanel.setVisible(true);
            fileSelectionPanel.revalidate();
            fileSelectionPanel.repaint();
        }
    }

    /**
     * 清除选择的文件
     */
    private void clearSelectedFile() {
        selectedFile = null;
        if (fileSelectionPanel != null) {
            fileSelectionPanel.setVisible(false);
            fileSelectionPanel.removeAll();
            fileSelectionPanel.revalidate();
            fileSelectionPanel.repaint();
        }
    }

    private void appendErrorMessage(String message) {
        JLabel errorLabel = new JLabel(message);
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
        messageArea.insertComponent(errorLabel);
        appendChangeLine();
        // 自动滚动到底部
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    /**
     * 发送文件（当前只模拟发送过程）
     */
    private void sendSelectedFile(String msg) {
        if (selectedFile == null) return;
        try {
            ServiceResponse<Message> response = MessageService.getInstance().sendFileMessageToUser(
                    sender, receiver, msg, FileUtil.readBytes(selectedFile), selectedFile.getName());
            if(!response.isSuccess()) {
                appendErrorMessage("文件发送失败: " +response.getMessage());
                return;
            }
            Message responseMessage = response.getData();
            appendSendMessage(responseMessage,false);
            // 清除文件选择
            clearSelectedFile();
        } catch (Exception e) {
            // 显示发送失败消息
            appendErrorMessage("文件发送失败: " + e.getMessage());
        }
    }
}
