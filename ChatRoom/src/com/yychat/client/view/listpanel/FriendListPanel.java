package com.yychat.client.view.listpanel;

import cn.hutool.core.util.StrUtil;
import com.yychat.client.ClientMain;
import com.yychat.client.service.AvatarService;
import com.yychat.client.service.UserService;
import com.yychat.client.view.MainWindow;
import com.yychat.client.view.chat.FriendChat;
import com.yychat.common.model.ServiceResponse;
import com.yychat.common.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FriendListPanel extends JPanel {

    protected final JPanel listPanel;
    protected final MainWindow parent;
    protected JLabel[] friendLabels;
    private final CountDownLatch friendListLatch;

    public FriendListPanel(MainWindow mainWindow) {
        super(new BorderLayout());
        friendListLatch = new CountDownLatch(1);
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane friendScrollPane = new JScrollPane(listPanel);
        add(friendScrollPane, BorderLayout.CENTER);
        this.parent = mainWindow;

    }

    public JLabel[] getFriendLabel() {
        try {
            if (!friendListLatch.await(20, TimeUnit.SECONDS)) {
                System.out.println("好友列表获取超时");
                return new JLabel[0];
            }
            return friendLabels;
        } catch (InterruptedException e) {
            System.out.println("好友列表获取超时");
            return new JLabel[0];
        }
    }

    /**
     * 初始化好友列表面板
     */
    private void initializeFriendListPanel(java.util.List<User> friendList) {
        if (!this.parent.waitingReady()) {
            System.err.println("好友列表面板尚未初始化，跳过设置");
            return;
        }
        int friendListSize = friendList.size();
        listPanel.removeAll(); // 清除现有内容
        friendLabels = new JLabel[friendListSize];
        for (int i = 0; i < friendListSize; i++) {
            String friendName = friendList.get(i).getUserName();
            if (!StrUtil.isBlank(friendName)) {
                friendLabels[i] = AvatarService.createUserLabel(friendList.get(i), createFriendMouseListener());
                friendLabels[i].setAlignmentX(Component.LEFT_ALIGNMENT);
                listPanel.add(friendLabels[i]);
            }
        }

        listPanel.revalidate();
        listPanel.repaint();

        friendListLatch.countDown();
    }

    /**
     * 设置好友列表
     */
    public void setFriendList(java.util.List<User> friendList) {
        if (!this.parent.waitingReady()) {
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
     * 添加好友到列表
     */
    public void addNewFriend(User friend) {
        // 检查组件是否已初始化
        if (!this.parent.waitingReady()) {
            System.err.println("好友列表面板尚未初始化，跳过添加好友");
            return;
        }
        try {
            JLabel label = AvatarService.createUserLabel(friend, createFriendMouseListener());
            label.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(label);
            listPanel.revalidate();
            listPanel.repaint();
        } catch (Exception e) {
            System.err.println("添加好友时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void changeFriendIconStatus(String friendName, boolean status) {
        if (this.parent.waitingReady()) {
            Arrays.stream(getFriendLabel())
                    .filter(label -> label.getText().equals(friendName))
                    .findAny().ifPresent(label -> label.setEnabled(status));
        }
    }

    /**
     * 创建好友鼠标监听器
     */
    private MouseListener createFriendMouseListener() {
        User sender = ClientMain.getCurrentUser();
        FriendListPanel instance = this;
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
                        FriendChat chat = MainWindow.getFriendChatMap().get(chatKey);
                        if (chat != null) {
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
                        SwingUtilities.invokeLater(() -> new FriendChat(sender, optionalUser.getData(), chatKey));
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
     * 从好友列表中移除好友
     *
     * @param friendName 要移除的好友名
     */
    public void removeFriendFromList(String friendName) {
        if (!this.parent.waitingReady()) {
            System.err.println("好友列表面板尚未初始化，跳过移除好友");
            return;
        }

        try {
            // 找到并移除对应的JLabel
            Component[] components = listPanel.getComponents();
            for (Component component : components) {
                if (component instanceof JLabel) {
                    JLabel label = (JLabel) component;
                    if (friendName.equals(label.getText())) {
                        listPanel.remove(label);
                        listPanel.revalidate();
                        listPanel.repaint();

                        // 更新friendLabels数组
                        if (friendLabels != null) {
                            friendLabels = Arrays.stream(friendLabels)
                                    .filter(l -> l != label)
                                    .toArray(JLabel[]::new);
                        }
                        System.out.println("已从好友列表中移除好友: " + friendName);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("移除好友时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示好友右键菜单
     */
    private void showFriendContextMenu(MouseEvent e, String friendName) {
        JPopupMenu contextMenu = new JPopupMenu();
        // 删除好友菜单项
        JMenuItem deleteItem = new JMenuItem("删除好友");
        deleteItem.addActionListener(actionEvent -> {
            // 确认删除对话框
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "确定要删除好友 '" + friendName + "' 吗？\n此操作不可撤销。",
                    "确认删除好友",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (result == JOptionPane.YES_OPTION) {
                // 在后台线程执行删除操作
                ClientMain.backgroundThreadPool.execute(() -> {
                    try {
                        ServiceResponse<String> response = UserService.getInstance().removeFriend(friendName);
                        if (!response.isSuccess()) {
                            SwingUtilities.invokeLater(() ->
                                    JOptionPane.showMessageDialog(this,
                                            "删除好友时发生错误：" + response.getMessage(),
                                            "错误",
                                            JOptionPane.ERROR_MESSAGE)
                            );
                            return;
                        }
                        // 删除成功后刷新UI
                        SwingUtilities.invokeLater(() -> {
                            removeFriendFromList(friendName); // 刷新好友列表
                            JOptionPane.showMessageDialog(this,
                                    "删除好友成功",
                                    "成功",
                                    JOptionPane.INFORMATION_MESSAGE);
                        });
                    } catch (Exception ex) {
                        SwingUtilities.invokeLater(() ->
                                JOptionPane.showMessageDialog(this,
                                        "删除好友时发生错误：" + ex.getMessage(),
                                        "错误",
                                        JOptionPane.ERROR_MESSAGE)
                        );
                    }
                });
            }
        });

        contextMenu.add(deleteItem);
        contextMenu.show(e.getComponent(), e.getX(), e.getY());
    }
}
