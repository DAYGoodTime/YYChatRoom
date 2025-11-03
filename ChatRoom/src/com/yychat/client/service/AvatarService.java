package com.yychat.client.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.yychat.client.ClientMain;
import com.yychat.common.model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.HashMap;
import java.util.Optional;

import static com.yychat.common.model.Constant.*;

public class AvatarService {

    private static final AvatarService instance = new AvatarService();

    private static final HashMap<String, ImageIcon> avatarCache = new HashMap<>(); // 头像缓存
    private static final HashMap<String, JLabel> userLabelMap = new HashMap<>();    // 用户标签映射

    public static AvatarService getInstance() {
        return instance;
    }

    public static final int AVATAR_SIZE = 40; // 头像显示大小

    public ServiceResponse<?> updateAvatarToServer(String selectedAvatarPath) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        String username = ClientMain.getCurrentUserName();
        boolean isDefaultAvatar = selectedAvatarPath.contains(DEFAULT_AVATAR_PATH);
        File avatarfile = new File(selectedAvatarPath);
        JSONObject json = new JSONObject();
        json.set("username", username);
        json.set("is_default", isDefaultAvatar);
        json.set("filename", selectedAvatarPath);
        Message message = Message.builder()
                .setMessageType(Message.TCP_FILE_UPLOAD)
                .setSender(ClientMain.getCurrentUserName())
                .setReceiver(SystemUser.Server.getStr())
                .setJsonMessage(json)
                .setAttachment(FileUtil.readBytes(avatarfile), byte[].class, AttachmentType.IMAGE_AVATAR);
        Optional<Message> response = ClientMain.getTCPConnection().sendMessage(message);
        if (!response.isPresent()) {
            return serviceResponse.error("服务器失联");
        }
        if (!response.get().getJson().getBool("success", false)) {
            return serviceResponse.error(response.get().getJson().getStr("message", "未知错误"));
        }
        return serviceResponse.success("头像更新成功");
    }

    /**
     * 加载用户头像 - 本地优先，如果本地没有则从服务端获取
     *
     * @param userName   用户名
     * @param targetPath 指定路径
     * @return 用户头像，如果获取失败返回null
     */
    public static ImageIcon loadUserAvatar(String userName, String targetPath) {
        try {
            // 1. 首先尝试从CurrentUser获取头像地址（如果是当前用户）
            String avatarPath = targetPath == null ? getUserAvatarPath(userName) : targetPath;
            if (avatarPath == null) {
                System.out.println("用户 " + userName + " 头像路径为空，使用默认头像");
                avatarPath = DEFAULT_AVATAR_FULL_PATH; // 默认头像
            }
            // 2. 尝试从本地加载头像
            ImageIcon icon = loadIconFromLocal(avatarPath);
            if (icon != null) {
                System.out.println("成功从本地加载 " + userName + " 的头像: " + avatarPath);
                return icon;
            }
            // 3. 本地没有，从服务端获取
            System.out.println("本地没有用户 " + userName + " 的头像，从服务端获取...");
            icon = loadIconFromServer(userName, AvatarType.UserAvatar);
            if (icon != null) {
                System.out.println("成功从服务端获取用户 " + userName + " 的头像");
                return icon;
            }
            // 4. 服务端获取失败，返回默认头像
            System.out.println("无法获取用户 " + userName + " 的头像，使用默认头像");
            return loadIconFromLocal(DEFAULT_AVATAR_FULL_PATH);

        } catch (Exception e) {
            System.err.println("加载用户 " + userName + " 头像时发生错误: " + e.getMessage());
            e.printStackTrace();
            // 发生错误时返回默认头像
            return loadIconFromLocal(DEFAULT_AVATAR_FULL_PATH);
        }
    }
    /**
     * 加载用户头像 - 本地优先，如果本地没有则从服务端获取
     *
     * @param user 用户对象
     * @return 用户头像，如果获取失败返回null
     */
    public static ImageIcon loadUserAvatar(User user) {
        return loadUserAvatar(user.getUserName(),user.getAvatarPath());
    }

    /**
     * 加载用户头像 - 本地优先，如果本地没有则从服务端获取
     *
     * @param groupName  群组名称
     * @param targetPath 指定路径
     * @return 用户头像，如果获取失败返回null
     */
    public ImageIcon loadGroupAvatar(String groupName, String targetPath) {
        try {
            // 1. 首先尝试从CurrentUser获取头像地址（如果是当前用户）
            if (targetPath == null) {
                System.out.println("群 " + groupName + " 头像路径为空，使用默认头像");
                targetPath = DEFAULT_AVATAR_FULL_PATH; // 默认头像
            }
            // 2. 尝试从本地加载头像
            ImageIcon icon = loadIconFromLocal(targetPath);
            if (icon != null) {
                System.out.println("成功从本地加载群 " + groupName + " 的头像: " + targetPath);
                return icon;
            }
            // 3. 本地没有，从服务端获取
            System.out.println("本地没有群 " + groupName + " 的头像，从服务端获取...");
            icon = loadIconFromServer(groupName, AvatarType.GroupAvatar);
            if (icon != null) {
                System.out.println("成功从服务端获取用户 " + groupName + " 的头像");
                return icon;
            }
            // 4. 服务端获取失败，返回默认头像
            System.out.println("无法获取群 " + groupName + " 的头像，使用默认头像");
            return loadIconFromLocal(DEFAULT_AVATAR_FULL_PATH);

        } catch (Exception e) {
            System.err.println("加载群 " + groupName + " 头像时发生错误: " + e.getMessage());
            e.printStackTrace();
            // 发生错误时返回默认头像
            return loadIconFromLocal(DEFAULT_AVATAR_FULL_PATH);
        }
    }

    /**
     * 从本地加载图标
     *
     * @param avatarPath 头像路径
     * @return 图标对象，如果加载失败返回null
     */
    private static ImageIcon loadIconFromLocal(String avatarPath) {
        try {
            if (avatarPath == null || avatarPath.trim().isEmpty()) {
                return null;
            }
            ImageIcon icon = new ImageIcon(avatarPath);
            if (icon.getImage() != null && icon.getIconWidth() > 0) {
                // 缩放到合适大小
                Image scaledImage = icon.getImage().getScaledInstance(AVATAR_SIZE, AVATAR_SIZE, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            System.err.println("无法从本地加载头像路径 " + avatarPath + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * 从服务端加载头像
     *
     * @param targetName 用户名或群名
     * @return 图标对象，如果加载失败返回null
     */
    private static ImageIcon loadIconFromServer(String targetName, AvatarType type) {
        try {
            Message message = Message.builder()
                    .setMessageType(Message.TCP_FILE_DOWNLOAD)
                    .setSender(ClientMain.getCurrentUserName())
                    .setReceiver(SystemUser.Server.getStr());
            switch (type) {
                case UserAvatar:
                    message = message
                            .setJsonMessage(new JSONObject().set("username", targetName))
                            .setAttachmentType(AttachmentType.IMAGE_AVATAR);
                    break;
                case GroupAvatar:
                    message = message
                            .setJsonMessage(new JSONObject().set("group_name", targetName))
                            .setAttachmentType(AttachmentType.GROUP_AVATAR);
                    break;

            }
            Optional<Message> response = ClientMain.getTCPConnection().sendMessage(message);
            if (!response.isPresent()) {
                System.out.println("服务端无法返回头像");
                return null;
            }
            if (!response.get().getJson().getBool("success", false)) {
                System.out.println("服务端无法返回头像" + response.get().getJson().getStr("message", "未知错误"));
                return null;
            }
            String filePath = response.get().getJson().getStr("filename");
            byte[] avatarData = response.get().getAttachment(byte[].class);
            if (filePath != null) {
                File avatarFile = new File(filePath);
                FileUtil.touch(avatarFile);
                FileUtil.writeBytes(avatarData, avatarFile);
                return new ImageIcon(avatarData);
            }
            return null;
        } catch (Exception e) {
            String typeName = (type.equals(AvatarType.UserAvatar)) ? "用户" : "群";
            System.err.println("从服务端加载 " + typeName + " 头像时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取用户头像路径
     *
     * @param userName 用户名
     * @return 头像路径，如果无法获取返回null
     */
    private static String getUserAvatarPath(String userName) {
        try {
            User currentUser = ClientMain.getCurrentUser();
            // 如果是当前用户，从CurrentUser获取头像路径
            if (currentUser.getUserName().equals(userName)) {
                return currentUser.getAvatarPath();
            }
            // 如果是其他用户，从服务端获取用户信息
//            return null;
            return getUserAvatarPathFromServer(userName);
        } catch (Exception e) {
            System.err.println("获取用户 " + userName + " 头像路径时发生错误: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从服务端获取用户头像路径
     *
     * @param userName 用户名
     * @return 头像路径，如果无法获取返回null
     */
    private static String getUserAvatarPathFromServer(String userName) {
        Message message = Message.builder()
                .setMessageType(Message.REQUEST_AVATAR_PATH)
                .setSender(ClientMain.getCurrentUserName())
                .setJsonMessage(new JSONObject().set("username", userName));
        // 5秒超时
        Message response = ClientMain.getUDPConnection().sendMessageToServerSync(message);
        if (response == null || !response.isJsonMessage()
                || response.getJson().getStr("avatarPath", null) == null) {
            System.out.println("无法获取用户头像地址");
            return null;
        }
        String avatarPath = response.getJson().getStr("avatarPath");
        System.out.println("成功从服务端获取用户 " + userName + " 的头像路径: " + avatarPath);
        return avatarPath;
    }

    public enum AvatarType {
        UserAvatar,
        GroupAvatar
    }


    /**
     * 创建通用的JLabel好友标签（增强版 - 支持动态头像加载和整行高亮）
     */
    public static JLabel createUserLabel(User user, MouseListener mouseListener) {
        String userName = user.getUserName();
        ImageIcon icon = loadUserAvatar(user);
        JLabel label = new JLabel(userName, icon, JLabel.LEFT);
        // 设置整行高亮效果 - 让标签占满整个可用宽度
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        label.setPreferredSize(new Dimension(0, 30)); // 高度30，宽度由布局管理器决定
        label.setMinimumSize(new Dimension(0, 30));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (mouseListener != null) {
            label.addMouseListener(mouseListener);
        }
        // 将标签添加到映射中，方便后续更新头像
        userLabelMap.put(userName, label);
        //创建的标签默认不启用
        label.setEnabled(false);
        return label;
    }

    /**
     * 更新用户头像
     */
    public static void updateUserAvatar(String userName, String avatarPath) {
        JLabel friendLabel = userLabelMap.get(userName);
        if (friendLabel != null) {
            ImageIcon newIcon = AvatarService.loadUserAvatar(userName, avatarPath);
            //更新缓存
            avatarCache.put(userName, newIcon);
            System.out.println("已更新用户 " + userName + " 的头像为: " + avatarPath);
            friendLabel.setIcon(newIcon);
        }
    }

}
