package com.yychat.view;

import com.yychat.control.YYchatClientConnectionUDP;
import com.yychat.model.Message;
import com.yychat.model.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 头像选择器对话框
 * 支持从默认头像中选择和上传自定义头像
 */
public class AvatarSelector extends JDialog {
    private String selectedAvatarPath = "0.jpg"; // 默认选中的头像
    private JLabel previewLabel;
    private JButton confirmButton;
    private JButton cancelButton;
    private JButton uploadButton;
    private JPanel defaultAvatarsPanel;
    private String userName;
    private FriendList friendList;
    private YYchatClientConnectionUDP clientConnection;
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    // 默认头像文件名
    private static final String[] DEFAULT_AVATARS = {
            "0.jpg", "1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg"
    };

    public AvatarSelector(Frame parent, String userName, FriendList friendList, YYchatClientConnectionUDP clientConnection) {
        super(parent, "选择头像", true);
        this.userName = userName;
        this.friendList = friendList;
        this.clientConnection = clientConnection;

        initializeComponents();
        layoutComponents();
        setupEventListeners();
    }

    /**
     * 初始化组件
     */
    private void initializeComponents() {
        setSize(500, 400);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // 创建预览标签
        previewLabel = new JLabel();
        previewLabel.setPreferredSize(new Dimension(80, 80));
        previewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        previewLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 创建默认头像面板
        defaultAvatarsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        loadDefaultAvatars();

        // 创建按钮
        confirmButton = new JButton("确定");
        cancelButton = new JButton("取消");
        uploadButton = new JButton("上传自定义头像");

        // 设置初始预览
        updatePreview(selectedAvatarPath);
    }

