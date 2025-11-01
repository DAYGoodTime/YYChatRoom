package com.yychat.client.view;

import cn.hutool.core.util.StrUtil;
import com.yychat.client.ClientMain;
import com.yychat.client.service.AvatarService;
import com.yychat.common.model.ServiceResponse;
import com.yychat.client.service.UserService;
import com.yychat.client.util.ImageIconUtil;
import com.yychat.common.model.Message;
import com.yychat.common.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 好友列表窗口类，支持TCP和UDP协议
 * 对于UDP，需要通过ClientLoginUDP传递连接对象
 */

public class FriendList extends JFrame {
    protected JLabel[] friendLabel;
    protected JPanel friendListPanel;
    protected JPanel strangerListPanel;
    // 旧的面板变量保留以兼容现有代码
    protected JPanel friendPanel;
    protected JPanel strangerPanel;

    private final static HashMap<String, FriendChat> friendChatMap = new HashMap<String, FriendChat>();

    private CountDownLatch initializationLatch;
    private CountDownLatch friendListLatch;

    // 新布局组件
    private JPanel leftPanel;           // 左侧面板（头像+按钮+添加）
    private JPanel rightPanel;          // 右侧面板（内容区域）
    private JPanel userAvatarPanel;     // 用户头像面板
    private JPanel navButtonPanel;      // 导航按钮面板
    private JPanel addButtonPanel;      // 添加按钮面板

    // 右侧内容面板
    private JPanel contentPanel;        // 内容主面板
    private JPanel friendContentPanel;  // 好友列表内容面板
    private JPanel strangerContentPanel; // 陌生人列表内容面板
    private JPanel groupContentPanel;   // 群组列表内容面板（预留）

    // 导航按钮
    private JButton friendButton;
    private JButton strangerButton;
    private JButton groupButton;

    // 头像相关字段
    private final HashMap<String, ImageIcon> avatarCache = new HashMap<>(); // 头像缓存
    private final HashMap<String, JLabel> friendLabelMap = new HashMap<>();    // 好友标签映射
    private JLabel userAvatarLabel;     // 用户头像标签

    public static FriendChat getFriendChat(String name) {
        return friendChatMap.get(name);
    }

    public static HashMap<String, FriendChat> getFriendChatMap() {
        return friendChatMap;
    }

    public FriendList() {
        initializationLatch = new CountDownLatch(1);
        friendListLatch = new CountDownLatch(1);
        initializeNewComponents();
        initializeFrame();
        setFrameVisible();

        // 标记初始化完成
        initializationLatch.countDown();
    }

    /**
     * 初始化所有组件
     */
    private void initializeNewComponents() {
        initializeNewPanels();
        setupNewLayout();
        setupButtonListeners();
    }

