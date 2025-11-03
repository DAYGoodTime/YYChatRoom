package com.yychat.client.view.chat;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.yychat.client.ClientMain;
import com.yychat.client.service.AvatarService;
import com.yychat.client.service.MessageService;
import com.yychat.client.util.ImageIconUtil;
import com.yychat.common.model.*;
import com.yychat.common.util.StringUtil;
import com.yychat.common.util.ThumbnailGenerator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseChat extends JFrame implements KeyListener {
    protected JButton sendButton = new JButton("发送");
    protected JButton sendFileButton = new JButton("上传文件");
    protected JTextPane messageArea = new JTextPane(); // 使用JTextPane替代JTextArea以支持组件插入
    protected JTextField messageInputField;

    //发送者
    protected User sender;
    protected String chatKey;//用来从map获取窗口的key

    // 文件选择显示面板
    protected JPanel fileSelectionPanel = null;
    //窗口标题
    protected String chatTitle = "默认聊天窗口";
    //已选择的文件
    protected File selectedFile = null;

    protected int chatHistoryIndex = 0;
    protected int chatHistoryPageSize = 20;
    protected long total = 0;
    protected Set<ChatMessage> chatHistory = new HashSet<>();

    public BaseChat(String title, User sender, String chatKey) {
        this.sender = sender;
        this.chatKey = chatKey;
        this.chatTitle = title;
        //初始化UI
        initUI();
        //初始化监听器
        initListener();
    }

    // 抽象方法：由子类实现具体的消息发送逻辑
    protected abstract ServiceResponse<Message> sendTextMessage(String text);

    protected abstract void sendFileMessage(File file, String message);

    protected abstract void loadMessageFromHistory();

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
        messageInputField.addKeyListener(this); // 添加键盘监听器

        sendPanel.add(messageInputField);
        sendPanel.add(sendButton);
        sendPanel.add(sendFileButton);

        this.add(mainPanel, BorderLayout.CENTER);
        this.add(sendPanel, BorderLayout.SOUTH);

        this.setSize(480, 420); // 稍微增加高度以容纳文件选择区域
        this.setLocationRelativeTo(null);
        this.setTitle(chatTitle);
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
        // 发送按钮事件监听 - 处理文本和文件消息发送
        sendButton.addActionListener(e -> {
            String msg = messageInputField.getText();
            messageInputField.setText("");

            // 发送文本消息
            if (selectedFile == null && !msg.trim().isEmpty()) {
                ServiceResponse<Message> response = sendTextMessage(msg);
                if (!response.isSuccess()) {
                    appendErrorMessage("消息发送失败: " + response.getMessage());
                    return;
                }
                appendMessage(response.getData(), false);
                return;
            }

            // 发送文件消息
            if (selectedFile != null) {
                sendFileMessage(selectedFile, msg);
            }
        });
        sendButton.setForeground(Color.blue);
        // 发送文件按钮事件监听
        sendFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
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

    /**
     * 创建文件选择显示面板
     */
    protected JPanel createFileSelectionPanel() {
        fileSelectionPanel = new JPanel(new BorderLayout());
        fileSelectionPanel.setBackground(new Color(240, 248, 255)); // 淡蓝色背景
        fileSelectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        fileSelectionPanel.setPreferredSize(new Dimension(0, 80)); // 固定高度
        fileSelectionPanel.setVisible(false); // 初始时不可见
        return fileSelectionPanel;
    }

    /**
     * 往消息内容区内添加消息
     */
    public void appendMessage(Message message, boolean received) {
        JSONObject json = message.getJson();
        appendMessage(new ChatMessage(
                json.getLong("message_id", -1L),
                message.getSender(),
                message.getReceiver(),
                json,
                message.getTime()
        ), received);
    }

    /**
     * 往消息内容区内添加消息
     */
    public void appendMessage(ChatMessage chatMessage, boolean received) {
        // 创建消息面板：左侧头像 + 右侧内容和时间
        JPanel messagePanel = new JPanel(new BorderLayout(10, 5));
        // 左侧：发送方头像（根据接收状态选择正确的头像）
        JLabel avatarLabel;
        if (received) {
            // 接收的消息：显示发送方的头像
            avatarLabel = createAvatarLabel(AvatarService.loadUserAvatar(chatMessage.getSenderName(), null));
        } else {
            // 自己发送的消息：显示当前用户的头像
            avatarLabel = createAvatarLabel(AvatarService.loadUserAvatar(ClientMain.getCurrentUser()));
        }
        // 创建头像容器面板，确保头像始终在顶部
        JPanel avatarPanel = new JPanel(new BorderLayout());
        avatarPanel.setOpaque(false);
        avatarPanel.add(avatarLabel, BorderLayout.NORTH);
        messagePanel.add(avatarPanel, BorderLayout.WEST);
        // 顶部：时间 + 发送者名称
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        headerPanel.setOpaque(false);
        SimpleDateFormat sdf;
        if (chatMessage.getTime().isBefore(LocalDate.now().atStartOfDay())) {
            sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        } else {
            sdf = new SimpleDateFormat("HH:mm:ss");
        }
        String currentTime = sdf.format(new Date());
        // 时间标签（灰色）
        JLabel timeLabel = new JLabel("[" + currentTime + "]");
        timeLabel.setForeground(Color.GRAY);
        timeLabel.setFont(new Font("微软雅黑", Font.PLAIN, 10));
        // 发送者标签（蓝色）
        JLabel senderLabel;
        if (chatMessage.getSenderName().equals(sender.getUserName())) {
            senderLabel = new JLabel("我");
            senderLabel.setForeground(Color.BLUE);
            senderLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        } else {
            senderLabel = new JLabel(chatMessage.getSenderName());
            senderLabel.setForeground(Color.GREEN);
            senderLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        }

        headerPanel.add(timeLabel);
        headerPanel.add(senderLabel);

        // 消息内容标签
        Component messageLabel;
        JSONObject contentJson = chatMessage.getContent();
        switch (ChatMessageType.fromCode(contentJson.getInt("chat_type", -1))) {
            case UserChatPainText:
            case GroupChatPainText:
                messageLabel = appendTextMessage(contentJson);
                break;
            case UserChatFile:
            case GroupChatFile:
                messageLabel = appendFileMessage(contentJson, received);
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

    /**
     * 返回文字消息组件
     */
    protected Component appendTextMessage(JSONObject contentJson) {
        String result = contentJson.getStr("content", "[错误]未知消息");
        JLabel messageLabel = new JLabel(result);
        messageLabel.setForeground(Color.BLUE);
        messageLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        return messageLabel;
    }

    /**
     * 返回文件消息组件
     * 如果是图片则返回缩略图
     */
    protected Component appendFileMessage(JSONObject contentJson, boolean received) {
        String content = contentJson.getStr("content", "");
        String fileName = contentJson.getStr("file_name", "");
        String fileMd5 = contentJson.getStr("file_md5", "");
        int fileSizeLength = contentJson.getInt("file_size", -1);
        boolean isImage = contentJson.getBool("is_image", false);

        // 创建主面板，垂直布局
        JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.Y_AXIS));
        filePanel.setOpaque(false);
        filePanel.setAlignmentX(LEFT_ALIGNMENT); // 确保整个文件面板左对齐

        // 如果有消息内容，先显示消息内容
        if (!content.isEmpty()) {
            JLabel contentLabel = new JLabel(content);
            contentLabel.setForeground(Color.BLACK);
            contentLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));
            contentLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            contentLabel.setAlignmentX(LEFT_ALIGNMENT); // 确保文字左对齐
            filePanel.add(contentLabel);
        }

        // 如果是图片且有缩略图数据，显示缩略图
        if (isImage && contentJson.containsKey("thumbnail_data")) {
            filePanel.add(createImageThumbnailPanel(contentJson, fileName));
        } else {
            // 普通文件显示
            filePanel.add(createNormalFilePanel(received, fileName, fileMd5, fileSizeLength));
        }

        return filePanel;
    }

    /**
     * 创建图片缩略图显示面板
     */
    protected JPanel createImageThumbnailPanel(JSONObject contentJson, String fileName) {
        // 使用BoxLayout实现左对齐排列
        JPanel imagePanel = new JPanel();
        imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.X_AXIS));
        imagePanel.setBackground(Color.WHITE);

        try {
            // 获取缩略图数据
            String thumbnailBase64 = contentJson.getStr("thumbnail_data");
            int originalWidth = contentJson.getInt("original_width", 0);
            int originalHeight = contentJson.getInt("original_height", 0);
            int thumbnailWidth = contentJson.getInt("thumbnail_width", 0);
            int thumbnailHeight = contentJson.getInt("thumbnail_height", 0);

            if (thumbnailBase64 != null && !thumbnailBase64.isEmpty()) {
                // 解码Base64缩略图数据
                byte[] thumbnailData = Base64.decode(thumbnailBase64);
                ImageIcon thumbnailIcon = ThumbnailGenerator.bytesToImageIcon(thumbnailData);

                if (thumbnailIcon != null) {
                    // 根据缩略图实际尺寸计算合适的内边距
                    int topBottomPadding = Math.max(8, thumbnailHeight / 8);
                    int leftRightPadding = Math.max(8, thumbnailWidth / 8);

                    // 设置动态边框
                    imagePanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(173, 216, 230), 1),
                            BorderFactory.createEmptyBorder(topBottomPadding, leftRightPadding, topBottomPadding, leftRightPadding)
                    ));

                    // 创建缩略图标签（左对齐）
                    JLabel thumbnailLabel = new JLabel(thumbnailIcon);
                    thumbnailLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    thumbnailLabel.setAlignmentX(LEFT_ALIGNMENT); // 确保左对齐

                    // 添加双击事件
                    thumbnailLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mouseClicked(java.awt.event.MouseEvent e) {
                            if (e.getClickCount() == 2) {
                                showImageViewer(contentJson, fileName, originalWidth, originalHeight);
                            }
                        }
                    });

                    // 设置面板左对齐
                    imagePanel.setAlignmentX(LEFT_ALIGNMENT);
                    imagePanel.add(thumbnailLabel);
                } else {
                    // 如果缩略图加载失败，使用默认边框
                    imagePanel.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(173, 216, 230), 1),
                            BorderFactory.createEmptyBorder(10, 10, 10, 10)
                    ));
                }
            } else {
                // 如果没有缩略图数据，使用默认边框
                imagePanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(173, 216, 230), 1),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
            }
        } catch (Exception e) {
            System.out.println("加载缩略图失败: " + e.getMessage());
            // 发生异常时使用默认边框
            imagePanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(173, 216, 230), 1),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)
            ));
        }
        return imagePanel;
    }

    /**
     * 创建普通文件显示面板
     */
    protected JPanel createNormalFilePanel(boolean received, String fileName, String fileMd5, int fileSizeLength) {
        // 使用BoxLayout实现左对齐排列
        JPanel fileInfoPanel = new JPanel();
        fileInfoPanel.setLayout(new BoxLayout(fileInfoPanel, BoxLayout.X_AXIS));
        fileInfoPanel.setBackground(Color.WHITE);
        fileInfoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(173, 216, 230), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        // 文件图标
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

        //注册文件下载逻辑
        downloadButton.addActionListener(e -> handelFileDownload(fileName, fileMd5));

        // 设置左对齐
        fileInfoPanel.setAlignmentX(LEFT_ALIGNMENT);
        fileIconLabel.setAlignmentX(LEFT_ALIGNMENT);
        fileNameLabel.setAlignmentX(LEFT_ALIGNMENT);
        downloadButton.setAlignmentX(LEFT_ALIGNMENT);

        // 组装面板：从左到右排列
        fileInfoPanel.add(fileIconLabel);
        fileInfoPanel.add(Box.createHorizontalStrut(10)); // 文件图标和文件名之间的间距
        fileInfoPanel.add(fileNameLabel);

        // 只有收到的文件消息才显示下载按钮
        if (received) {
            fileInfoPanel.add(Box.createHorizontalStrut(10)); // 文件名和下载按钮之间的间距
            fileInfoPanel.add(downloadButton);
        }

        fileInfoPanel.add(Box.createHorizontalGlue()); // 填充剩余空间

        return fileInfoPanel;
    }

    /**
     * 显示图片查看器
     */
    protected void showImageViewer(JSONObject contentJson, String fileName, int originalWidth, int originalHeight) {
        // 创建一个图片查看器对话框
        JDialog imageDialog = new JDialog(this, "图片查看 - " + fileName, true);
        imageDialog.setLayout(new BorderLayout());
        imageDialog.setSize(Math.min(originalWidth + 100, 800), Math.min(originalHeight + 150, 700));
        imageDialog.setLocationRelativeTo(this);

        // 状态标签（显示下载状态）
        JLabel statusLabel = new JLabel("正在加载原图，请稍候...");
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setFont(new Font("微软雅黑", Font.ITALIC, 11));
        statusLabel.setForeground(Color.BLUE);

        // 创建加载动画面板
        JPanel loadingPanel = createLoadingPanel();
        loadingPanel.setPreferredSize(new Dimension(400, 300));

        // 创建图片显示标签
        JLabel imageLabel = new JLabel();
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);

        // 创建滚动面板（初始显示加载面板）
        JScrollPane scrollPane = new JScrollPane(loadingPanel);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        // 控制按钮面板
        JPanel controlPanel = new JPanel(new FlowLayout());

        // 保存按钮（初始禁用）
        JButton saveButton = new JButton("保存图片");
        saveButton.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        saveButton.setBackground(new Color(34, 139, 34));
        saveButton.setForeground(Color.WHITE);
        saveButton.setBorderPainted(false);
        saveButton.setFocusPainted(false);
        saveButton.setEnabled(false); // 初始时禁用

        JButton closeButton = new JButton("关闭");
        closeButton.setFont(new Font("微软雅黑", Font.PLAIN, 11));
        closeButton.addActionListener(e -> imageDialog.dispose());

        controlPanel.add(saveButton);
        controlPanel.add(closeButton);

        // 立即开始下载原图
        downloadOriginalImage(contentJson, imageLabel, scrollPane, loadingPanel, statusLabel, saveButton, fileName, imageDialog);

        // 保存图片的逻辑
        saveButton.addActionListener(e -> saveCurrentImage(imageLabel, fileName, imageDialog));

        // 组装对话框
        imageDialog.add(scrollPane, BorderLayout.CENTER);
        imageDialog.add(statusLabel, BorderLayout.NORTH);
        imageDialog.add(controlPanel, BorderLayout.SOUTH);

        imageDialog.setVisible(true);
    }

    /**
     * 下载原图（后台执行）
     */
    protected void downloadOriginalImage(JSONObject contentJson, JLabel imageLabel, JScrollPane scrollPane, JPanel loadingPanel,
                                         JLabel statusLabel, JButton saveButton, String fileName, JDialog imageDialog) {
        final BaseChat friendChatInstance = this;
        // 使用SwingWorker在后台线程中下载文件
        SwingWorker<byte[], Void> worker = new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() {
                String fileMd5 = contentJson.getStr("file_md5", "");
                if (fileMd5.isEmpty()) {
                    JOptionPane.showMessageDialog(friendChatInstance, "下载失败", "无法获取文件MD5", JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                ServiceResponse<byte[]> response = MessageService.getInstance().downloadFileFromServer(fileMd5);
                if (!response.isSuccess()) {
                    JOptionPane.showMessageDialog(friendChatInstance, "下载失败", response.getMessage(), JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                return response.getData();
            }

            @Override
            protected void done() {
                try {
                    byte[] originalImageData = get();
                    // 生成原图的ImageIcon
                    ImageIcon originalIcon = ThumbnailGenerator.bytesToImageIcon(originalImageData);

                    if (originalIcon != null) {
                        // 更新UI显示原图
                        imageLabel.setIcon(originalIcon);
                        scrollPane.setViewportView(imageLabel);

                        // 调整对话框大小以适应原图
                        int dialogWidth = Math.min(originalIcon.getIconWidth() + 100, 1000);
                        int dialogHeight = Math.min(originalIcon.getIconHeight() + 150, 800);
                        imageDialog.setSize(dialogWidth, dialogHeight);
                        imageDialog.setLocationRelativeTo(friendChatInstance);

                        statusLabel.setText("原图加载完成 - " + originalIcon.getIconWidth() + " x " + originalIcon.getIconHeight());
                        statusLabel.setForeground(new Color(34, 139, 34)); // 绿色表示成功
                        saveButton.setEnabled(true);

                    } else {
                        throw new Exception("无法加载图片数据");
                    }

                } catch (Exception ex) {
                    // 下载失败，显示错误状态
                    statusLabel.setText("下载失败: " + ex.getMessage());
                    statusLabel.setForeground(Color.RED);

                    // 显示错误对话框
                    int result = JOptionPane.showConfirmDialog(imageDialog,
                            "下载原图失败:\n" + ex.getMessage() + "\n\n是否显示缩略图？",
                            "下载错误",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.ERROR_MESSAGE);

                    if (result == JOptionPane.YES_OPTION) {
                        // 用户选择显示缩略图
                        ImageIcon thumbnailIcon = ThumbnailGenerator.bytesToImageIcon(
                                Base64.decode(contentJson.getStr("thumbnail_data", "")));
                        if (thumbnailIcon != null) {
                            imageLabel.setIcon(thumbnailIcon);
                            scrollPane.setViewportView(imageLabel);
                            statusLabel.setText("显示缩略图");
                            statusLabel.setForeground(Color.ORANGE);
                        }
                    }
                }
            }
        };

        worker.execute();
    }

    /**
     * 创建加载动画面板
     */
    protected JPanel createLoadingPanel() {
        JPanel loadingPanel = new JPanel(new GridBagLayout());
        loadingPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();

        // 加载文本
        JLabel loadingText = new JLabel("正在下载原图，请稍候...");
        loadingText.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        loadingText.setForeground(new Color(100, 149, 237));

        // 简单的加载动画（旋转图标）
        JLabel loadingIcon = new JLabel("⏳");
        loadingIcon.setFont(new Font("微软雅黑", Font.PLAIN, 24));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10);
        loadingPanel.add(loadingIcon, gbc);

        gbc.gridy = 1;
        loadingPanel.add(loadingText, gbc);

        return loadingPanel;
    }

    /**
     * 保存当前显示的图片
     */
    protected void saveCurrentImage(JLabel imageLabel, String fileName, JDialog parentDialog) {
        ImageIcon currentIcon = (ImageIcon) imageLabel.getIcon();
        if (currentIcon == null) {
            JOptionPane.showMessageDialog(parentDialog, "没有可保存的图片", "错误", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 创建文件保存选择器
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存图片");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // 设置默认文件名
        String defaultFileName = fileName;
        if (!defaultFileName.toLowerCase().matches(Constant.IMAGE_REX)) {
            defaultFileName += ".jpg"; // 默认添加.jpg扩展名
        }
        fileChooser.setSelectedFile(new File(defaultFileName));

        // 显示保存对话框
        int result = fileChooser.showSaveDialog(parentDialog);

        if (result == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            try {
                // 将ImageIcon转换为图片并保存
                Image image = currentIcon.getImage();
                BufferedImage bufferedImage = new BufferedImage(
                        image.getWidth(null),
                        image.getHeight(null),
                        BufferedImage.TYPE_INT_RGB);

                Graphics2D g2d = bufferedImage.createGraphics();
                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();

                // 确定文件格式
                String filePath = fileToSave.getAbsolutePath();
                String format = "JPG";
                if (filePath.toLowerCase().endsWith(".png")) {
                    format = "PNG";
                } else if (filePath.toLowerCase().endsWith(".gif")) {
                    format = "GIF";
                } else if (filePath.toLowerCase().endsWith(".bmp")) {
                    format = "BMP";
                }

                // 保存图片
                javax.imageio.ImageIO.write(bufferedImage, format, fileToSave);

                JOptionPane.showMessageDialog(parentDialog,
                        "图片已成功保存到:\n" + fileToSave.getAbsolutePath(),
                        "保存成功",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parentDialog,
                        "保存图片失败:\n" + ex.getMessage(),
                        "保存失败",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    protected void appendChangeLine() {
        // 强制换行，确保每个消息面板独占一行
        try {
            messageArea.getDocument().insertString(messageArea.getDocument().getLength(), "\n", null);
        } catch (Exception e) {
            // 插入换行符失败时使用备选方案
            messageArea.replaceSelection("\n");
        }
    }

    /**
     * 更新文件选择面板显示
     */
    protected void updateFileSelectionPanel() {
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
     * 创建头像标签
     */
    protected JLabel createAvatarLabel(ImageIcon avatar) {
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
     * 清除选择的文件
     */
    protected void clearSelectedFile() {
        selectedFile = null;
        if (fileSelectionPanel != null) {
            fileSelectionPanel.setVisible(false);
            fileSelectionPanel.removeAll();
            fileSelectionPanel.revalidate();
            fileSelectionPanel.repaint();
        }
    }

    protected void appendErrorMessage(String message) {
        JLabel errorLabel = new JLabel(message);
        errorLabel.setForeground(Color.RED);
        errorLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
        messageArea.insertComponent(errorLabel);
        appendChangeLine();
        // 自动滚动到底部
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    protected byte[] cachedDownloadFileBytes = null;

    protected void handelFileDownload(String fileName, String fileMd5) {
        // 这里会实现下载功能
        System.out.println("下载文件: " + fileName + ", MD5: " + fileMd5);
        if (cachedDownloadFileBytes == null) {
            ServiceResponse<byte[]> response = MessageService.getInstance().downloadFileFromServer(fileMd5);
            if (!response.isSuccess()) {
                JOptionPane.showMessageDialog(this, response.getMessage(), "下载文件失败", JOptionPane.ERROR_MESSAGE);
                return;
            }
            cachedDownloadFileBytes = response.getData();
        }

        // 创建文件保存选择器
        JFileChooser saveFileChooser = new JFileChooser();
        saveFileChooser.setDialogTitle("保存文件");
        saveFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // 设置默认文件名
        saveFileChooser.setSelectedFile(new File(fileName));

        // 显示保存对话框
        int userSelection = saveFileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = saveFileChooser.getSelectedFile();

            try {
                // 使用Hutool工具类保存文件
                FileUtil.writeBytes(cachedDownloadFileBytes, fileToSave);

                // 显示保存成功消息
                JOptionPane.showMessageDialog(this,
                        "文件已成功保存到:\n" + fileToSave.getAbsolutePath(),
                        "下载完成",
                        JOptionPane.INFORMATION_MESSAGE);
                cachedDownloadFileBytes = null;
            } catch (Exception e) {
                // 处理保存失败的情况
                JOptionPane.showMessageDialog(this,
                        "保存文件时发生错误:\n" + e.getMessage(),
                        "保存失败",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // 用户取消保存
            System.out.println("用户取消了文件保存操作");
        }
    }

    /**
     * 高亮当前聊天窗口（从最小化状态恢复并激活）
     * <p>
     * 此方法的功能：
     * 1. 如果窗口被最小化，将其恢复正常状态
     * 2. 确保窗口可见
     * 3. 将窗口移到前台
     * 4. 请求窗口获得焦点
     * 5. 让消息输入框获得焦点，方便用户输入
     * <p>
     * 使用场景：
     * - 收到新消息时提醒用户
     * - 需要用户关注此聊天窗口时
     * - 从系统托盘恢复窗口时
     */
    public void highlightChatWindow() {
        // 使用SwingUtilities确保在EDT中执行UI操作
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. 如果窗口被最小化，将其恢复
                if (getState() == JFrame.ICONIFIED) {
                    setState(JFrame.NORMAL);
                }
                // 2. 确保窗口可见
                if (!isVisible()) {
                    setVisible(true);
                }
                // 3. 将窗口移到前台
                toFront();
                // 4. 请求窗口获得焦点
                requestFocus();
                // 5. 如果是聊天窗口，让输入框获得焦点
                if (messageInputField != null) {
                    messageInputField.requestFocusInWindow();
                }
            } catch (Exception e) {
                System.err.println("高亮聊天窗口时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // KeyListener接口实现：处理键盘事件
    @Override
    public void keyTyped(KeyEvent e) {
        // 键入事件，暂不需要处理
    }

    @Override
    public void keyPressed(KeyEvent e) {
        // 按下Enter键时模拟点击发送按钮
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            sendButton.doClick();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // 释放键事件，暂不需要处理
    }

    public void updateChatMessages() {
        // 清空消息显示区域
        messageArea.setText("");
        // 检查是否还有更多消息需要加载
        if (hasMoreMessages()) {
            showLoadMoreButton();
        }
        // 重新显示所有历史消息
        displayChatHistory();


    }

    /**
     * 显示聊天历史消息
     */
    private void displayChatHistory() {
        chatHistory.forEach((chatMessage) -> {
            boolean isReceived = !chatMessage.getSenderName().equals(sender.getUserName());
            appendMessage(chatMessage, isReceived);
        });
    }

    /**
     * 检查是否还有更多消息可以加载
     * 这里可以根据实际需求实现，比如：
     * 1. 通过服务器端查询总消息数
     * 2. 通过本地缓存状态判断
     * 3. 或者通过某个标志位判断
     */
    private boolean hasMoreMessages() {
        // 假设如果当前显示的消息数量达到pageSize，则认为还有更多消息
        // 实际实现中可能需要根据服务器返回的总数量来判断
        return chatHistory.size() >= chatHistoryPageSize && total > chatHistory.size();
    }

    /**
     * 显示"加载更多"按钮
     */
    private void showLoadMoreButton() {
        // 创建带边距的面板来容纳按钮
        JPanel buttonContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonContainer.setBackground(new Color(248, 248, 255)); // 淡紫色背景，区分消息

        JButton loadMoreButton = new JButton("加载更多消息");
        loadMoreButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        loadMoreButton.setBackground(new Color(100, 149, 237));
        loadMoreButton.setForeground(Color.WHITE);
        loadMoreButton.setBorderPainted(false);
        loadMoreButton.setFocusPainted(false);
        loadMoreButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loadMoreButton.setPreferredSize(new Dimension(150, 30));

        // 添加点击事件 - 重新调用loadMessageFromHistory获取更多消息
        loadMoreButton.addActionListener(e -> {
            // 移除加载更多按钮
            buttonContainer.remove(loadMoreButton);
            // 重新加载历史消息（loadMessageFromHistory已经实现了获取逻辑）
            loadMessageFromHistory();
            updateChatMessages();
        });
        buttonContainer.add(loadMoreButton);
        // 在消息区域顶部插入按钮容器
        messageArea.insertComponent(buttonContainer);
        // 在按钮后面添加换行符（重要！）
        appendChangeLine();
    }

    /**
     * 刷新聊天消息（供外部调用）
     */
    public void refreshChatMessages() {
        // 在Swing事件线程中执行
        SwingUtilities.invokeLater(() -> {
            updateChatMessages();
        });
    }
}
