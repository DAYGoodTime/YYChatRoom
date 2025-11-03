package com.yychat.client.view;

import com.yychat.client.ClientMain;
import com.yychat.client.service.AvatarService;
import com.yychat.client.util.ImageIconUtil;
import com.yychat.client.view.chat.FriendChat;
import com.yychat.client.view.chat.GroupChat;
import com.yychat.client.view.listpanel.FriendListPanel;
import com.yychat.client.view.listpanel.GroupListPanel;
import com.yychat.client.view.listpanel.StrangerListPanel;
import com.yychat.common.model.Message;
import com.yychat.common.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 好友列表窗口类，支持TCP和UDP协议
 * 对于UDP，需要通过ClientLoginUDP传递连接对象
 */

public class MainWindow extends JFrame {
    private final static HashMap<String, FriendChat> friendChatMap = new HashMap<>();
    private final static HashMap<String, GroupChat> groupChatMap = new HashMap<>();

    private final CountDownLatch initializationLatch;

    // 新布局组件
    private JPanel leftPanel;           // 左侧面板（头像+按钮+添加）
    private JPanel rightPanel;          // 右侧面板（内容区域）
    private JPanel userAvatarPanel;     // 用户头像面板
    private JPanel navButtonPanel;      // 导航按钮面板
    private JPanel addButtonPanel;      // 添加按钮面板

    // 右侧内容面板
    private JPanel contentPanel;        // 内容主面板
    private FriendListPanel friendContentPanel;  // 好友列表内容面板
    private StrangerListPanel strangerListPanel; // 陌生人列表内容面板
    private GroupListPanel groupContentPanel;    // 群组列表内容面板

    // 导航按钮
    private JButton friendButton;
    private JButton strangerButton;
    private JButton groupButton;

    // 头像相关字段

    private JLabel userAvatarLabel;     // 用户头像标签

    public MainWindow() {
        initializationLatch = new CountDownLatch(1);
        initializeComponents();
        initializeFrame();
        setFrameVisible();

        // 标记初始化完成
        initializationLatch.countDown();
    }

    /**
     * 初始化所有组件
     */
    private void initializeComponents() {
        initializePanels();
        setupLayout();
        setupButtonListeners();
    }

    /**
     * 初始化新面板
     */
    private void initializePanels() {
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
        friendContentPanel = new FriendListPanel(this);

        // 创建陌生人列表内容面板
        strangerListPanel = new StrangerListPanel(this);

        // 创建群组列表内容面板
        groupContentPanel = new GroupListPanel(this);

        // 添加到内容面板
        contentPanel.add(friendContentPanel, "friend");
        contentPanel.add(strangerListPanel, "stranger");
        contentPanel.add(groupContentPanel, "group");

        rightPanel.add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * 设置布局
     */
    private void setupLayout() {
        // 默认显示好友列表
        showFriendContent();
    }

    /**
     * 设置按钮监听器
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
                ImageIcon icon = AvatarService.loadUserIcon(currentUser.getUserName(), avatarPath);
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

    //Getter
    public StrangerListPanel getStrangerListPanel() {
        return strangerListPanel;
    }

    public FriendListPanel getFriendListPanel() {
        return friendContentPanel;
    }

    public GroupListPanel getGroupListPanel() {
        return groupContentPanel;
    }

    public static FriendChat getFriendChat(String name) {
        return friendChatMap.get(name);
    }

    public static HashMap<String, FriendChat> getFriendChatMap() {
        return friendChatMap;
    }

    public static GroupChat getGroupChat(String name) {
        return groupChatMap.get(name);
    }

    public static HashMap<String, GroupChat> getGroupChatMap() {
        return groupChatMap;
    }
}
