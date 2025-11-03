package com.yychat.client.view.listpanel;

import com.yychat.client.ClientMain;
import com.yychat.client.service.AvatarService;
import com.yychat.client.view.MainWindow;
import com.yychat.common.model.Message;
import com.yychat.common.model.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class StrangerListPanel extends JPanel {

    protected final JPanel listPanel;
    protected final MainWindow parent;

    public StrangerListPanel(MainWindow mainWindow) {
        super(new BorderLayout());
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane strangerScrollPane = new JScrollPane(listPanel);
        add(strangerScrollPane, BorderLayout.CENTER);
        this.parent = mainWindow;
    }

    /**
     * 初始化陌生人列表
     */
    public void updateStrangerPanel(java.util.List<User> strangers) {
        // 检查组件是否已初始化
        if (!parent.waitingReady()) {
            System.err.println("陌生人列表面板尚未初始化，跳过更新");
            return;
        }
        if (strangers == null) {
            strangers = new java.util.ArrayList<>();
        }
        // 清除现有内容
        listPanel.removeAll();
        // 添加所有陌生人标签
        for (User stranger : strangers) {
            if (stranger != null && !stranger.getUserName().trim().isEmpty()) {
                ImageIcon strangerIcon = AvatarService.loadUserAvatar(stranger);
                JLabel strangerLabel = new JLabel(stranger.getUserName(), strangerIcon, JLabel.LEFT);
                // 设置整行高亮效果 - 让标签占满整个可用宽度
                strangerLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                strangerLabel.setPreferredSize(new Dimension(0, 30)); // 高度30，宽度由布局管理器决定
                strangerLabel.setMinimumSize(new Dimension(0, 30));
                strangerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                // 为陌生人标签添加专门的鼠标监听器（支持添加好友功能）
                strangerLabel.addMouseListener(createStrangerMouseListener());
                listPanel.add(strangerLabel);
            }
        }
        // 刷新面板显示
        listPanel.revalidate();
        listPanel.repaint();

    }

    /**
     * 创建陌生人鼠标监听器
     */
    private MouseListener createStrangerMouseListener() {
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
        addFriendItem.addActionListener(actionEvent -> addStrangerAsFriend(strangerName));

        contextMenu.add(addFriendItem);
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
}