    /**
     * 初始化新面板（新布局）
     */
    private void initializeNewPanels() {
        // 创建主面板使用BorderLayout
        setLayout(new BorderLayout());

        // 创建左侧面板（头像+按钮+添加）
        leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        leftPanel.setPreferredSize(new Dimension(200, 0)); // 固定宽度200

        initializeUserAvatarPanel();
        initializeNavButtonPanel();
        initializeAddButtonPanel();

        // 将所有面板添加到左侧面板
        leftPanel.add(userAvatarPanel);
        leftPanel.add(navButtonPanel);
        leftPanel.add(addButtonPanel);

        // 创建右侧面板（内容区域）
        rightPanel = new JPanel(new BorderLayout());
        initializeContentPanel();

        // 添加到主面板
        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    /**
     * 初始化用户头像面板
     */
    private void initializeUserAvatarPanel() {
        userAvatarPanel = new JPanel();
        userAvatarPanel.setLayout(new BoxLayout(userAvatarPanel, BoxLayout.Y_AXIS));
        userAvatarPanel.setBorder(new EmptyBorder(10, 10, 15, 10));

        // 创建用户头像标签
        userAvatarLabel = new JLabel();
        userAvatarLabel.setPreferredSize(new Dimension(80, 80));
        userAvatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        userAvatarLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        userAvatarLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        userAvatarLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 居中对齐

        // 加载用户头像
        loadUserAvatar();

        // 添加点击事件
        userAvatarLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    openMyInfoWindow();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                userAvatarLabel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                userAvatarLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            }
        });

        // 添加用户名标签
        JLabel userNameLabel = new JLabel(ClientMain.getCurrentUser().getUserName(), SwingConstants.CENTER);
        userNameLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        userNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT); // 居中对齐
        userNameLabel.setBorder(new EmptyBorder(5, 0, 0, 0)); // 顶部间距

        // 使用BoxLayout垂直排列，头像在上，用户名在下
        userAvatarPanel.add(userAvatarLabel);
        userAvatarPanel.add(userNameLabel);
    }

    /**
     * 初始化导航按钮面板
     */
    private void initializeNavButtonPanel() {
        navButtonPanel = new JPanel();
        navButtonPanel.setLayout(new GridLayout(3, 1, 5, 5));
        navButtonPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        // 创建导航按钮
        friendButton = new JButton("我的好友");
        strangerButton = new JButton("陌生人");
        groupButton = new JButton("我的群组");

        // 设置按钮样式
        styleNavButton(friendButton);
        styleNavButton(strangerButton);
        styleNavButton(groupButton);

        navButtonPanel.add(friendButton);
        navButtonPanel.add(strangerButton);
        navButtonPanel.add(groupButton);
    }

    /**
     * 初始化添加按钮面板
     */
    private void initializeAddButtonPanel() {
        addButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // 创建添加图标按钮
        JButton addButton = createAddButton();
        addButtonPanel.add(addButton);
    }

    /**
     * 初始化内容面板
     */
    private void initializeContentPanel() {
        contentPanel = new JPanel(new CardLayout());

        // 创建好友列表内容面板
        friendContentPanel = new JPanel(new BorderLayout());
        friendListPanel = new JPanel();
        friendListPanel.setLayout(new BoxLayout(friendListPanel, BoxLayout.Y_AXIS));
        JScrollPane friendScrollPane = new JScrollPane(friendListPanel);
        friendContentPanel.add(friendScrollPane, BorderLayout.CENTER);

        // 创建陌生人列表内容面板
        strangerContentPanel = new JPanel(new BorderLayout());
        strangerListPanel = new JPanel();
        strangerListPanel.setLayout(new BoxLayout(strangerListPanel, BoxLayout.Y_AXIS));
        JScrollPane strangerScrollPane = new JScrollPane(strangerListPanel);
        strangerContentPanel.add(strangerScrollPane, BorderLayout.CENTER);

        // 创建群组列表内容面板（预留）
        groupContentPanel = new JPanel(new BorderLayout());
        JLabel groupLabel = new JLabel("群组功能正在开发中...", SwingConstants.CENTER);
        groupContentPanel.add(groupLabel, BorderLayout.CENTER);

        // 添加到内容面板
        contentPanel.add(friendContentPanel, "friend");
        contentPanel.add(strangerContentPanel, "stranger");
        contentPanel.add(groupContentPanel, "group");

        rightPanel.add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * 设置新布局
     */
    private void setupNewLayout() {
        // 默认显示好友列表
        showFriendContent();
    }


    /**
     * 设置按钮监听器（新布局）
     */
    private void setupButtonListeners() {
        // 设置导航按钮监听器
        if (friendButton != null) {
            friendButton.addActionListener(e -> showFriendContent());
        }

        if (strangerButton != null) {
            strangerButton.addActionListener(e -> showStrangerContent());
        }

        if (groupButton != null) {
            groupButton.addActionListener(e -> showGroupContent());
        }
    }

    /**
     * 初始化窗口框架
     */
    private void initializeFrame() {
        this.setIconImage(ImageIconUtil.getWindowIcon().getImage());
        this.setTitle(ClientMain.getCurrentUser().getUserName() + "的好友列表");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setBounds(800, 600, 800, 500); // 增大窗口尺寸
    }

    /**
     * 设置窗口可见
     */
    private void setFrameVisible() {
        this.setVisible(true);
    }

    /**
     * 创建好友鼠标监听器（支持右键菜单）
     */
    private MouseListener createFriendMouseListener() {
        User sender = ClientMain.getCurrentUser();
        final FriendList instance = this;
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() instanceof JLabel) {
                    JLabel label = (JLabel) e.getSource();
                    String clickedFriendName = label.getText();

                    // 右键点击显示菜单
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        showFriendContextMenu(e, clickedFriendName);
                        return;
                    }

                    // 左键双击打开聊天窗口
                    if (e.getClickCount() == 2) {
                        String chatKey = sender.getUserName() + "to" + clickedFriendName;
                        FriendChat chat = friendChatMap.get(chatKey);
                        if(chat != null){
                            //如果已经打开了就不用创建,对其进行高亮
                            chat.highlightChatWindow();
                            return;
                        }
                        //获取好友信息
                        ServiceResponse<User> optionalUser = UserService.getInstance().queryUserInfoByUsername(clickedFriendName);
                        if (!optionalUser.isSuccess()) {
                            JOptionPane.showMessageDialog(instance, optionalUser.getMessage());
                            return;
                        }
                        // 创建聊天窗口
                        chat = new FriendChat(sender, optionalUser.getData());
                        friendChatMap.put(chatKey, chat);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (e.getSource() instanceof JLabel) {
                    JLabel label = (JLabel) e.getSource();
                    // 保存原始背景色并设置悬停背景色
                    label.setBackground(new Color(173, 216, 230)); // 浅蓝色背景
                    label.setOpaque(true); // 使背景色生效
                    label.setForeground(Color.red); // 保持红色文本高亮
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (e.getSource() instanceof JLabel) {
                    JLabel label = (JLabel) e.getSource();
                    // 恢复默认背景色
                    label.setBackground(null); // 使用默认背景
                    label.setOpaque(false); // 取消不透明
                    label.setForeground(Color.black); // 恢复黑色文本
                }
            }
        };
    }

    /**
     * 创建陌生人鼠标监听器（支持右键菜单和双击添加）
     */
    private MouseListener createStrangerMouseListener() {
        final FriendList instance = this;
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() instanceof JLabel) {
                    JLabel label = (JLabel) e.getSource();
                    String strangerName = label.getText();
                    // 右键点击显示菜单
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        showStrangerContextMenu(e, strangerName);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (e.getSource() instanceof JLabel) {
                    JLabel label = (JLabel) e.getSource();
                    // 陌生人悬停效果（与好友区分）
                    label.setBackground(new Color(144, 238, 144)); // 浅绿色背景
                    label.setOpaque(true); // 使背景色生效
                    label.setForeground(Color.darkGray); // 深灰色文本
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (e.getSource() instanceof JLabel) {
                    JLabel label = (JLabel) e.getSource();
                    // 恢复默认背景色
                    label.setBackground(null); // 使用默认背景
                    label.setOpaque(false); // 取消不透明
                    label.setForeground(Color.black); // 恢复黑色文本
                }
            }
        };
    }

    /**
     * 显示陌生人右键菜单
     */
    private void showStrangerContextMenu(MouseEvent e, String strangerName) {
        JPopupMenu contextMenu = new JPopupMenu();

        // 添加为好友菜单项
        JMenuItem addFriendItem = new JMenuItem("添加为好友");
        addFriendItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                addStrangerAsFriend(strangerName);
            }
        });

        contextMenu.add(addFriendItem);
        contextMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * 显示好友右键菜单
     */
    private void showFriendContextMenu(MouseEvent e, String friendName) {
        JPopupMenu contextMenu = new JPopupMenu();

        // 删除好友菜单项（仅注册事件，未实现）
        JMenuItem deleteItem = new JMenuItem("删除好友");
        deleteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                // TODO: 实现删除好友功能
                System.out.println("删除好友功能待实现: " + friendName);
                JOptionPane.showMessageDialog(FriendList.this,
                    "删除好友功能待实现: " + friendName,
                    "提示",
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });

        contextMenu.add(deleteItem);
        contextMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * 将陌生人添加为好友
     */
    private void addStrangerAsFriend(String strangerName) {
        try {
            // 发送添加好友请求
            ClientMain.getUDPConnection().sendMessageToServer(
                    Message.builder()
                            .setMessageType(Message.USER_ADD_NEW_FRIEND)
                            .setSender(ClientMain.getCurrentUser().getUserName())
                            .setContent(strangerName)
            );

            JOptionPane.showMessageDialog(this,
                "已向 " + strangerName + " 发送好友申请",
                "提示",
                JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "添加好友失败: " + e.getMessage(),
                "错误",
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * 创建通用的JLabel好友标签（增强版 - 支持动态头像加载和整行高亮）
     */
    private JLabel createFriendLabel(User user) {
        String friendName = user.getUserName();
        ImageIcon icon = loadDynamicFriendIcon(user);
        JLabel label = new JLabel(friendName, icon, JLabel.LEFT);

        // 设置整行高亮效果 - 让标签占满整个可用宽度
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        label.setPreferredSize(new Dimension(0, 30)); // 高度30，宽度由布局管理器决定
        label.setMinimumSize(new Dimension(0, 30));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (!friendName.equals(ClientMain.getCurrentUser().getUserName())) {
            label.setEnabled(false);
        }
        label.addMouseListener(createFriendMouseListener());
        // 将标签添加到映射中，方便后续更新头像
        friendLabelMap.put(friendName, label);
        return label;
    }

    /**
     * 动态加载好友头像（优先从缓存或服务器获取）
     */
    private ImageIcon loadDynamicFriendIcon(User user) {
        String friendName = user.getUserName();
        // 先检查缓存
        if (avatarCache.containsKey(friendName)) {
            return avatarCache.get(friendName);
        }
        // 获取头像
        ImageIcon icon = AvatarService.getInstance().loadUserAvatar(friendName, user.getAvatarPath());
        avatarCache.put(friendName, icon);
        return icon;
    }

    /**
     * 更新好友头像
     */
    public void updateFriendAvatar(String friendName, String avatarPath) {
        JLabel friendLabel = friendLabelMap.get(friendName);
        if (friendLabel != null) {
            ImageIcon newIcon = AvatarService.getInstance().loadUserAvatar(friendName, avatarPath);
            //更新缓存
            avatarCache.put(friendName, newIcon);
            System.out.println("已更新好友 " + friendName + " 的头像为（本地）: " + avatarPath);
            friendLabel.setIcon(newIcon);
        }
    }

    /**
     * 初始化陌生人列表
     */
    public void updateStrangerPanel(java.util.List<User> strangers, boolean first) {
        // 检查组件是否已初始化
        if (strangerListPanel == null || contentPanel == null) {
            System.err.println("陌生人列表面板尚未初始化，跳过更新");
            return;
        }

        if (first || waitingReady()) {
            if (strangers == null) {
                strangers = new java.util.ArrayList<>();
            }
            // 清除现有内容
            strangerListPanel.removeAll();

            // 添加所有陌生人标签
            for (User stranger : strangers) {
                if (stranger != null && !stranger.getUserName().trim().isEmpty()) {
                    ImageIcon strangerIcon = AvatarService.getInstance().loadUserAvatar(stranger.getUserName(), stranger.getAvatarPath());
                    JLabel strangerLabel = new JLabel(stranger.getUserName(), strangerIcon, JLabel.LEFT);
                    // 设置整行高亮效果 - 让标签占满整个可用宽度
                    strangerLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                    strangerLabel.setPreferredSize(new Dimension(0, 30)); // 高度30，宽度由布局管理器决定
                    strangerLabel.setMinimumSize(new Dimension(0, 30));
                    strangerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    // 为陌生人标签添加专门的鼠标监听器（支持添加好友功能）
                    strangerLabel.addMouseListener(createStrangerMouseListener());
                    strangerListPanel.add(strangerLabel);
                }
            }

            // 刷新面板显示
            refreshPanel(strangerListPanel);
        } else {
            System.out.println("无法初始化陌生人列表");
        }
    }

    public JLabel[] getFriendLabel() {
        try {
            if (!friendListLatch.await(20, TimeUnit.SECONDS)) {
                System.out.println("好友列表获取超时");
                return null;
            }
            return friendLabel;
        } catch (InterruptedException e) {
            System.out.println("好友列表获取超时");
            return null;
        }
    }

    public void activeOnlineFriendIcon(java.util.List<String> onlineFriends) {
        // 检查组件是否已初始化
        if (friendListPanel == null) {
            System.err.println("好友列表面板尚未初始化，跳过在线好友激活");
            return;
        }

        JLabel[] friendLabelList = getFriendLabel();
        if (onlineFriends == null) return;
        for (String friendName : onlineFriends) {
            if (friendLabelList != null) {
                for (JLabel jLabel : friendLabelList) {
                    if (jLabel != null && jLabel.getText().equals(friendName)) {
                        jLabel.setEnabled(true);
                    }
                }
            }
        }
    }

    public void activeNewOnlineFriendIcon(String s) {
        if (waitingReady()) {
            // 检查组件是否已初始化
            if (friendListPanel == null) {
                System.err.println("好友列表面板尚未初始化，跳过新好友激活");
                return;
            }

            JLabel[] friendLabel = getFriendLabel();
            if (!s.isEmpty() && friendLabel != null) {
                for (JLabel jLabel : friendLabel) {
                    if (jLabel != null && jLabel.getText().equals(s)) {
                        jLabel.setEnabled(true);
                    }
                }
            }
        }
    }

    public void addNewFriend(String friendName) {
        // 检查组件是否已初始化
        if (friendListPanel == null) {
            System.err.println("好友列表面板尚未初始化，跳过添加好友");
            return;
        }

        try {
            JLabel label = createFriendLabel(new User(friendName, null));
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            friendListPanel.add(label);
            refreshPanel(friendListPanel);
        } catch (Exception e) {
            System.err.println("添加好友时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 等待初始化完成，使用CountDownLatch替代忙等待
     */
    public boolean waitingReady() {
        try {
            if (!initializationLatch.await(20, TimeUnit.SECONDS)) {
                System.out.println("初始化失败");
                return false;
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("等待初始化时被中断");
            return false;
        }
        return true;
    }

    /**
     * 设置好友列表
     */
    public void setFriendList(java.util.List<User> friendList) {
        if (friendList == null || friendList.isEmpty()) {
            System.out.println("好友列表为空，无法设置");
            return;
        }
        if (!waitingReady()) {
            System.out.println("窗口初始化未完成，无法设置好友列表");
            return;
        }
        try {
            initializeFriendListPanel(friendList);
        } catch (Exception e) {
            System.err.println("设置好友列表时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 初始化好友列表面板
     */
    private void initializeFriendListPanel(java.util.List<User> friendList) {
        // 检查组件是否已初始化
        if (friendListPanel == null || contentPanel == null) {
            System.err.println("好友列表面板尚未初始化，跳过设置");
            return;
        }

        int friendListSize = friendList.size();
        friendListPanel.removeAll(); // 清除现有内容
        friendLabel = new JLabel[friendListSize];
        for (int i = 0; i < friendListSize; i++) {
            String friendName = friendList.get(i).getUserName();
            if (!StrUtil.isBlank(friendName)) {
                friendLabel[i] = createFriendLabel(friendList.get(i));
                friendLabel[i].setAlignmentX(Component.LEFT_ALIGNMENT);
                friendListPanel.add(friendLabel[i]);
            }
        }
        refreshPanel(friendListPanel);
        friendListLatch.countDown();
    }

    /**
     * 加载用户头像
     */
    private void loadUserAvatar() {
        try {
            // 检查组件是否已初始化
            if (userAvatarLabel == null) {
                System.err.println("用户头像标签尚未初始化，跳过加载");
                return;
            }

            User currentUser = ClientMain.getCurrentUser();
            if (currentUser != null) {
                String avatarPath = currentUser.getAvatarPath();
                ImageIcon icon = AvatarService.getInstance().loadUserAvatar(currentUser.getUserName(), avatarPath);
                // 缩放头像到80x80
                Image scaledImage = icon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                userAvatarLabel.setIcon(new ImageIcon(scaledImage));
            }
        } catch (Exception e) {
            // 使用默认头像
            if (userAvatarLabel != null) {
                userAvatarLabel.setText("头像\n加载失败");
            }
            System.err.println("[FriendList]无法加载用户头像: " + e.getMessage());
        }
    }

    /**
     * 设置导航按钮样式
     */
    private void styleNavButton(JButton button) {
        button.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setIconTextGap(10);
    }

    /**
     * 创建添加按钮
     */
    private JButton createAddButton() {
        JButton addButton = new JButton();
        addButton.setPreferredSize(new Dimension(40, 40));
        addButton.setMaximumSize(new Dimension(40, 40));
        addButton.setMinimumSize(new Dimension(40, 40));

        // 设置加号图标
        try {
            // 使用文本显示加号
            addButton.setText("+");
            addButton.setFont(new Font("微软雅黑", Font.BOLD, 24));
        } catch (Exception e) {
            // 如果设置文本失败，使用默认设置
        }

        addButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addButton.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // 添加悬停效果
        addButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                addButton.setBorder(BorderFactory.createLineBorder(Color.BLUE));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                addButton.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            }
        });

        // 添加点击事件
        addButton.addActionListener(e -> {
            String newFriendName = JOptionPane.showInputDialog("请输入新好友的名字：");
            if (newFriendName != null) {
                ClientMain.getUDPConnection().sendMessageToServer(
                        Message.builder()
                                .setMessageType(Message.USER_ADD_NEW_FRIEND)
                                .setSender(ClientMain.getCurrentUser().getUserName())
                                .setContent(newFriendName)
                );
            }
        });

        return addButton;
    }

    /**
     * 显示好友内容
     */
    private void showFriendContent() {
        CardLayout cardLayout = (CardLayout) contentPanel.getLayout();
        cardLayout.show(contentPanel, "friend");

        // 更新按钮状态
        updateButtonState(friendButton);
    }

    /**
     * 显示陌生人内容
     */
    private void showStrangerContent() {
        CardLayout cardLayout = (CardLayout) contentPanel.getLayout();
        cardLayout.show(contentPanel, "stranger");

        // 更新按钮状态
        updateButtonState(strangerButton);
    }

    /**
     * 显示群组内容
     */
    private void showGroupContent() {
        CardLayout cardLayout = (CardLayout) contentPanel.getLayout();
        cardLayout.show(contentPanel, "group");

        // 更新按钮状态
        updateButtonState(groupButton);
    }

    /**
     * 更新按钮状态
     */
    private void updateButtonState(JButton activeButton) {
        // 重置所有按钮状态
        JButton[] buttons = {friendButton, strangerButton, groupButton};
        for (JButton button : buttons) {
            if (button != null) {
                button.setBackground(null); // 使用默认背景
                button.setForeground(Color.BLACK);
            }
        }

        // 设置激活按钮状态
        if (activeButton != null) {
            activeButton.setBackground(new Color(173, 216, 230)); // 浅蓝色
            activeButton.setForeground(Color.BLACK);
        }
    }

    /**
     * 打开MyInfo窗口
     */
    private void openMyInfoWindow() {
        try {
            MyInfo myInfo = new MyInfo(this);
            myInfo.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "打开个人信息窗口失败：" + e.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * 刷新面板
     */
    private void refreshPanel(JPanel panel) {
        panel.revalidate();
        panel.repaint();
    }
}
