package com.yychat.client.view.chat.group;

import com.yychat.client.service.AvatarService;
import com.yychat.client.util.ImageIconUtil;
import com.yychat.common.model.GroupMember;

import javax.swing.*;
import java.awt.*;

/**
 * 成员列表渲染器
 */
public class MemberListCellRenderer extends JPanel implements ListCellRenderer<GroupMember> {

    private JLabel avatarLabel;
    private JLabel nameLabel;
    private JLabel roleLabel;
    private JLabel statusLabel;

    public MemberListCellRenderer() {
        setLayout(new BorderLayout(5, 2));
        setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(48, 48));
        avatarLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        nameLabel = new JLabel();
        nameLabel.setFont(new Font("微软雅黑", Font.PLAIN, 16));

        JPanel rightPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        roleLabel = new JLabel();
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusLabel = new JLabel();
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));

        rightPanel.add(roleLabel);
        rightPanel.add(statusLabel);

        add(avatarLabel, BorderLayout.WEST);
        add(nameLabel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends GroupMember> list, GroupMember member,
                                                  int index, boolean isSelected, boolean cellHasFocus) {

        nameLabel.setText(member.getUsername());

        ImageIcon avatar = AvatarService.loadUserAvatar(member.getUser());
        if(avatar == null) avatar = ImageIconUtil.getDefaultIcon();

        //设置头像
        avatarLabel.setIcon(avatar);

        // 设置角色标识
        if (member.isOwner()) {
            roleLabel.setText("👑 群主");
            roleLabel.setForeground(Color.ORANGE);
        } else if (member.isAdmin()) {
            roleLabel.setText("⭐ 管理员");
            roleLabel.setForeground(Color.GREEN);
        } else {
            roleLabel.setText("👤 成员");
            roleLabel.setForeground(Color.GRAY);
        }

        // 设置在线状态
        if (member.isOnline()) {
            statusLabel.setText("🟢 在线");
            statusLabel.setForeground(Color.GREEN);
        } else {
            statusLabel.setText("⚪ 离线");
            statusLabel.setForeground(Color.GRAY);
        }

        // 设置选中状态
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        return this;
    }
}