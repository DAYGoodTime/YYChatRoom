package com.yychat.client.view.listpanel;

import com.yychat.client.service.AvatarService;
import com.yychat.client.service.GroupService;
import com.yychat.client.util.ImageIconUtil;
import com.yychat.client.view.MainWindow;
import com.yychat.client.view.chat.GroupChat;
import com.yychat.common.model.Constant;
import com.yychat.common.model.Group;
import com.yychat.common.model.ServiceResponse;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 群组列表面板
 * 显示用户加入的所有群组，支持群组管理功能
 */
public class GroupListPanel extends JPanel {

    private final MainWindow parentWindow;
    private final GroupService groupService;
    private final Map<String, JLabel> groupLabelMap = new HashMap<>(); // 群组标签映射
    private final Map<String, Group> groupDataMap = new HashMap<>();   // 群组数据映射

    private JPanel groupListPanel;     // 群组列表面板
    private JPanel createGroupPanel;   // 创建群组面板
    private JPanel searchGroupPanel;   // 搜索群组面板
    private JButton createGroupButton; // 创建群组按钮
    private JButton searchGroupButton; // 搜索群组按钮
    private JTextField searchField;    // 搜索输入框


    private String selectedGroupAvatarPath;

    public GroupListPanel(MainWindow parentWindow) {
        this.parentWindow = parentWindow;
        this.groupService = GroupService.getInstance();
        initializeComponents();
        setupLayout();
        setupListeners();
        loadUserGroups();
    }

