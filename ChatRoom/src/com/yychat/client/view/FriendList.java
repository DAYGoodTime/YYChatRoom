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
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
    protected JPanel friendPanel;
    protected JPanel strangerListPanel;
    protected JPanel strangerPanel;
    private final static HashMap<String, FriendChat> friendChatMap = new HashMap<String, FriendChat>();

    private final CountDownLatch initializationLatch;
    private final CountDownLatch friendListLatch;

    // 头像相关字段
    private final HashMap<String, ImageIcon> avatarCache = new HashMap<String, ImageIcon>(); // 头像缓存
    private final HashMap<String, JLabel> friendLabelMap = new HashMap<String, JLabel>();    // 好友标签映射

    public static FriendChat getFriendChat(String name) {
        return friendChatMap.get(name);
    }

    public static HashMap<String, FriendChat> getFriendChatMap() {
        return friendChatMap;
    }

    public FriendList() {
        this.initializationLatch = new CountDownLatch(1);
        this.friendListLatch = new CountDownLatch(1);

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
        setupCardLayout();
        setupButtonListeners();
    }

    /**
     * 初始化面板
     */
    private void initializePanels() {
        friendPanel = new JPanel(new BorderLayout());
        strangerPanel = new JPanel(new BorderLayout());

        initializeFriendPanel();
        initializeStrangerPanel();
    }

    /**
     * 初始化好友面板
     */
    private void initializeFriendPanel() {
        JPanel addFriendPanel = new JPanel(new GridLayout(2, 1));
        JButton friendButton1 = new JButton("我的好友");
        JButton strangerButton1 = new JButton("陌生人");
        JButton addFriendButton = initAddFriendBTN();

        addFriendPanel.add(addFriendButton);
        addFriendPanel.add(friendButton1);

        JButton blackListButton1 = new JButton("黑名单");
        friendPanel.add(addFriendPanel, BorderLayout.NORTH);

        JPanel strangerBlackPanel = new JPanel(new GridLayout(2, 1));
        strangerBlackPanel.add(strangerButton1);
        strangerBlackPanel.add(blackListButton1);
        friendPanel.add(strangerBlackPanel, BorderLayout.SOUTH);
    }

    /**
     * 初始化陌生人面板
     */
    private void initializeStrangerPanel() {
        JButton friendButton2 = new JButton("我的好友");
        JButton strangerButton2 = new JButton("陌生人");
        JPanel friendStrangerPanel = new JPanel(new GridLayout(2, 1));
        friendStrangerPanel.add(friendButton2);
        friendStrangerPanel.add(strangerButton2);
        strangerPanel.add(friendStrangerPanel, BorderLayout.NORTH);
        JButton blackListButton2 = new JButton("黑名单");
        strangerPanel.add(blackListButton2, BorderLayout.SOUTH);
    }

    /**
     * 设置卡片布局
     */
    private void setupCardLayout() {
        CardLayout cardLayout = new CardLayout();
        // 设置content pane的布局管理器，而不是JFrame本身的
        this.getContentPane().setLayout(cardLayout);
        this.getContentPane().add(friendPanel, "Card1");
        this.getContentPane().add(strangerPanel, "Card2");
        // 使用content pane进行面板切换
        cardLayout.show(this.getContentPane(), "Card1");
    }

    /**
     * 设置按钮监听器
     */
    private void setupButtonListeners() {
        // 从面板中获取按钮（这里需要通过查找方式获取）
        Component[] friendPanelComponents = friendPanel.getComponents();
        for (Component comp : friendPanelComponents) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                Component[] subComponents = panel.getComponents();
                for (Component subComp : subComponents) {
                    if (subComp instanceof JButton && "陌生人".equals(((JButton) subComp).getText())) {
                        ((JButton) subComp).addActionListener(e -> {
                            LayoutManager layout = this.getContentPane().getLayout();
                            if (layout instanceof CardLayout) {
                                CardLayout cardLayout = (CardLayout) layout;
                                // 使用content pane进行面板切换，与setupCardLayout保持一致
                                cardLayout.show(this.getContentPane(), "Card2");
                            }
                        });
                        break;
                    }
                }
            }
        }

        Component[] strangerPanelComponents = strangerPanel.getComponents();
        for (Component comp : strangerPanelComponents) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                Component[] subComponents = panel.getComponents();
                for (Component subComp : subComponents) {
                    if (subComp instanceof JButton && "我的好友".equals(((JButton) subComp).getText())) {
                        ((JButton) subComp).addActionListener(e -> {
                            LayoutManager layout = this.getContentPane().getLayout();
                            if (layout instanceof CardLayout) {
                                CardLayout cardLayout = (CardLayout) layout;
                                // 使用content pane进行面板切换，与setupCardLayout保持一致
                                cardLayout.show(this.getContentPane(), "Card1");
                            }
                        });
                        break;
                    }
                }
            }
        }
    }

    /**
     * 初始化窗口框架
     */
    private void initializeFrame() {
        this.setIconImage(ImageIconUtil.getWindowIcon().getImage());
        this.setTitle(ClientMain.getCurrentUser().getUserName() + "的好友列表");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setBounds(800, 600, 350, 250);
    }

    /**
     * 设置窗口可见
     */
    private void setFrameVisible() {
        this.setVisible(true);
    }

    //初始化添加好友按钮
    private JButton initAddFriendBTN() {
        JButton addFriendButton = new JButton("添加好友");
        addFriendButton.addActionListener(e -> {
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
        return addFriendButton;
    }

    /**
     * 创建通用的好友鼠标监听器
     */
    private MouseListener createFriendMouseListener() {
        User sender = ClientMain.getCurrentUser();
        final FriendList instance = this;
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getSource() instanceof JLabel) {
                    JLabel label = (JLabel) e.getSource();
                    String clickedFriendName = label.getText();
                    //获取好友信息
                    ServiceResponse<User> optionalUser = UserService.getInstance().queryUserInfoByUsername(clickedFriendName);
                    if (!optionalUser.isSuccess()) {
                        JOptionPane.showMessageDialog(instance, optionalUser.getMessage());
                        return;
                    }
                    // 创建聊天窗口
                    FriendChat chat = new FriendChat(sender, optionalUser.getData());
                    friendChatMap.put(sender.getUserName() + "to" + clickedFriendName, chat);
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
                    label.setForeground(Color.red);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (e.getSource() instanceof JLabel) {
                    JLabel label = (JLabel) e.getSource();
                    label.setForeground(Color.black);
                }
            }
        };
    }

    /**
     * 创建通用的JLabel好友标签（增强版 - 支持动态头像加载）
     */
    private JLabel createFriendLabel(String friendName) {
        ImageIcon icon = loadDynamicFriendIcon(friendName);
        JLabel label = new JLabel(friendName, icon, JLabel.LEFT);
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
    private ImageIcon loadDynamicFriendIcon(String friendName) {
        // 先检查缓存
        if (avatarCache.containsKey(friendName)) {
            return avatarCache.get(friendName);
        }
        // 获取头像
        ImageIcon icon = AvatarService.getInstance().loadUserAvatar(friendName);
        avatarCache.put(friendName, icon);
        return icon;
    }

    /**
     * 更新好友头像
     */
    public void updateFriendAvatar(String friendName, String avatarPath) {
        JLabel friendLabel = friendLabelMap.get(friendName);
        if (friendLabel != null) {
            ImageIcon newIcon = AvatarService.getInstance().loadUserAvatar(friendName);
            //更新缓存
            avatarCache.put(friendName, newIcon);
            System.out.println("已更新好友 " + friendName + " 的头像为（本地）: " + avatarPath);
            friendLabel.setIcon(newIcon);
        }
    }

    /**
     * 清除头像缓存（用于强制重新加载）
     */
    public void clearAvatarCache() {
        avatarCache.clear();
        System.out.println("已清除头像缓存");
    }

    /**
     * 初始化陌生人列表
     */
    public void updateStrangerPanel(java.util.List<String> strangers, boolean first) {
        if (first || waitingReady()) {
            if (strangers == null) {
                strangers = new java.util.ArrayList<>();
            }
            // 创建新的陌生人列表面板，使用单列布局
            strangerListPanel = new JPanel(new GridLayout(0, 1));
            ImageIcon strangerIcon = ImageIconUtil.getStrangerIcon();

            // 添加所有陌生人标签
            for (String stranger : strangers) {
                if (stranger != null && !stranger.trim().isEmpty()) {
                    JLabel strangerLabel = new JLabel(stranger, strangerIcon, JLabel.LEFT);
                    strangerListPanel.add(strangerLabel);
                }
            }
            //将陌生人列表添加到面板

            // 清理现有的ScrollPane（如果有）
            Component[] components = strangerPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JScrollPane) {
                    strangerPanel.remove(comp);
                    break;
                }
            }

            // 创建新的ScrollPane并添加到面板
            JScrollPane strangerListScrollPane = new JScrollPane(strangerListPanel);
            strangerPanel.add(strangerListScrollPane, BorderLayout.CENTER);

            // 刷新面板显示
            refreshPanel(strangerListPanel);
            refreshPanel(strangerPanel);
        } else {
            System.out.println("无法初始化陌生人列表");
        }
    }


    public JLabel[] getFriendLabel() {
        // 使用CountDownLatch等待好友列表初始化完成
        try {
            if (!initializationLatch.await(120, java.util.concurrent.TimeUnit.SECONDS)) {
                System.out.println("处理好友超时，已跳过");
                return null;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("等待好友列表初始化时发生中断");
            return null;
        }
        return friendLabel;
    }

    public void activeOnlineFriendIcon(java.util.List<String> onlineFriends) {
        JLabel[] friendLabel = getFriendLabel();
        if (onlineFriends == null) return;
        for (String friendName : onlineFriends) {
            for (JLabel jLabel : friendLabel) {
                if (jLabel != null && jLabel.getText().equals(friendName)) {
                    jLabel.setEnabled(true);
                }
            }
        }
    }

    public void activeNewOnlineFriendIcon(String s) {
        JLabel[] friendLabel = getFriendLabel();
        if (!s.isEmpty() && friendLabel != null) {
            for (JLabel jLabel : friendLabel) {
                if (jLabel != null && jLabel.getText().equals(s)) {
                    jLabel.setEnabled(true);
                }
            }
        }
    }

    public void addNewFriend(String friendName) {
        JLabel label = createFriendLabel(friendName);
        friendListPanel.add(label);
        friendListPanel.revalidate();
        friendListPanel.repaint();
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
    public void setFriendList(java.util.List<String> friendList) {
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
            // 标记好友列表设置完成
            friendListLatch.countDown();
        } catch (Exception e) {
            System.err.println("设置好友列表时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 初始化好友列表面板
     */
    private void initializeFriendListPanel(java.util.List<String> friendList) {
        int friendListSize = friendList.size();
        friendListPanel = new JPanel(new GridLayout(0, 1)); // 单列布局
        friendLabel = new JLabel[friendListSize];

        for (int i = 0; i < friendListSize; i++) {
            String friendName = friendList.get(i);
            if (!StrUtil.isBlank(friendName)) {
                friendLabel[i] = createFriendLabel(friendName);
                friendListPanel.add(friendLabel[i]);
            }
        }

        addFriendListToPanel();
    }

    /**
     * 将好友列表添加到面板
     */
    private void addFriendListToPanel() {
        JScrollPane friendListScrollPane = new JScrollPane(friendListPanel);
        friendPanel.add(friendListScrollPane, BorderLayout.CENTER);
        refreshPanel(friendPanel);
    }

    /**
     * 刷新面板
     */
    private void refreshPanel(JPanel panel) {
        panel.revalidate();
        panel.repaint();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        try {
            // 清理好友聊天窗口映射
            if (friendChatMap != null) {
                for (FriendChat chat : friendChatMap.values()) {
                    if (chat != null) {
                        chat.dispose();
                    }
                }
                friendChatMap.clear();
            }

            // 清理CountDownLatch
            if (initializationLatch != null) {
                initializationLatch.countDown();
            }
            if (friendListLatch != null) {
                friendListLatch.countDown();
            }

        } catch (Exception e) {
            System.err.println("清理资源时发生错误: " + e.getMessage());
        }
    }

}
