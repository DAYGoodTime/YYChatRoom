package com.yychat.client.view.chat.group;

import cn.hutool.core.io.FileUtil;
import com.yychat.client.ClientMain;
import com.yychat.client.service.AvatarService;
import com.yychat.client.service.GroupService;
import com.yychat.client.service.MessageService;
import com.yychat.client.view.chat.BaseChatPanel;
import com.yychat.common.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Optional;

/**
 * 群聊面板组件
 * 继承 BaseChatPanel，实现新的群聊布局
 */
public class GroupChatPanel extends BaseChatPanel {


    private JPanel headerPanel;
    private JPanel contentPanel;
    private JPanel inputPanel;
    private JPanel groupInfoPanel;
    private JLabel groupAvatarLabel;
    private JLabel groupNameLabel;
    private JButton settingsButton;
    private JScrollPane messageScrollPane;
    private JPanel memberListPanel;
    private JList<GroupMember> memberList;
    private java.util.List<GroupMember> groupMemberList = new java.util.ArrayList<>();
    private DefaultListModel<GroupMember> memberListModel;
    private GroupChat parentFrame;

    public GroupChatPanel(User sender, GroupChat parentFrame) {
        super(sender, parentFrame);
        this.parentFrame = parentFrame;
        initGroupLayout();
    }

    protected Group getGroup() {
        return parentFrame.getGroup();
    }

    protected void setGroup(Group group) {
        parentFrame.setGroup(group);
    }

