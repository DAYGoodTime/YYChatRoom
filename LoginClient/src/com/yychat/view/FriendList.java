package com.yychat.view;

import com.yychat.ClientMain;
import com.yychat.control.YYchatClientConnectionUDP;
import com.yychat.model.Message;
import com.yychat.model.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * 好友列表窗口类，支持TCP和UDP协议
 * 对于UDP，需要通过ClientLoginUDP传递连接对象
 */

public class FriendList extends JFrame {
    private static String Name;
    private JLabel[] friendLabel;
    private JPanel friendListPanel;
    private JPanel friendPanel;
    private JPanel strangerListPanel;
    private JPanel strangerPanel;
    private static HashMap<String, FriendChat> friendChatMap = new HashMap<String,FriendChat>();
    private YYchatClientConnectionUDP udpConnection = null; // 保存UDP连接对象

    private CountDownLatch initializationLatch;
    private CountDownLatch friendListLatch;

    // 头像相关字段
    private HashMap<String, ImageIcon> avatarCache = new HashMap<String, ImageIcon>(); // 头像缓存
    private HashMap<String, JLabel> friendLabelMap = new HashMap<String, JLabel>();    // 好友标签映射

    public FriendList(String name) {
        Name = name;
        this.udpConnection = (YYchatClientConnectionUDP) ClientMain.getClient().getConnection();
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

        // 初始化陌生人列表（等待数据到达时更新）
        // 注意：不在这里初始化，等服务器发送数据时再更新
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
                        ((JButton)subComp).addActionListener(e -> {
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
                        ((JButton)subComp).addActionListener(e -> {
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
        this.setIconImage(loadWindowIcon().getImage());
        this.setTitle(Name + "的好友列表");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setBounds(800, 600, 350, 250);
    }

    /**
     * 设置窗口可见
     */
    private void setFrameVisible() {
        this.setVisible(true);
    }

    private JButton initAddFriendBTN() {
        JButton addFriendButton = new JButton("添加好友");

        addFriendButton.addActionListener(e -> {
            String newFriendName = JOptionPane.showInputDialog("请输入新好友的名字：");
            System.out.println("new friend:" + newFriendName);
            if (newFriendName != null) {
                Message message = new Message();
                message.setSender(Name);
                message.setReceiver("Server");
                message.setContent(newFriendName);
                message.setMessageType(MessageType.USER_ADD_NEW_FRIEND);
                try {
                    if (udpConnection != null) {
                        udpConnection.sendChatMessage(message);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        return addFriendButton;
    }

    /**
     * 创建通用的好友鼠标监听器
     */
    private MouseListener createFriendMouseListener(String friendName) {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getSource() instanceof JLabel) {
                    JLabel label = (JLabel) e.getSource();
                    String clickedFriendName = label.getText();
                    // UDP模式下创建聊天窗口
                    FriendChat chat = new FriendChat(Name, clickedFriendName, udpConnection);
                    friendChatMap.put(Name + "to" + clickedFriendName, chat);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

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
    private JLabel createFriendLabel(String friendName, int iconIndex, boolean isEnabled) {
        ImageIcon icon = loadDynamicFriendIcon(friendName, iconIndex);
        JLabel label = new JLabel(friendName, icon, JLabel.LEFT);
        if (!friendName.equals(Name)) {
            label.setEnabled(isEnabled);
        }
        label.addMouseListener(createFriendMouseListener(friendName));

        // 将标签添加到映射中，方便后续更新头像
        friendLabelMap.put(friendName, label);

        return label;
    }

    /**
     * 动态加载好友头像（优先从缓存或服务器获取）
     */
    private ImageIcon loadDynamicFriendIcon(String friendName, int iconIndex) {
        // 先检查缓存
        if (avatarCache.containsKey(friendName)) {
            return avatarCache.get(friendName);
        }

        // 从服务器获取头像路径
        String avatarPath = requestAvatarFromServer(friendName);
        if (avatarPath != null && !avatarPath.trim().isEmpty()) {
            ImageIcon icon = loadIconFromPath(avatarPath);
            if (icon != null) {
                avatarCache.put(friendName, icon);
                return icon;
            }
        }

        // 如果服务器获取失败，使用默认图标
        return loadFriendIcon(iconIndex);
    }

    /**
     * 从服务器请求用户头像路径
     */
    private String requestAvatarFromServer(String userName) {
        try {
            Message message = new Message();
            message.setMessageType(MessageType.REQUEST_AVATAR);
            message.setSender(Name);
            message.setContent(userName);
            message.setReceiver("Server");
            udpConnection.sendChatMessage(message);
            // 注意：这里应该有一个等待响应的机制，简化实现直接返回默认路径
            return "0.jpg"; // 默认头像
        } catch (Exception e) {
            System.err.println("请求用户 " + userName + " 头像失败: " + e.getMessage());
            return "0.jpg"; // 返回默认头像
        }
    }

    /**
     * 从文件路径加载图标
     */
    private ImageIcon loadIconFromPath(String avatarPath) {
        try {
            // 处理不同类型的路径
            String fullPath;
            if (avatarPath.startsWith("avatars/")) {
                // 自定义头像：使用完整路径
                fullPath = avatarPath;
            } else if (avatarPath.startsWith("/") || avatarPath.contains(":")) {
                // 绝对路径
                fullPath = avatarPath;
            } else {
                // 默认头像：相对路径，添加res目录
                fullPath = "res/" + avatarPath;
            }

            return new ImageIcon(fullPath);
        } catch (Exception e) {
            System.err.println("[FriendList]无法加载头像: " + avatarPath);
            return null;
        }
    }

    /**
     * 更新好友头像
     */
    public void updateFriendAvatar(String friendName, String avatarPath) {
        JLabel friendLabel = friendLabelMap.get(friendName);
        if (friendLabel != null) {
            // 检查是否为自定义头像
            if (avatarPath.startsWith("avatars/")) {
                // 尝试直接加载本地自定义头像
                ImageIcon newIcon = loadIconFromPath(avatarPath);
                if (newIcon != null) {
                    friendLabel.setIcon(newIcon);
                    avatarCache.put(friendName, newIcon);
                    System.out.println("已更新好友 " + friendName + " 的头像为（本地）: " + avatarPath);
                } else {
                    // 本地文件不存在，尝试从服务器下载
                    requestAvatarDownloadFromServer(friendName, avatarPath);
                }
            } else {
                // 默认头像，直接加载
                ImageIcon newIcon = loadIconFromPath(avatarPath);
                if (newIcon != null) {
                    friendLabel.setIcon(newIcon);
                    avatarCache.put(friendName, newIcon);
                    System.out.println("已更新好友 " + friendName + " 的头像为（默认）: " + avatarPath);
                }
            }
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
     * 请求从服务器下载头像文件
     */
    private void requestAvatarDownloadFromServer(String friendName, String avatarPath) {
        try {
            if (udpConnection != null) {
                Message message = new Message();
                message.setMessageType(MessageType.REQUEST_AVATAR_DOWNLOAD);
                message.setSender(Name); // 当前用户
                message.setReceiver(friendName); // 好友的用户名
                message.setContent(avatarPath); // 头像路径

                udpConnection.sendChatMessage(message);
                System.out.println("请求下载好友 " + friendName + " 的头像: " + avatarPath);
            } else {
                System.err.println("UDP连接为空，无法请求头像下载");
            }
        } catch (Exception e) {
            System.err.println("请求头像下载失败: " + e.getMessage());
        }
    }

    /**
     * 处理头像下载完成后的回调
     */
    public void handleAvatarDownloaded(String friendName, String avatarPath) {
        // 延迟一下再尝试加载，确保文件已经保存完成
        javax.swing.Timer timer = new javax.swing.Timer(500, e -> {
            updateFriendAvatar(friendName, avatarPath);
        });
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * 加载好友图标
     */
    private ImageIcon loadFriendIcon(int iconIndex) {
        String iconPath = "res/" + iconIndex % 6 + ".jpg";
        try {
            return new ImageIcon(iconPath);
        } catch (Exception e) {
            System.err.println("无法加载好友图标: " + iconPath);
            return new ImageIcon(); // 返回空图标
        }
    }

    /**
     * 加载陌生人图标
     */
    private ImageIcon loadStrangerIcon() {
        try {
            return new ImageIcon("./res/tortoise.gif");
        } catch (Exception e) {
            System.err.println("无法加载陌生人图标");
            return new ImageIcon(); // 返回空图标
        }
    }

    /**
     * 加载窗口图标
     */
    private ImageIcon loadWindowIcon() {
        try {
            return new ImageIcon("./res/duck2.gif");
        } catch (Exception e) {
            System.err.println("无法加载窗口图标");
            return new ImageIcon(); // 返回空图标
        }
    }

    public void initStrangerPanel(java.util.List<String> strangers, boolean first) {
        if (first || waitingReady()) {
            if (strangers == null) {
                strangers = new java.util.ArrayList<>();
            }
            initializeStrangerPanel(strangers);
        } else {
            System.out.println("无法初始化陌生人列表");
        }
    }

    /**
     * 初始化陌生人面板
     */
    private void initializeStrangerPanel(java.util.List<String> strangers) {
        // 创建新的陌生人列表面板，使用单列布局
        strangerListPanel = new JPanel(new GridLayout(0, 1));
        ImageIcon strangerIcon = loadStrangerIcon();

        // 添加所有陌生人标签
        for (String stranger : strangers) {
            if (stranger != null && !stranger.trim().isEmpty()) {
                JLabel strangerLabel = new JLabel(stranger, strangerIcon, JLabel.LEFT);
                strangerListPanel.add(strangerLabel);
            }
        }

        addStrangerListToPanel();
    }

    /**
     * 将陌生人列表添加到面板
     */
    private void addStrangerListToPanel() {
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
    }

    public static FriendChat getFriendChat(String name){
        return friendChatMap.get(name);
    }

    public static HashMap<String, FriendChat> getFriendChatMap() {
        return friendChatMap;
    }

    public static String getUserName(){
        return Name;
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

    public void activeOnlineFriendIcon(java.util.List<String> onlineFriends){
        JLabel[] friendLabel = getFriendLabel();
        if(onlineFriends == null) return;
        for(String friendName : onlineFriends){
            for (JLabel jLabel : friendLabel) {
                if (jLabel != null && jLabel.getText().equals(friendName)) {
                    jLabel.setEnabled(true);
                }
            }
        }
    }
    public void activeNewOnlineFriendIcon(String s){
        JLabel[] friendLabel = getFriendLabel();
        if(!s.isEmpty() && friendLabel != null){
            for (JLabel jLabel : friendLabel) {
                if (jLabel != null && jLabel.getText().equals(s)) {
                    jLabel.setEnabled(true);
                }
            }
        }
    }

    public void addNewFriend(String friendName) {
        JLabel label = createFriendLabel(friendName, 2, false);
        friendListPanel.add(label);
        friendListPanel.revalidate();
        friendListPanel.repaint();
    }

    /**
     * 等待初始化完成，使用CountDownLatch替代忙等待
     */
    public boolean waitingReady() {
        try {
            if (!initializationLatch.await(20, java.util.concurrent.TimeUnit.SECONDS)) {
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
            if (isValidFriendName(friendName)) {
                friendLabel[i] = createFriendLabel(friendName, i, false);
                friendListPanel.add(friendLabel[i]);
            }
        }

        addFriendListToPanel();
    }

    /**
     * 验证好友名称是否有效
     */
    private boolean isValidFriendName(String friendName) {
        return friendName != null && !friendName.trim().isEmpty() && !friendName.equals(Name);
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

    /**
     * 安全执行操作，处理异常
     */
    private void safeExecute(Runnable operation, String operationName) {
        try {
            operation.run();
        } catch (Exception e) {
            System.err.println(operationName + " 执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取初始化状态
     */
    public boolean isInitialized() {
        return initializationLatch.getCount() == 0;
    }

    /**
     * 获取好友列表状态
     */
    public boolean isFriendListSet() {
        return friendListLatch.getCount() == 0;
    }

    /**
     * 验证组件状态
     */
    private boolean validateComponents() {
        return friendPanel != null && strangerPanel != null &&
               friendListPanel != null && udpConnection != null;
    }

    /**
     * 错误恢复机制
     */
    private void handleInitializationError(Exception e) {
        System.err.println("FriendList 初始化失败: " + e.getMessage());
        e.printStackTrace();

        // 尝试恢复基本功能
        try {
            friendPanel = new JPanel(new BorderLayout());
            friendPanel.add(new JLabel("好友列表加载失败", JLabel.CENTER), BorderLayout.CENTER);
            this.add(friendPanel);
        } catch (Exception recoveryError) {
            System.err.println("错误恢复失败: " + recoveryError.getMessage());
        }
    }

//    public void setOldFriendList(String friendListStr) {
//        String[] friendList = friendListStr.split(" ");
//        friendLabel = new JLabel[friendList.length];
//        for(int i=0;i<friendList.length;i++){
//            if(friendList[i] != null && !friendList[i].isEmpty()){
//                ImageIcon icon = new ImageIcon("res/"+ i%6 +".jpg");
//                friendLabel[i] = new JLabel(friendList[i],icon,JLabel.LEFT);
//                if(!friendLabel[i].getText().equals(getUserName())){
//                    friendLabel[i].setEnabled(false);
//                }
//                friendLabel[i].addMouseListener(new MouseListener() {
//                    public void mouseClicked(MouseEvent e) {
//                        if(e.getClickCount() == 2 && e.getSource() instanceof JLabel){
//                            JLabel label = (JLabel) e.getSource();
//                            String friendName = label.getText();
//                            FriendChat chat = new FriendChat(getUserName(),friendName);
//                            friendChatMap.put(getUserName() + "to" + friendName,chat);
//                        }
//                    }
//                    @Override
//                    public void mousePressed(MouseEvent e) {}
//                    @Override
//                    public void mouseReleased(MouseEvent e) {}
//                    @Override
//                    public void mouseEntered(MouseEvent e) {
//                        if(e.getSource() instanceof JLabel){
//                            JLabel label = (JLabel) e.getSource();
//                            label.setForeground(Color.red);
//                        }
//                    }
//                    @Override
//                    public void mouseExited(MouseEvent e) {
//                        if(e.getSource() instanceof JLabel){
//                            JLabel label = (JLabel) e.getSource();
//                            label.setForeground(Color.black);
//                        }
//                    }
//                });
//                friendListPanel.add(friendLabel[i]);
//            }
//        }
//        JScrollPane friendListScrollPane = new JScrollPane(friendListPanel);
//        friendPanel.add(friendListScrollPane, BorderLayout.CENTER);
//    }
}