    /**
     * 布局组件
     */
    private void layoutComponents() {
        setLayout(new BorderLayout());

        // 顶部：标题和预览
        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel titlePanel = new JPanel();
        titlePanel.add(new JLabel("请选择您的头像:", SwingConstants.CENTER));

        JPanel previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel previewTitleLabel = new JLabel("预览:");
        previewTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewPanel.add(previewTitleLabel, BorderLayout.NORTH);
        previewPanel.add(previewLabel, BorderLayout.CENTER);

        topPanel.add(titlePanel, BorderLayout.NORTH);
        topPanel.add(previewPanel, BorderLayout.CENTER);

        // 中间：默认头像网格
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        JLabel defaultAvatarsLabel = new JLabel("默认头像:");
        defaultAvatarsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        centerPanel.add(defaultAvatarsLabel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(defaultAvatarsPanel), BorderLayout.CENTER);

        // 底部：按钮
        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.add(uploadButton);
        bottomPanel.add(Box.createHorizontalStrut(50)); // 间距
        bottomPanel.add(confirmButton);
        bottomPanel.add(cancelButton);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 加载默认头像
     */
    private void loadDefaultAvatars() {
        for (int i = 0; i < DEFAULT_AVATARS.length; i++) {
            final String avatarPath = DEFAULT_AVATARS[i];
            JPanel avatarPanel = new JPanel(new BorderLayout());
            avatarPanel.setBorder(BorderFactory.createLineBorder(
                    avatarPath.equals(selectedAvatarPath) ? Color.BLUE : Color.GRAY));

            try {
                ImageIcon icon = new ImageIcon("res/" + avatarPath);
                // 缩放头像到合适大小
                Image scaledImage = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledImage);

                JLabel avatarLabel = new JLabel(scaledIcon);
                avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
                avatarPanel.add(avatarLabel, BorderLayout.CENTER);

                // 添加点击事件
                avatarPanel.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        selectAvatar(avatarPath);
                    }

                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        avatarPanel.setBorder(BorderFactory.createLineBorder(Color.ORANGE));
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        avatarPanel.setBorder(BorderFactory.createLineBorder(
                                avatarPath.equals(selectedAvatarPath) ? Color.BLUE : Color.GRAY));
                    }
                });

                defaultAvatarsPanel.add(avatarPanel);

            } catch (Exception e) {
                System.err.println("无法加载默认头像: " + avatarPath);
                // 创建占位符
                JLabel placeholder = new JLabel("图片\n" + (i + 1));
                placeholder.setHorizontalAlignment(SwingConstants.CENTER);
                placeholder.setPreferredSize(new Dimension(60, 60));
                placeholder.setBorder(BorderFactory.createLineBorder(Color.GRAY));
                defaultAvatarsPanel.add(placeholder);
            }
        }
    }

    /**
     * 选择头像
     */
    private void selectAvatar(String avatarPath) {
        selectedAvatarPath = avatarPath;
        updatePreview(avatarPath);

        // 更新面板边框
        updateAvatarPanelBorders();
    }

    /**
     * 更新头像面板边框
     */
    private void updateAvatarPanelBorders() {
        Component[] components = defaultAvatarsPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof JPanel) {
                JPanel panel = (JPanel) components[i];
                String avatarPath = DEFAULT_AVATARS[i];
                panel.setBorder(BorderFactory.createLineBorder(
                        avatarPath.equals(selectedAvatarPath) ? Color.BLUE : Color.GRAY));
            }
        }
    }

    /**
     * 更新预览
     */
    private void updatePreview(String avatarPath) {
        try {
            ImageIcon icon;
            // 根据路径类型确定完整路径
            if (avatarPath.startsWith("avatars/")) {
                // 自定义头像：使用完整路径
                icon = new ImageIcon(avatarPath);
            } else {
                // 默认头像：使用res目录
                icon = new ImageIcon("res/" + avatarPath);
            }
            // 缩放头像到预览大小
            Image scaledImage = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            previewLabel.setIcon(new ImageIcon(scaledImage));
        } catch (Exception e) {
            previewLabel.setText("预览\n失败");
            System.err.println("无法显示头像预览: " + avatarPath);
        }
    }

    /**
     * 设置事件监听器
     */
    private void setupEventListeners() {
        confirmButton.addActionListener(e -> onConfirm());

        cancelButton.addActionListener(e -> onCancel());

        uploadButton.addActionListener(e -> onUploadAvatar());
    }

    /**
     * 确认选择
     */
    private void onConfirm() {
        // 发送头像更新消息到服务器
        sendAvatarUpdateToServer();
        dispose();
    }

    /**
     * 取消选择
     */
    private void onCancel() {
        dispose();
    }

    /**
     * 上传自定义头像
     */
    private void onUploadAvatar() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "图片文件 (*.jpg, *.jpeg, *.png, *.gif)", "jpg", "jpeg", "png", "gif"));

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName().toLowerCase();

            // 验证文件格式
            if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") &&
                    !fileName.endsWith(".png") && !fileName.endsWith(".gif")) {
                JOptionPane.showMessageDialog(this,
                        "仅支持 JPG、PNG、GIF 格式的图片文件！",
                        "格式错误",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 验证文件大小（假设限制为5MB）
            if (selectedFile.length() > MAX_FILE_SIZE) {
                JOptionPane.showMessageDialog(this,
                        "图片文件大小不能超过 5MB！",
                        "文件过大",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                // 创建avatars目录（如果不存在）
                File avatarsDir = new File("avatars");
                if (!avatarsDir.exists()) {
                    avatarsDir.mkdirs();
                }

                // 生成唯一的头像文件名
                String uniqueFileName = "custom_" + System.currentTimeMillis() + "_" + fileName;
                File targetFile = new File(avatarsDir, uniqueFileName);

                // 复制文件到avatars目录
                copyFile(selectedFile, targetFile);

                // 设置头像路径（相对路径）
                selectedAvatarPath = "avatars/" + uniqueFileName;
                updatePreview(selectedAvatarPath);

                JOptionPane.showMessageDialog(this,
                        "头像上传成功！",
                        "成功",
                        JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "头像上传失败：" + e.getMessage(),
                        "上传失败",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 发送头像更新到服务器
     */
    private void sendAvatarUpdateToServer() {
        try {
            Message message = new Message();
            message.setMessageType(MessageType.UPDATE_AVATAR);
            message.setSender(userName);
            message.setContent(selectedAvatarPath);
            message.setReceiver("Server");

            // 如果是自定义头像（avatars/开头），读取并发送头像文件数据
            if (selectedAvatarPath.startsWith("avatars/")) {
                File avatarFile = new File(selectedAvatarPath);
                if (avatarFile.exists()) {
                    byte[] avatarData = readFileAsBytes(avatarFile);
                    if (avatarData != null && avatarData.length > 0) {
                        message.setAvatarData(avatarData);
                        message.setAvatarFileName(avatarFile.getName());
                        System.out.println("用户 " + userName + " 准备上传自定义头像: " + avatarFile.getName() + " (" + avatarData.length + " bytes)");
                    }
                }
            }

            // 通过客户端连接发送消息到服务器
            if (clientConnection != null) {
                clientConnection.sendChatMessage(message);
                System.out.println("用户 " + userName + " 更新头像: " + selectedAvatarPath);

                // 如果有FriendList实例，更新本地显示
                if (friendList != null) {
                    friendList.updateFriendAvatar(userName, selectedAvatarPath);
                }
            } else {
                throw new Exception("客户端连接为空");
            }

        } catch (Exception e) {
            System.err.println("发送头像更新失败: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "头像更新失败，请稍后重试。",
                    "更新失败",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 读取文件为字节数组
     */
    private byte[] readFileAsBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return data;
        }
    }

    /**
     * 获取选中的头像路径
     */
    public String getSelectedAvatarPath() {
        return selectedAvatarPath;
    }

    /**
     * 显示对话框并返回选择结果
     */
    public String showDialogAndGetResult() {
        setVisible(true);
        return selectedAvatarPath;
    }

    /**
     * 复制文件
     */
    private void copyFile(File source, File target) throws IOException {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(target)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
    }
}