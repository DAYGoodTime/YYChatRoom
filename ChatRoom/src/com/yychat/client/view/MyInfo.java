package com.yychat.client.view;

import com.yychat.client.ClientMain;
import com.yychat.client.service.AvatarService;
import com.yychat.common.model.Constant;
import com.yychat.common.model.User;

import javax.swing.*;
import java.awt.*;

/**
 * 用户信息窗口
 * 显示用户基本信息并支持头像上传
 */
public class MyInfo extends JFrame {
    private String avatarPath;
    private JLabel avatarLabel;
    private JLabel userNameLabel;
    private JButton uploadAvatarButton;
    private final FriendList friendList;
    protected JTextArea infoTextArea;

    public MyInfo(FriendList friendList) {
        this.friendList = friendList;
        initializeComponents();
        layoutComponents();
        setupEventListeners();
        loadUserInfo();
    }

    /**
     * 初始化组件
     */
    private void initializeComponents() {
        // 设置窗口标题
        setTitle("我的信息 - " + ClientMain.getCurrentUserName());

        // 设置窗口大小和属性
        setSize(540, 360);
        setLocationRelativeTo(null); // 居中显示
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // 创建头像标签
        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(120, 120));
        avatarLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 创建用户名标签
        userNameLabel = new JLabel(ClientMain.getCurrentUserName(), SwingConstants.CENTER);
        userNameLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));

        // 创建上传头像按钮
        uploadAvatarButton = new JButton("更换头像");
        uploadAvatarButton.setPreferredSize(new Dimension(120, 30));

        // 加载用户头像
        updateAvatar();
    }

    /**
     * 布局组件
     */
    private void layoutComponents() {
        setLayout(new BorderLayout());

        // 顶部：用户信息面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JPanel userInfoPanel = new JPanel(new BorderLayout());
        userInfoPanel.add(avatarLabel, BorderLayout.CENTER);

        JPanel namePanel = new JPanel(new BorderLayout());
        namePanel.add(userNameLabel, BorderLayout.NORTH);
        namePanel.add(uploadAvatarButton, BorderLayout.SOUTH);

        // 左侧：头像和用户名
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 10));
        leftPanel.add(userInfoPanel, BorderLayout.NORTH);
        leftPanel.add(namePanel, BorderLayout.CENTER);

        // 右侧：用户详细信息
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 20));

        JLabel infoTitleLabel = new JLabel("用户信息");
        infoTitleLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));

        infoTextArea = new JTextArea();
        infoTextArea.setEditable(false);
        infoTextArea.setBackground(getBackground());
        infoTextArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        infoTextArea.setText("用户名: " + ClientMain.getCurrentUserName() + "\n" +
                "状态: 在线\n" +
                "头像: " + avatarPath + "\n" +
                "登录方式: UDP");

        JScrollPane scrollPane = new JScrollPane(infoTextArea);
        scrollPane.setPreferredSize(new Dimension(200, 120));

        rightPanel.add(infoTitleLabel);
        rightPanel.add(Box.createVerticalStrut(10));
        rightPanel.add(scrollPane);

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

        // 底部：关闭按钮
        JPanel bottomPanel = new JPanel();
        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(e -> dispose());
        bottomPanel.add(closeButton);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * 设置事件监听器
     */
    private void setupEventListeners() {
        uploadAvatarButton.addActionListener(e -> openAvatarSelector());

        // 双击头像也可以更换
        avatarLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openAvatarSelector();
                }
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                uploadAvatarButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        });
    }

    /**
     * 加载用户信息
     */
    private void loadUserInfo() {
        try {
            // 从ClientMain中获取当前用户信息
            User currentUser = ClientMain.getCurrentUser();
            if (currentUser != null) {
                updateUserInfo();
            }
        } catch (Exception e) {
            System.err.println("加载用户信息时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 打开头像选择器
     */
    private void openAvatarSelector() {
        try {
            AvatarSelector avatarSelector = new AvatarSelector(this);
            String selectedAvatar = avatarSelector.showDialogAndGetResult();

            if (selectedAvatar != null && !selectedAvatar.isEmpty()) {
                avatarPath = selectedAvatar;
                updateAvatar();
                updateUserInfo();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "打开头像选择器失败：" + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 更新头像显示
     */
    private void updateAvatar() {
        try {
            User currentUser = ClientMain.getCurrentUser();
            avatarPath = currentUser.getAvatarPath();
            ImageIcon icon = AvatarService.getInstance().loadUserAvatar(currentUser.getUserName(), avatarPath);
            // 缩放头像到合适大小
            Image scaledImage = icon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
            avatarLabel.setIcon(new ImageIcon(scaledImage));
            avatarLabel.setText(""); // 清除可能存在的文本
        } catch (Exception e) {
            avatarLabel.setIcon(new ImageIcon(Constant.DEFAULT_AVATAR_PATH + Constant.DEFAULT_AVATAR));
            avatarLabel.setText("头像\n加载失败");
            System.err.println("[MyInfo]无法加载头像: " + avatarPath);
        }
    }

    /**
     * 更新用户信息显示
     */
    private void updateUserInfo() {
        User currentUser = ClientMain.getCurrentUser();
        if (currentUser != null) {
            currentUser.setAvatarPath(avatarPath);
        }
        // 如果有FriendList实例，更新其中的头像显示
        if (friendList != null) {
            friendList.updateFriendAvatar(ClientMain.getCurrentUserName(), avatarPath);
        }
        infoTextArea.setText("用户名: " + ClientMain.getCurrentUserName() + "\n" +
                "状态: 在线\n" +
                "头像: " + avatarPath + "\n" +
                "登录方式: UDP");
        System.out.println("用户 " + ClientMain.getCurrentUserName() + " 更新头像为: " + avatarPath);
    }

}