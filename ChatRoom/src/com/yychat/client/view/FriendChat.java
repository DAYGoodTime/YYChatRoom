package com.yychat.client.view;


import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import com.yychat.client.service.AvatarService;
import com.yychat.client.service.MessageService;
import com.yychat.common.model.ServiceResponse;
import com.yychat.client.util.ImageIconUtil;
import com.yychat.common.model.*;
import com.yychat.common.util.StringUtil;
import com.yychat.common.util.ThumbnailGenerator;

import javax.swing.*;
import javax.swing.Box;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
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

            // 使用新的格式化方法显示发送的消息
            messageInputField.setText("");
            // 发送文本消息
            if (selectedFile == null && !msg.trim().isEmpty()) {

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
        // 左侧：发送|接受者者头像（始终显示在顶部）
        JLabel avatarLabel = createAvatarLabel(received ? receiverAvatar : senderAvatar);
        // 创建头像容器面板，确保头像始终在顶部
        JPanel avatarPanel = new JPanel(new BorderLayout());
        avatarPanel.setOpaque(false);
        avatarPanel.add(avatarLabel, BorderLayout.NORTH);
        messagePanel.add(avatarPanel, BorderLayout.WEST);
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
        switch (ChatMessageType.fromCode(message.getJson().getInt("chat_type", -1))) {
            case UserChatPainText:
                messageLabel = appendTextMessage(message);
                break;
            case UserChatFile:
                messageLabel = appendFileMessage(message, received);
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

    private Component appendFileMessage(Message message, boolean received) {
        String content = message.getJson().getStr("content", "");
        String fileName = message.getJson().getStr("file_name", "");
        String fileMd5 = message.getJson().getStr("file_md5", "");
        int fileSizeLength = message.getJson().getInt("file_size", -1);
        boolean isImage = message.getJson().getBool("is_image", false);

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
        if (isImage && message.getJson().containsKey("thumbnail_data")) {
            filePanel.add(createImageThumbnailPanel(message, received, fileName, fileMd5, fileSizeLength));
        } else {
            // 普通文件显示
            filePanel.add(createNormalFilePanel(received, fileName, fileMd5, fileSizeLength));
        }

        return filePanel;
    }

    /**
     * 创建图片缩略图显示面板
     */
    private JPanel createImageThumbnailPanel(Message message, boolean received, String fileName, String fileMd5, int fileSizeLength) {
        // 使用BoxLayout实现左对齐排列
        JPanel imagePanel = new JPanel();
        imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.X_AXIS));
        imagePanel.setBackground(Color.WHITE);

        try {
            // 获取缩略图数据
            String thumbnailBase64 = message.getJson().getStr("thumbnail_data");
            int originalWidth = message.getJson().getInt("original_width", 0);
            int originalHeight = message.getJson().getInt("original_height", 0);
            int thumbnailWidth = message.getJson().getInt("thumbnail_width", 0);
            int thumbnailHeight = message.getJson().getInt("thumbnail_height", 0);

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
                                showImageViewer(message, fileName, originalWidth, originalHeight);
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
    private JPanel createNormalFilePanel(boolean received, String fileName, String fileMd5, int fileSizeLength) {
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
    /**
     * 显示图片查看器（自动下载原图）
     */
    private void showImageViewer(Message message, String fileName, int originalWidth, int originalHeight) {
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
        downloadOriginalImage(message, imageLabel, scrollPane, loadingPanel, statusLabel, saveButton, fileName, imageDialog);

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
    private void downloadOriginalImage(Message message, JLabel imageLabel, JScrollPane scrollPane, JPanel loadingPanel,
                                       JLabel statusLabel, JButton saveButton, String fileName, JDialog imageDialog) {
        final FriendChat friendChatInstance = this;
        // 使用SwingWorker在后台线程中下载文件
        SwingWorker<byte[], Void> worker = new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() {
                String fileMd5 = message.getJson().getStr("file_md5", "");
                if (fileMd5.isEmpty()) {
                    JOptionPane.showMessageDialog(friendChatInstance,"下载失败","无法获取文件MD5",JOptionPane.ERROR_MESSAGE);
                    return null;
                }
                ServiceResponse<byte[]> response = MessageService.getInstance().downloadFileFromServer(fileMd5);
                if (!response.isSuccess()) {
                    JOptionPane.showMessageDialog(friendChatInstance,"下载失败",response.getMessage(),JOptionPane.ERROR_MESSAGE);
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
                        imageDialog.setLocationRelativeTo(FriendChat.this);

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
                                Base64.decode(message.getJson().getStr("thumbnail_data", "")));
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
    private JPanel createLoadingPanel() {
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
    private void saveCurrentImage(JLabel imageLabel, String fileName, JDialog parentDialog) {
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
        if (!defaultFileName.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp)$")) {
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
     * 发送文件
     */
    private void sendSelectedFile(String msg) {
        if (selectedFile == null) return;
        try {
            ServiceResponse<Message> response = MessageService.getInstance().sendFileMessageToUser(
                    sender, receiver, msg, FileUtil.readBytes(selectedFile), selectedFile.getName());
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

    private byte[] cachedDownloadFileBytes = null;

    private void handelFileDownload(String fileName, String fileMd5) {
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
     *
     * 此方法的功能：
     * 1. 如果窗口被最小化，将其恢复正常状态
     * 2. 确保窗口可见
     * 3. 将窗口移到前台
     * 4. 请求窗口获得焦点
     * 5. 让消息输入框获得焦点，方便用户输入
     *
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
}
