package com.yychat.client.view.chat.group;

import com.yychat.client.service.AvatarService;
import com.yychat.client.util.ImageIconUtil;
import com.yychat.common.model.Group;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 * 群组设置对话框
 * 允许用户修改群组名称和头像
 */
public class GroupSettingsDialog extends JDialog {

    private Group group;
    private JTextField groupNameField;
    private JLabel avatarLabel;
    private JButton changeAvatarButton;
    private String avatarPath = null;
    private boolean confirmed = false;

    public GroupSettingsDialog(Window parent, Group group) {
        super(parent, "群组设置", ModalityType.APPLICATION_MODAL);
        this.group = group;


        initializeComponents();
        setupLayout();
        setupListeners();
        loadCurrentSettings();

        pack();
        setLocationRelativeTo(parent);
    }

    private void initializeComponents() {
        groupNameField = new JTextField(20);
        groupNameField.setFont(new Font("微软雅黑", Font.PLAIN, 14));

        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(80, 80));
        avatarLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);

        changeAvatarButton = new JButton("更换头像");
        changeAvatarButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // 主面板
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // 群组名称
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        mainPanel.add(new JLabel("群组名称:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        mainPanel.add(groupNameField, gbc);

        // 群组头像
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        mainPanel.add(new JLabel("群组头像:"), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        JPanel avatarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        avatarPanel.add(avatarLabel);
        avatarPanel.add(changeAvatarButton);
        mainPanel.add(avatarPanel, gbc);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton confirmButton = new JButton("确定");
        confirmButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        confirmButton.setPreferredSize(new Dimension(80, 30));

        JButton cancelButton = new JButton("取消");
        cancelButton.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        cancelButton.setPreferredSize(new Dimension(80, 30));

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);

        // 添加事件监听器
        confirmButton.addActionListener(e -> {
            if (validateInput()) {
                confirmed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupListeners() {
        changeAvatarButton.addActionListener(e -> {
            String groupAvatarPath = AvatarService.handelSelectAvatarButton(this, "选择群组头像");
            if (groupAvatarPath != null) {
                avatarPath = groupAvatarPath;
                loadAvatarPreview();
            }
        });
    }

    private void loadCurrentSettings() {
        groupNameField.setText(group.getGroupName());
        loadAvatarPreview();
    }

    private void loadAvatarPreview() {
        try {
            ImageIcon avatar;
            if (avatarPath == null) {
                //没有选择，则加载原来的头像
                avatar = AvatarService.loadGroupAvatar(group.getGroupName(), group.getGroupAvatarPath());
            } else {
                avatar = new ImageIcon(avatarPath);
            }
            Image scaledImage = avatar.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
            avatarLabel.setIcon(new ImageIcon(scaledImage));
            avatarLabel.setText("");
        } catch (Exception e) {
            avatarLabel.setIcon(ImageIconUtil.getDefaultIcon());
            avatarLabel.setText("加载失败");
        }
    }

    private boolean validateInput() {
        String groupName = groupNameField.getText().trim();
        if (groupName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "群组名称不能为空", "错误", JOptionPane.ERROR_MESSAGE);
            groupNameField.requestFocus();
            return false;
        }

        if (groupName.length() > 50) {
            JOptionPane.showMessageDialog(this, "群组名称不能超过50个字符", "错误", JOptionPane.ERROR_MESSAGE);
            groupNameField.requestFocus();
            return false;
        }

        // 检查头像文件是否存在
        if (avatarPath != null && !new File(avatarPath).exists()) {
            JOptionPane.showMessageDialog(this, "头像文件不存在", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    public String getGroupName() {
        return groupNameField.getText().trim();
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}