    /**
     * 初始化组件
     */
    private void initializeComponents() {
        setLayout(new BorderLayout());

        // 创建群组列表面板
        groupListPanel = new JPanel();
        groupListPanel.setLayout(new BoxLayout(groupListPanel, BoxLayout.Y_AXIS));
        groupListPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建搜索面板
        searchGroupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchGroupPanel.setBorder(new TitledBorder("搜索群组"));
        searchField = new JTextField(15);
        searchGroupButton = new JButton("搜索");
        searchGroupPanel.add(new JLabel("关键词:"));
        searchGroupPanel.add(searchField);
        searchGroupPanel.add(searchGroupButton);

        // 创建群组按钮面板
        createGroupPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        createGroupButton = new JButton("创建群组");
        createGroupPanel.add(createGroupButton);

        // 添加顶部面板
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(searchGroupPanel, BorderLayout.WEST);
        topPanel.add(createGroupPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);

        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(groupListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        // 显示提示信息
        showLoadingMessage("正在加载群组列表...");
    }

    /**
     * 设置布局
     */
    private void setupLayout() {
        // 布局已在initializeComponents中设置
    }

    /**
     * 设置事件监听器
     */
    private void setupListeners() {
        // 创建群组按钮监听器
        createGroupButton.addActionListener(e -> openCreateGroupDialog());

        // 搜索群组按钮监听器
        searchGroupButton.addActionListener(e -> searchGroups());

        // 搜索输入框回车监听器
        searchField.addActionListener(e -> searchGroups());
    }

    /**
     * 加载用户群组列表
     */
    private void loadUserGroups() {
        ServiceResponse<List<Group>> response = groupService.getUserGroups();
        if (response.isSuccess()) {
            List<Group> groups = response.getData();
            SwingUtilities.invokeLater(() -> updateGroupList(groups));
        } else {
            SwingUtilities.invokeLater(() -> showErrorMessage("获取群组列表失败: " + response.getMessage()));
        }
    }

    /**
     * 更新群组列表显示
     */
    public void updateGroupList(List<Group> groups) {
        groupListPanel.removeAll();
        groupLabelMap.clear();
        groupDataMap.clear();

        if (groups == null || groups.isEmpty()) {
            showEmptyMessage("暂无群组，点击上方按钮创建或加入群组");
            return;
        }

        for (Group group : groups) {
            groupDataMap.put(String.valueOf(group.getGroupId()), group);
            addGroupToList(group);
        }

        revalidate();
        repaint();
    }

    /**
     * 添加群组到列表
     */
    private void addGroupToList(Group group) {
        // 群组项主面板 - 使用更现代的布局
        JPanel groupItemPanel = new JPanel(new BorderLayout());
        groupItemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 12, 8, 12),  // 外边距
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230))  // 底部分割线
        ));
        groupItemPanel.setBackground(Color.WHITE);
        groupItemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        groupItemPanel.setMinimumSize(new Dimension(0, 75));
        groupItemPanel.setPreferredSize(new Dimension(0, 75));

        // 群组头像容器 - 使用圆形头像和阴影效果
        JPanel avatarContainer = new JPanel(new GridBagLayout());
        avatarContainer.setBackground(Color.WHITE);
        avatarContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));  // 头像右侧间距

        JLabel avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(48, 48));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);

        // 创建头像背景 - 圆形背景
        JPanel avatarBackground = new JPanel(new GridBagLayout());
        avatarBackground.setPreferredSize(new Dimension(48, 48));
        avatarBackground.setBackground(new Color(100, 149, 237));  // 群组头像背景色
        avatarBackground.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        avatarBackground.add(avatarLabel);

        avatarContainer.add(avatarBackground);
        loadGroupAvatar(avatarLabel, group);

        // 群组信息面板 - 改进布局和样式
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 0);

        // 群组名称 - 更大更突出
        JLabel nameLabel = new JLabel(group.getGroupName());
        nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 15));
        nameLabel.setForeground(new Color(33, 37, 41));  // 深灰色
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        infoPanel.add(nameLabel, gbc);

        // 群组统计信息 - 包含成员数量和在线状态
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statsPanel.setBackground(Color.WHITE);

        // 成员数量标签
        JLabel memberLabel = new JLabel(group.getMemberCount() + " 人");
        memberLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        memberLabel.setForeground(new Color(108, 117, 125));

        statsPanel.add(memberLabel);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        infoPanel.add(statsPanel, gbc);

        // 添加到群组项面板
        groupItemPanel.add(avatarContainer, BorderLayout.WEST);
        groupItemPanel.add(infoPanel, BorderLayout.CENTER);

        // 创建群组标签
        JLabel groupLabel = new JLabel();
        groupLabel.setLayout(new BorderLayout());
        groupLabel.add(groupItemPanel, BorderLayout.CENTER);

        // 设置标签属性 - 匹配新的面板高度
        groupLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        groupLabel.setMinimumSize(new Dimension(0, 75));
        groupLabel.setPreferredSize(new Dimension(0, 75));
        groupLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        groupLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 添加鼠标事件 - 改进的交互效果
        groupLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openGroupChat(group);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // 更现代的悬停效果
                groupItemPanel.setBackground(new Color(248, 249, 250)); // 浅灰背景
                groupItemPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(8, 12, 8, 12),  // 保持相同边距
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 123, 255))  // 顶部蓝色边框
                ));
                // 头像背景色变化
                avatarBackground.setBackground(new Color(70, 130, 180)); // 悬停时头像背景变深
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 恢复原始状态
                groupItemPanel.setBackground(Color.WHITE);
                groupItemPanel.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(8, 12, 8, 12),
                        BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230))  // 恢复灰色分割线
                ));
                avatarBackground.setBackground(new Color(100, 149, 237)); // 恢复原始头像背景
            }
        });

        // 添加到映射
        groupLabelMap.put(String.valueOf(group.getGroupId()), groupLabel);

        // 添加到列表面板
        groupListPanel.add(groupLabel);
    }

    /**
     * 加载群组头像 - 优化为圆形头像显示
     */
    private void loadGroupAvatar(JLabel avatarLabel, Group group) {
        try {
            ImageIcon avatarIcon = AvatarService.getInstance().loadGroupAvatar(group.getGroupName(), group.getGroupAvatarPath());
            if (avatarIcon != null) {
                // 将头像缩放为44x44以适应48x48的容器，留出2像素边距
                Image scaledImage = avatarIcon.getImage().getScaledInstance(44, 44, Image.SCALE_SMOOTH);
                avatarLabel.setIcon(new ImageIcon(scaledImage));
                avatarLabel.setText("");  // 清除文字
            } else {
                // 如果没有头像，显示默认群组标识
                avatarLabel.setText("群");
                avatarLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
                avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
                avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
                avatarLabel.setForeground(Color.WHITE);
            }
        } catch (Exception e) {
            System.err.println("加载群组头像失败: " + e.getMessage());
            avatarLabel.setText("群");
            avatarLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
            avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
            avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
            avatarLabel.setForeground(Color.WHITE);
        }
    }

    /**
     * 加载头像预览
     */
    private void loadAvatarPreview(JLabel previewLabel, String avatarPath) {
        try {
            File avatarFile = new File(avatarPath);
            if (avatarFile.exists()) {
                ImageIcon icon = new ImageIcon(avatarPath);
                // 缩放图片以适应预览区域
                Image scaledImage = icon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(scaledImage));
                previewLabel.setText("");
            } else {
                // 如果文件不存在，显示默认头像
                ImageIcon defaultIcon = ImageIconUtil.getDefaultIcon();
                Image scaledImage = defaultIcon.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                previewLabel.setIcon(new ImageIcon(scaledImage));
                previewLabel.setText("");
            }
        } catch (Exception e) {
            System.err.println("加载头像预览失败: " + e.getMessage());
            previewLabel.setIcon(null);
            previewLabel.setText("头像");
            previewLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
            previewLabel.setForeground(Color.GRAY);
        }
    }

    /**
     * 打开创建群组对话框
     */
    private void openCreateGroupDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "创建群组", true);
        dialog.setSize(450, 300);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel nameLabel = new JLabel("群组名称:");
        JTextField nameField = new JTextField(20);

        // 头像选择面板
        JPanel avatarPanel = new JPanel(new BorderLayout(10, 0));
        JLabel avatarLabel = new JLabel("群组头像:");
        JButton selectAvatarButton = new JButton("选择头像");
        JLabel avatarPreviewLabel = new JLabel();
        avatarPreviewLabel.setPreferredSize(new Dimension(60, 60));
        avatarPreviewLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        avatarPreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel avatarButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        avatarButtonPanel.add(selectAvatarButton);

        avatarPanel.add(avatarLabel, BorderLayout.WEST);
        avatarPanel.add(avatarButtonPanel, BorderLayout.CENTER);
        avatarPanel.add(avatarPreviewLabel, BorderLayout.EAST);

        // 设置布局
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        inputPanel.add(nameLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        inputPanel.add(nameField, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        inputPanel.add(avatarPanel, gbc);

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        JButton okButton = new JButton("创建");
        JButton cancelButton = new JButton("取消");
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // 默认使用系统头像
        selectedGroupAvatarPath = Constant.DEFAULT_AVATAR;
        loadAvatarPreview(avatarPreviewLabel, selectedGroupAvatarPath);

        // 头像选择按钮事件
        selectAvatarButton.addActionListener(e -> {
            String avatarPath = handelSelectAvatarButton(dialog);
            if (avatarPath != null) {
                selectedGroupAvatarPath = avatarPath;
                loadAvatarPreview(avatarPreviewLabel, avatarPath);
            }
        });

        // 事件监听
        okButton.addActionListener(e -> {
            String groupName = nameField.getText().trim();

            if (groupName.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "请输入群组名称", "警告", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // 异步创建群组
            ServiceResponse<Group> response = groupService.createGroup(groupName, selectedGroupAvatarPath);
            SwingUtilities.invokeLater(() -> {
                if (response.isSuccess()) {
                    JOptionPane.showMessageDialog(dialog, "群组创建成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    // 刷新群组列表
                    loadUserGroups();
                } else {
                    JOptionPane.showMessageDialog(dialog, "群组创建失败: " + response.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            });
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    public String handelSelectAvatarButton(JDialog dialog) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("选择群组头像");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "图片文件 (jpg, jpeg, png, gif)", "jpg", "jpeg", "png", "gif"));
        int result = fileChooser.showOpenDialog(dialog);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            // 验证文件大小 (5MB限制)
            long fileSize = selectedFile.length();
            if (fileSize > 5 * 1024 * 1024) {
                JOptionPane.showMessageDialog(dialog, "文件大小不能超过5MB", "错误", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            String fileName = selectedFile.getName();
            // 验证文件格式
            if (!fileName.matches(Constant.IMAGE_REX)) {
                JOptionPane.showMessageDialog(dialog, "不支持的文件格式，请选择jpg、jpeg、png或gif格式的图片", "错误", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return selectedFile.getAbsolutePath();
        }
        return null;
    }

    /**
     * 搜索群组
     */
    private void searchGroups() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入搜索关键词", "警告", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ServiceResponse<List<Group>> response = groupService.searchGroups(keyword);
        if (response.isSuccess()) {
            List<Group> groups = response.getData();
            if (groups != null && !groups.isEmpty()) {
                showSearchResults(groups);
            } else {
                showEmptyMessage("未找到相关群组");
            }
        } else {
            showErrorMessage("搜索群组失败: " + response.getMessage());
        }
    }

    /**
     * 显示搜索结果 - 复用群组列表布局，并添加右键功能
     */
    private void showSearchResults(List<Group> groups) {
        // 创建搜索结果对话框
        JDialog resultDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "搜索结果", true);
        resultDialog.setSize(600, 400);
        resultDialog.setLocationRelativeTo(this);
        resultDialog.setLayout(new BorderLayout());

        // 创建标题面板
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        titlePanel.setBorder(new EmptyBorder(10, 15, 5, 15));
        JLabel titleLabel = new JLabel("搜索结果 (" + groups.size() + " 个群组)");
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        titleLabel.setForeground(new Color(33, 37, 41));
        titlePanel.add(titleLabel);

        // 创建搜索结果列表面板（复用原有布局）
        JPanel searchListPanel = new JPanel();
        searchListPanel.setLayout(new BoxLayout(searchListPanel, BoxLayout.Y_AXIS));
        searchListPanel.setBorder(new EmptyBorder(0, 10, 10, 10));
        searchListPanel.setBackground(Color.WHITE);

        // 添加搜索结果项
        for (Group group : groups) {
            JPanel groupItemPanel = createSearchResultItem(group, resultDialog);
            searchListPanel.add(groupItemPanel);
            searchListPanel.add(Box.createVerticalStrut(5)); // 间距
        }

        // 如果没有搜索结果，显示提示信息
        if (groups.isEmpty()) {
            JLabel emptyLabel = new JLabel("未找到相关群组", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            emptyLabel.setForeground(Color.GRAY);
            emptyLabel.setPreferredSize(new Dimension(0, 100));
            searchListPanel.add(emptyLabel);
        }

        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(searchListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(new EmptyBorder(10, 15, 15, 15));
        JButton closeButton = new JButton("关闭");
        closeButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        closeButton.addActionListener(e -> resultDialog.dispose());
        buttonPanel.add(closeButton);

        // 添加到对话框
        resultDialog.add(titlePanel, BorderLayout.NORTH);
        resultDialog.add(scrollPane, BorderLayout.CENTER);
        resultDialog.add(buttonPanel, BorderLayout.SOUTH);

        resultDialog.setVisible(true);
    }

    /**
     * 创建搜索结果项 - 复用群组列表项的布局样式
     */
    private JPanel createSearchResultItem(Group group, JDialog parentDialog) {
        // 主面板 - 复用原有样式
        JPanel groupItemPanel = new JPanel(new BorderLayout());
        groupItemPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 12, 8, 12),
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230))
        ));
        groupItemPanel.setBackground(Color.WHITE);
        groupItemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
        groupItemPanel.setMinimumSize(new Dimension(0, 75));
        groupItemPanel.setPreferredSize(new Dimension(0, 75));

        // 头像容器 - 复用原有样式
        JPanel avatarContainer = new JPanel(new GridBagLayout());
        avatarContainer.setBackground(Color.WHITE);
        avatarContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 12));

        JLabel avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(48, 48));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);

        JPanel avatarBackground = new JPanel(new GridBagLayout());
        avatarBackground.setPreferredSize(new Dimension(48, 48));
        avatarBackground.setBackground(new Color(100, 149, 237));
        avatarBackground.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        avatarBackground.add(avatarLabel);

        avatarContainer.add(avatarBackground);
        loadGroupAvatar(avatarLabel, group);

        // 信息面板
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 0, 2, 0);

        // 群组名称
        JLabel nameLabel = new JLabel(group.getGroupName());
        nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 15));
        nameLabel.setForeground(new Color(33, 37, 41));
        nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        infoPanel.add(nameLabel, gbc);

        // 统计信息面板
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statsPanel.setBackground(Color.WHITE);

        JLabel memberLabel = new JLabel("👥 " + group.getMemberCount() + " 人");
        memberLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        memberLabel.setForeground(new Color(108, 117, 125));

        JLabel idLabel = new JLabel("ID: " + group.getGroupId());
        idLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        idLabel.setForeground(new Color(108, 117, 125));

        statsPanel.add(memberLabel);
        statsPanel.add(idLabel);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        infoPanel.add(statsPanel, gbc);

        // 添加到主面板
        groupItemPanel.add(avatarContainer, BorderLayout.WEST);
        groupItemPanel.add(infoPanel, BorderLayout.CENTER);

        // 添加悬停效果
        groupItemPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                groupItemPanel.setBackground(new Color(248, 249, 250));
                avatarBackground.setBackground(new Color(70, 130, 180));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                groupItemPanel.setBackground(Color.WHITE);
                avatarBackground.setBackground(new Color(100, 149, 237));
            }
        });

        // 添加右键菜单
        groupItemPanel.setComponentPopupMenu(createGroupContextMenu(group, parentDialog));

        return groupItemPanel;
    }

    /**
     * 创建群组右键菜单
     */
    private JPopupMenu createGroupContextMenu(Group group, JDialog parentDialog) {
        JPopupMenu contextMenu = new JPopupMenu();

        // 添加群组菜单项
        JMenuItem addGroupItem = new JMenuItem("加入群组");
        addGroupItem.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        addGroupItem.addActionListener(e -> joinGroup(group, parentDialog));

        // 查看群组信息菜单项
        JMenuItem infoItem = new JMenuItem("查看群组信息");
        infoItem.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        infoItem.addActionListener(e -> showGroupInfo(group));

        contextMenu.add(addGroupItem);
        contextMenu.addSeparator();
        contextMenu.add(infoItem);

        return contextMenu;
    }

    /**
     * 加入群组
     */
    private void joinGroup(Group group, JDialog parentDialog) {
        int result = JOptionPane.showConfirmDialog(parentDialog,
                "确定要加入群组 '" + group.getGroupName() + "' 吗？",
                "加入群组确认",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            try {
                ServiceResponse<Group> response = groupService.joinGroup(group.getGroupId());
                SwingUtilities.invokeLater(() -> {
                    if (response.isSuccess()) {
                        JOptionPane.showMessageDialog(parentDialog,
                                "成功加入群组 '" + group.getGroupName() + "'！",
                                "成功",
                                JOptionPane.INFORMATION_MESSAGE);
                        parentDialog.dispose();
                        // 刷新群组列表
                        loadUserGroups();
                    } else {
                        JOptionPane.showMessageDialog(parentDialog,
                                "加入群组失败: " + response.getMessage(),
                                "错误",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parentDialog,
                        "加入群组时发生错误: " + ex.getMessage(),
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 打开群组聊天窗口
     */
    private void openGroupChat(Group group) {
        // 后续实现群组聊天窗口
        GroupChat chat = MainWindow.getGroupChat(group.getGroupName());
        if (chat == null) {
            String title = "群聊:  " + group.getGroupName();
            chat = new GroupChat(title, group);
            //放入缓存中
            MainWindow.getGroupChatMap().put(group.getGroupName(), chat);
        }
        chat.highlightChatWindow();
    }

    /**
     * 显示群组信息
     */
    private void showGroupInfo(Group group) {
        String info = String.format(
                "群组信息:\n" +
                        "群组名称: %s\n" +
                        "群组ID: %d\n" +
                        "成员数量: %d人\n" +
                        "创建时间: %s",
                group.getGroupName(),
                group.getGroupId(),
                group.getMemberCount(),
                group.getCreateTime()
        );

        JOptionPane.showMessageDialog(this, info, "群组信息", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 显示加载消息
     */
    private void showLoadingMessage(String message) {
        groupListPanel.removeAll();
        JLabel loadingLabel = new JLabel(message, SwingConstants.CENTER);
        loadingLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        loadingLabel.setForeground(Color.GRAY);
        groupListPanel.add(loadingLabel);
        revalidate();
        repaint();
    }

    /**
     * 显示空消息
     */
    private void showEmptyMessage(String message) {
        groupListPanel.removeAll();
        JLabel emptyLabel = new JLabel(message, SwingConstants.CENTER);
        emptyLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        emptyLabel.setForeground(Color.GRAY);
        groupListPanel.add(emptyLabel);
        revalidate();
        repaint();
    }

    /**
     * 显示错误消息
     */
    private void showErrorMessage(String message) {
        groupListPanel.removeAll();
        JLabel errorLabel = new JLabel(message, SwingConstants.CENTER);
        errorLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        errorLabel.setForeground(Color.RED);
        groupListPanel.add(errorLabel);
        revalidate();
        repaint();
    }

    // Getter方法
    public MainWindow getParentWindow() {
        return parentWindow;
    }
}