    /**
     * 初始化群聊布局
     */
    private void initGroupLayout() {
        // 设置面板为BorderLayout
        setLayout(new BorderLayout());

        // 创建各个区域
        createHeaderPanel();
        createContentPanel();

        // 添加到主面板
        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * 创建头部区域
     */
    private void createHeaderPanel() {
        headerPanel = new JPanel(new BorderLayout());
        headerPanel.setPreferredSize(new Dimension(0, 80));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 左侧：群组信息（头像 + 名称）
        groupInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        // 群聊头像
        groupAvatarLabel = new JLabel();
        groupAvatarLabel.setPreferredSize(new Dimension(50, 50));
        groupAvatarLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        groupAvatarLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 加载群组头像
        loadGroupAvatar();

        // 群聊名称
        groupNameLabel = new JLabel(getGroup().getGroupName());
        groupNameLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));
        groupNameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 添加点击事件
        groupAvatarLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openGroupSettingsDialog();
                }
            }
        });

        groupNameLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openGroupSettingsDialog();
                }
            }
        });

        groupInfoPanel.add(groupAvatarLabel);
        groupInfoPanel.add(groupNameLabel);

        // 右侧：设置按钮
        settingsButton = new JButton("设置");
        settingsButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        settingsButton.setPreferredSize(new Dimension(60, 30));
        settingsButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        settingsButton.addActionListener(e -> showSettingsMenu());

        headerPanel.add(groupInfoPanel, BorderLayout.WEST);
        headerPanel.add(settingsButton, BorderLayout.EAST);
    }

    /**
     * 创建内容区域
     */
    private void createContentPanel() {
        contentPanel = new JPanel(new BorderLayout());

        // 左侧：消息和输入区域（包含消息列表和输入框）
        JPanel leftPanel = new JPanel(new BorderLayout());

        // 消息区域
        messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        messageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        messageScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));

        // 输入区域（单独创建，不依赖于主面板的BorderLayout）
        createInputPanel();

        // 左侧面板添加消息区域和输入区域
        leftPanel.add(messageScrollPane, BorderLayout.CENTER);
        leftPanel.add(inputPanel, BorderLayout.SOUTH);

        // 右侧：成员列表
        createMemberListPanel();

        contentPanel.add(leftPanel, BorderLayout.CENTER);
        contentPanel.add(memberListPanel, BorderLayout.EAST);
    }

    /**
     * 创建成员列表面板
     */
    private void createMemberListPanel() {
        memberListPanel = new JPanel(new BorderLayout());
        memberListPanel.setPreferredSize(new Dimension(200, 0));

        // 成员列表标题
        JLabel memberListTitle = new JLabel("群成员 (" + getGroup().getMemberCount() + ")");
        memberListTitle.setFont(new Font("微软雅黑", Font.BOLD, 12));
        memberListTitle.setHorizontalAlignment(SwingConstants.CENTER);
        memberListTitle.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));

        // 成员列表
        memberListModel = new DefaultListModel<>();
        memberList = new JList<>(memberListModel);
        memberList.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        memberList.setCellRenderer(new MemberListCellRenderer());

        // 添加右键菜单
        memberList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMemberContextMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMemberContextMenu(e);
                }
            }
        });

        // 加载成员列表
        loadGroupMembers();

        // 创建带边框的滚动面板，与消息区域对齐
        JScrollPane memberScrollPane = new JScrollPane(memberList);
        memberScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

        // 添加到面板
        memberListPanel.add(memberListTitle, BorderLayout.NORTH);
        memberListPanel.add(memberScrollPane, BorderLayout.CENTER);
    }

    /**
     * 创建输入区域
     */
    private void createInputPanel() {
        // 创建输入面板（只包含消息输入区域）
        inputPanel = new JPanel(new BorderLayout());
        inputPanel.setPreferredSize(new Dimension(0, 60));

        // 输入框
        messageInputField = new JTextField();
        messageInputField.addKeyListener(this);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));

        // 发送按钮
        sendButton.setForeground(Color.blue);

        buttonPanel.add(sendButton);
        buttonPanel.add(sendFileButton);

        // 添加内边距，让输入区域与消息区域对齐
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 5));
        inputPanel.add(messageInputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
    }

    /**
     * 加载群组头像
     */
    private void loadGroupAvatar() {
        try {
            ImageIcon groupAvatar = AvatarService.loadGroupAvatar(getGroup().getGroupName(), getGroup().getGroupAvatarPath());
            if (groupAvatar != null) {
                Image scaledImage = groupAvatar.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                groupAvatarLabel.setIcon(new ImageIcon(scaledImage));
            } else {
                groupAvatarLabel.setText("群");
                groupAvatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
            }
        } catch (Exception e) {
            groupAvatarLabel.setText("群");
            groupAvatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        }
    }

    /**
     * 加载群组成员列表
     */
    private void loadGroupMembers() {
        // 在后台线程中加载成员列表
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    ServiceResponse<java.util.List<GroupMember>> response = GroupService.getInstance()
                            .getGroupMembers(getGroup().getGroupId());
                    if (response.isSuccess()) {
                        java.util.List<GroupMember> members = response.getData();
                        memberListModel.clear();
                        groupMemberList.clear();
                        groupMemberList.addAll(members);
                        SwingUtilities.invokeLater(() -> members.forEach(memberListModel::addElement));
                    }
                } catch (Exception e) {
                    System.err.println("加载群组成员列表失败: " + e.getMessage());
                }
                return null;
            }
        };
        worker.execute();
    }

    @Override
    protected ServiceResponse<Message> sendTextMessage(String text) {
        return MessageService.getInstance().sendPlainTextMessageToGroup(sender, getGroup(), text);
    }

    @Override
    protected void sendFileMessage(File file, String message) {
        if (selectedFile == null) return;
        try {
            ServiceResponse<Message> response = MessageService.getInstance().sendFileMessageToGroup(
                    sender, getGroup(), message, FileUtil.readBytes(file), file.getName());
            if (!response.isSuccess()) {
                appendErrorMessage("文件发送失败: " + response.getMessage());
                return;
            }
            Message responseMessage = response.getData();
            appendMessage(responseMessage, false);
            clearSelectedFile();
        } catch (Exception e) {
            appendErrorMessage("文件发送失败: " + e.getMessage());
        }
    }

    @Override
    protected ServiceResponse<Page<ChatMessage>> loadMessageFromHistory() {
        return MessageService.getInstance().queryGroupMessageHistory(getGroup(), ++chatHistoryIndex, chatHistoryPageSize);
    }


    /**
     * 显示设置菜单
     */
    private void showSettingsMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem groupInfoItem = new JMenuItem("群组信息设置");
        groupInfoItem.addActionListener(e -> openGroupSettingsDialog());
        menu.add(groupInfoItem);

        menu.addSeparator();

        JMenuItem leaveGroupItem = new JMenuItem("退出群聊");
        leaveGroupItem.addActionListener(e -> leaveGroup());
        menu.add(leaveGroupItem);

        menu.show(settingsButton, settingsButton.getWidth(), 0);
    }

    /**
     * 显示成员右键菜单
     */
    private void showMemberContextMenu(MouseEvent e) {
        int index = memberList.locationToIndex(e.getPoint());
        if (index >= 0) {
            GroupMember member = memberListModel.get(index);
            JPopupMenu menu = createMemberContextMenu(member);
            menu.show(memberList, e.getX(), e.getY());
        }
    }

    /**
     * 创建成员右键菜单
     */
    private JPopupMenu createMemberContextMenu(GroupMember member) {
        JPopupMenu menu = new JPopupMenu();
        String currentUsername = sender.getUserName();

        // 如果不是当前用户且不是好友，添加"加为好友"选项
        if (!member.getUsername().equals(currentUsername) && !isFriend(member.getUsername())) {
            JMenuItem addFriendItem = new JMenuItem("加为好友");
            addFriendItem.addActionListener(e -> addFriend(member.getUsername()));
            menu.add(addFriendItem);
        }

        // 如果当前用户有管理权限，添加管理选项
        if (currentUserIsAdmin() && !member.getUsername().equals(currentUsername)) {
            menu.addSeparator();

            // 设为管理员/取消管理员
            if (member.isMember()) {
                JMenuItem setAdminItem = new JMenuItem("设为管理员");
                setAdminItem.addActionListener(e -> setMemberAsAdmin(member.getUsername()));
                menu.add(setAdminItem);
            } else if (member.isAdmin()) {
                JMenuItem removeAdminItem = new JMenuItem("取消管理员");
                removeAdminItem.addActionListener(e -> removeMemberFromAdmin(member.getUsername()));
                menu.add(removeAdminItem);
            }

            // 移出群聊
            JMenuItem removeMemberItem = new JMenuItem("移出群聊");
            removeMemberItem.addActionListener(e -> removeMemberFromGroup(member.getUsername()));
            menu.add(removeMemberItem);
        }

        // 如果是群主，添加转让群主选项
        if (currentUserIsOwner() && !member.getUsername().equals(currentUsername)) {
            menu.addSeparator();
            JMenuItem transferOwnerItem = new JMenuItem("转让群主");
            transferOwnerItem.addActionListener(e -> transferGroupOwnership(member.getUsername()));
            menu.add(transferOwnerItem);
        }

        return menu;
    }

    // ================ 功能实现方法 ================

    private void openGroupSettingsDialog() {
        GroupSettingsDialog dialog = new GroupSettingsDialog(
                SwingUtilities.getWindowAncestor(this), getGroup());
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            String newGroupName = dialog.getGroupName();
            String newAvatarPath = dialog.getAvatarPath();
            Group newGroup = new Group(getGroup().getGroupId(), newGroupName, newAvatarPath, getGroup().getCreatorUsername());
            // 更新群组信息
            ServiceResponse<Group> response = GroupService.getInstance()
                    .updateGroupInfo(newGroup, ClientMain.getCurrentUserName());
            if (response.isSuccess()) {
                groupNameLabel.setText(newGroupName);
                setGroup(newGroup);
                loadGroupAvatar();
                JOptionPane.showMessageDialog(this, "群组信息更新成功", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "群组信息更新失败: " + response.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void leaveGroup() {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要退出群聊吗？",
                "确认退出",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            ServiceResponse<String> response = GroupService.getInstance()
                    .leaveGroup(getGroup().getGroupId());
            if (response.isSuccess()) {
                JOptionPane.showMessageDialog(this, "已成功退出群聊", "提示", JOptionPane.INFORMATION_MESSAGE);
                // 关闭窗口
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                    window.dispose();
                }
            } else {
                JOptionPane.showMessageDialog(this, "退出群聊失败: " + response.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void addFriend(String username) {
        ClientMain.getUDPConnection().sendMessageToServer(
                Message.builder()
                        .setMessageType(Message.USER_ADD_NEW_FRIEND)
                        .setSender(ClientMain.getCurrentUser().getUserName())
                        .setContent(username));
        JOptionPane.showMessageDialog(this, "已发送好友申请", "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setMemberAsAdmin(String username) {
        ServiceResponse<String> response = GroupService.getInstance().setGroupMemberIsAdmin(getGroup().getGroupId(), username, true);
        if (!response.isSuccess()) {
            JOptionPane.showMessageDialog(this, response.getMessage(), "设置管理员失败", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        loadGroupMembers();
        JOptionPane.showMessageDialog(this, "设置管理员成功", "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    private void removeMemberFromAdmin(String username) {
        ServiceResponse<String> response = GroupService.getInstance().setGroupMemberIsAdmin(getGroup().getGroupId(), username, false);
        if (!response.isSuccess()) {
            JOptionPane.showMessageDialog(this, response.getMessage(), "移除管理员失败", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        loadGroupMembers();
        JOptionPane.showMessageDialog(this, "移除管理员成功", "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    private void removeMemberFromGroup(String username) {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要将 " + username + " 移出群聊吗？",
                "确认移出",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            ServiceResponse<String> response = GroupService.getInstance()
                    .removeMember(getGroup().getGroupId(), username);

            if (response.isSuccess()) {
                loadGroupMembers(); // 重新加载成员列表
                JOptionPane.showMessageDialog(this, "已成功移出群聊", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "移除成员失败: " + response.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void transferGroupOwnership(String username) {
        int result = JOptionPane.showConfirmDialog(this,
                "确定要将群主转让给 " + username + " 吗？\n此操作不可撤销！",
                "确认转让",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            ServiceResponse<String> response = GroupService.getInstance()
                    .transferOwnership(getGroup().getGroupId(), username);

            if (response.isSuccess()) {
                loadGroupMembers(); // 重新加载成员列表
                JOptionPane.showMessageDialog(this, "群主转让成功", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "转让群主失败: " + response.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean currentUserIsOwner() {
        return groupMemberList.stream()
                .filter(u -> u.getUsername().equals(ClientMain.getCurrentUserName()))
                .findFirst().orElseGet(GroupMember::new).isOwner();
    }

    private boolean currentUserIsAdmin() {
        return groupMemberList.stream()
                .filter(u -> u.getUsername().equals(ClientMain.getCurrentUserName()))
                .findFirst().orElseGet(GroupMember::new).isAdmin();
    }

    private boolean isFriend(String username) {
        Optional<User> user = ClientMain.getFriendList().stream()
                .filter(u -> u.getUserName().equals(username)).findFirst();
        return user.isPresent();
    }
}
