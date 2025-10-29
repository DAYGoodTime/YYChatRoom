package com.yychat.view;

import com.yychat.api.Connection;
import com.yychat.control.YYchatClientConnectionUDP;
import com.yychat.model.Message;
import com.yychat.model.MessageType;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FriendChat extends JFrame implements KeyListener {
    private JButton sendButton = new JButton("发送");
    private JTextPane textArea = new JTextPane(); // 使用JTextPane替代JTextArea以支持组件插入
    private String sender;
    private String receiver;
    private YYchatClientConnectionUDP udpConnection = null; // UDP连接对象

    // 头像相关字段
    private ImageIcon senderAvatar = null; // 发送者头像（当前用户）
    private ImageIcon receiverAvatar = null; // 接收者头像（对方用户）
    private static final int AVATAR_SIZE = 40; // 头像显示大小

    // UDP构造函数
    public FriendChat(String sender, String receiver,Connection udpConnection) {
        this.sender = sender;
        this.receiver = receiver;
        this.udpConnection = (YYchatClientConnectionUDP) udpConnection;

        // 加载头像
        loadAvatars();

        // 设置文本区域为支持多色显示和组件插入
        textArea.setEditable(false);
        textArea.setBackground(Color.WHITE);
        textArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        textArea.setContentType("text/html"); // 支持HTML样式
        JScrollPane scrollPane = new JScrollPane(textArea);
        this.add(scrollPane, BorderLayout.CENTER);

        JTextField messageField = new JTextField(15);
        messageField.addKeyListener(this);

        sendButton.addActionListener(e -> {
            String msg = messageField.getText();
            if (msg.trim().isEmpty()) return;

            // 使用新的格式化方法显示发送的消息
            appendSendMessage(msg, sender);
            messageField.setText("");

            Message message = new Message();
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setMessageType(MessageType.COMMON_CHAT_MESSAGE);
            message.setContent(msg);

            try {
                if (this.udpConnection != null) {
                    this.udpConnection.sendChatMessage(message);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                // 错误消息使用红色标签显示
                JLabel errorLabel = new JLabel("消息发送失败: " + ex.getMessage());
                errorLabel.setForeground(Color.RED);
                textArea.setCaretPosition(textArea.getDocument().getLength());
                textArea.insertComponent(errorLabel);
                textArea.replaceSelection("\n");
            }
        });

        sendButton.setForeground(Color.blue);

        JPanel sendPanel = new JPanel();
        sendPanel.add(messageField);
        sendPanel.add(sendButton);
        this.add(sendPanel, BorderLayout.SOUTH);

        this.setSize(480, 360);
        this.setLocationRelativeTo(null);
        this.setTitle( "与 " + receiver + " 的聊天界面");
        try {
            this.setIconImage(new ImageIcon("./res/duck2.gif").getImage());
        } catch (Exception e) {
            System.out.println("无法加载图标文件");
        }

        // 在setVisible(true)之前添加
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setResizable(true);

        // 优化文本区域滚动
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.setVisible(true);
    }

    /**
     * 格式化发送消息显示
     * @param message 消息内容
     * @param sender 发送者
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
        textArea.setCaretPosition(textArea.getDocument().getLength());
        textArea.insertComponent(messagePanel);

        // 添加分隔符
        textArea.replaceSelection("\n");

        // 自动滚动到底部
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    /**
     * 格式化接收消息显示
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
        JLabel messageLabel = new JLabel(message.getContent());
        messageLabel.setForeground(Color.BLACK);
        messageLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        messageLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        rightPanel.add(headerPanel, BorderLayout.NORTH);
        rightPanel.add(messageLabel, BorderLayout.CENTER);

        messagePanel.add(rightPanel, BorderLayout.CENTER);

        // 添加到文本区域（使用更安全的方式）
        textArea.setCaretPosition(textArea.getDocument().getLength());
        textArea.insertComponent(messagePanel);

        // 添加分隔符
        textArea.replaceSelection("\n");

        // 自动滚动到底部
        textArea.setCaretPosition(textArea.getDocument().getLength());
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
    public void append(Message message){
        appendReceiveMessage(message);
    }

    /**
     * 加载发送者和接收者的头像
     */
    private void loadAvatars() {
        // 加载当前用户头像
        senderAvatar = loadUserAvatar(sender);

        // 加载对方用户头像
        receiverAvatar = loadUserAvatar(receiver);
    }

    /**
     * 加载用户头像 - 本地优先，如果本地没有则从服务端获取
     * @param userName 用户名
     * @return 用户头像，如果获取失败返回null
     */
    private ImageIcon loadUserAvatar(String userName) {
        try {
            // 1. 首先尝试从CurrentUser获取头像地址（如果是当前用户）
            String avatarPath = getUserAvatarPath(userName);
            if (avatarPath == null) {
                System.out.println("用户 " + userName + " 头像路径为空，使用默认头像");
                avatarPath = "res/0.jpg"; // 默认头像
            }

            // 2. 尝试从本地加载头像
            ImageIcon icon = loadIconFromLocal(avatarPath);
            if (icon != null) {
                System.out.println("成功从本地加载用户 " + userName + " 的头像: " + avatarPath);
                return icon;
            }

            // 3. 本地没有，从服务端获取
            System.out.println("本地没有用户 " + userName + " 的头像，从服务端获取...");
            icon = loadIconFromServer(userName);
            if (icon != null) {
                System.out.println("成功从服务端获取用户 " + userName + " 的头像");
                return icon;
            }

            // 4. 服务端获取失败，返回默认头像
            System.out.println("无法获取用户 " + userName + " 的头像，使用默认头像");
            return loadIconFromLocal("res/0.jpg");

        } catch (Exception e) {
            System.err.println("加载用户 " + userName + " 头像时发生错误: " + e.getMessage());
            e.printStackTrace();
            // 发生错误时返回默认头像
            return loadIconFromLocal("res/0.jpg");
        }
    }

    /**
     * 获取用户头像路径
     * @param userName 用户名
     * @return 头像路径，如果无法获取返回null
     */
    private String getUserAvatarPath(String userName) {
        try {
            // 如果是当前用户，从CurrentUser获取头像路径
            if (userName.equals(sender)) {
                com.yychat.model.User currentUser = getCurrentUser();
                if (currentUser != null && currentUser.getAvatarPath() != null) {
                    System.out.println("从CurrentUser获取当前用户头像路径: " + currentUser.getAvatarPath());
                    return currentUser.getAvatarPath();
                }
            }

            // 如果是其他用户，从服务端获取用户信息
            return getUserAvatarPathFromServer(userName);

        } catch (Exception e) {
            System.err.println("获取用户 " + userName + " 头像路径时发生错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从CurrentUser获取当前用户信息
     */
    private com.yychat.model.User getCurrentUser() {
        try {
            return com.yychat.view.ClientMain.getCurrentUser();
        } catch (Exception e) {
            System.err.println("无法获取当前用户信息: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从服务端获取用户头像路径
     * @param userName 用户名
     * @return 头像路径，如果无法获取返回null
     */
    private String getUserAvatarPathFromServer(String userName) {
        try {
            if (udpConnection == null) {
                System.err.println("UDP连接为空，无法从服务端获取用户信息");
                return null;
            }

            // 使用封装的同步请求方法，5秒超时
            String avatarPath = udpConnection.requestUserAvatarPath(userName, 5000);
            if (avatarPath != null && !avatarPath.trim().isEmpty()) {
                System.out.println("成功从服务端获取用户 " + userName + " 的头像路径: " + avatarPath);
                return avatarPath;
            } else {
                System.out.println("无法从服务端获取用户 " + userName + " 的头像路径");
                return null;
            }

        } catch (Exception e) {
            System.err.println("从服务端获取用户 " + userName + " 头像路径时发生错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从本地加载图标
     * @param avatarPath 头像路径
     * @return 图标对象，如果加载失败返回null
     */
    private ImageIcon loadIconFromLocal(String avatarPath) {
        try {
            if (avatarPath == null || avatarPath.trim().isEmpty()) {
                return null;
            }

            // 根据路径类型确定完整路径
            String fullPath;
            if (avatarPath.startsWith("avatars/") || avatarPath.contains("://")) {
                // 自定义头像或网络路径：使用完整路径
                fullPath = avatarPath;
            } else {
                // 默认头像：使用res目录
                fullPath = "res/" + avatarPath;
            }

            ImageIcon icon = new ImageIcon(fullPath);
            if (icon.getImage() != null && icon.getIconWidth() > 0) {
                // 缩放到合适大小
                Image scaledImage = icon.getImage().getScaledInstance(AVATAR_SIZE, AVATAR_SIZE, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            System.err.println("无法从本地加载头像路径 " + avatarPath + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * 从服务端加载头像 - 使用封装的同步请求方法
     * @param userName 用户名
     * @return 图标对象，如果加载失败返回null
     */
    private ImageIcon loadIconFromServer(String userName) {
        try {
            if (udpConnection == null) {
                System.err.println("UDP连接为空，无法从服务端加载头像");
                return null;
            }

            System.out.println("开始从服务端获取用户 " + userName + " 的头像...");

            // 使用封装的同步请求方法，5秒超时
            byte[] avatarData = udpConnection.requestUserAvatar(userName, 5000);

            if (avatarData != null && avatarData.length > 0) {
                // 创建头像图标
                ImageIcon icon = createImageIconFromData(avatarData, userName);
                if (icon != null) {
                    // 保存到本地缓存
                    saveAvatarToLocalCache(userName, avatarData);
                    System.out.println("成功从服务端获取用户 " + userName + " 的头像数据");
                    return icon;
                }
            } else {
                System.out.println("无法从服务端获取用户 " + userName + " 的头像数据");
            }

        } catch (Exception e) {
            System.err.println("从服务端加载用户 " + userName + " 头像时发生错误: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 从字节数据创建ImageIcon
     */
    private ImageIcon createImageIconFromData(byte[] avatarData, String userName) {
        try {
            if (avatarData == null || avatarData.length == 0) {
                return null;
            }

            // 直接使用字节数据创建ImageIcon
            ImageIcon icon = new ImageIcon(avatarData);

            if (icon.getImage() != null && icon.getIconWidth() > 0) {
                // 缩放到合适大小
                Image scaledImage = icon.getImage().getScaledInstance(AVATAR_SIZE, AVATAR_SIZE, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }

        } catch (Exception e) {
            System.err.println("从字节数据创建用户 " + userName + " 的头像图标失败: " + e.getMessage());
        }

        return null;
    }

    /**
     * 保存头像到本地缓存
     */
    private void saveAvatarToLocalCache(String userName, byte[] avatarData) {
        try {
            // 创建头像缓存目录
            java.io.File cacheDir = new java.io.File("avatars/cache/");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            // 保存头像文件
            String fileName = "avatars/cache/" + userName + ".jpg";
            java.io.FileOutputStream fos = new java.io.FileOutputStream(fileName);
            fos.write(avatarData);
            fos.close();

            System.out.println("已缓存用户 " + userName + " 的头像到: " + fileName);

        } catch (Exception e) {
            System.err.println("保存用户 " + userName + " 头像到本地缓存失败: " + e.getMessage());
        }
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
            avatarLabel.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
            avatarLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            avatarLabel.setBackground(Color.LIGHT_GRAY);
            avatarLabel.setOpaque(true);
        }
        return avatarLabel;
    }
}
