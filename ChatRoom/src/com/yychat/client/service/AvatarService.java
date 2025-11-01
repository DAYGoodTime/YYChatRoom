package com.yychat.client.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.yychat.client.ClientMain;
import com.yychat.common.model.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Optional;

import static com.yychat.common.model.Constant.*;

public class AvatarService {

    private static final AvatarService instance = new AvatarService();

    public static AvatarService getInstance() {
        return instance;
    }

    public static final int AVATAR_SIZE = 40; // 头像显示大小

    public ServiceResponse<?> updateAvatarToServer(String selectedAvatarPath) {
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
            return ServiceResponse.error("服务器失联");
        }
        if (!response.get().getJson().getBool("success", false)) {
            return ServiceResponse.error(response.get().getJson().getStr("message", "未知错误"));
        }
        return ServiceResponse.success(null);
    }

    /**
     * 加载用户头像 - 本地优先，如果本地没有则从服务端获取
     *
     * @param userName 用户名
     * @return 用户头像，如果获取失败返回null
     */
    public ImageIcon loadUserAvatar(String userName) {
        return loadUserAvatar(userName, null);
    }

    /**
     * 加载用户头像 - 本地优先，如果本地没有则从服务端获取
     *
     * @param userName   用户名
     * @param targetPath 指定路径
     * @return 用户头像，如果获取失败返回null
     */
    public ImageIcon loadUserAvatar(String userName, String targetPath) {
        try {
            // 1. 首先尝试从CurrentUser获取头像地址（如果是当前用户）
            String avatarPath = targetPath == null ? getUserAvatarPath(userName) : targetPath;
            if (avatarPath == null) {
                System.out.println("用户 " + userName + " 头像路径为空，使用默认头像");
                avatarPath = DEFAULT_AVATAR_PATH + DEFAULT_AVATAR; // 默认头像
            }
            // 2. 尝试从本地加载头像
            ImageIcon icon = loadIconFromLocal(avatarPath);
            if (icon != null) {
                System.out.println("成功从本地加载用户 " + userName + " 的头像: " + avatarPath);
                return icon;
            }
            // 3. 本地没有，从服务端获取
            System.out.println("本地没有用户 " + userName + " 的头像，从服务端获取...");
            icon = loadIconFromServer(userName);
            if (icon != null) {
                System.out.println("成功从服务端获取用户 " + userName + " 的头像");
                return icon;
            }
            // 4. 服务端获取失败，返回默认头像
            System.out.println("无法获取用户 " + userName + " 的头像，使用默认头像");
            return loadIconFromLocal(DEFAULT_AVATAR_PATH + DEFAULT_AVATAR);

        } catch (Exception e) {
            System.err.println("加载用户 " + userName + " 头像时发生错误: " + e.getMessage());
            e.printStackTrace();
            // 发生错误时返回默认头像
            return loadIconFromLocal(DEFAULT_AVATAR_PATH + DEFAULT_AVATAR);
        }
    }

    /**
     * 从本地加载图标
     *
     * @param avatarPath 头像路径
     * @return 图标对象，如果加载失败返回null
     */
    private ImageIcon loadIconFromLocal(String avatarPath) {
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
     * 从服务端加载头像 - 使用封装的同步请求方法
     *
     * @param userName 用户名
     * @return 图标对象，如果加载失败返回null
     */
    private ImageIcon loadIconFromServer(String userName) {
        try {
            System.out.println("开始从服务端获取用户 " + userName + " 的头像...");
            Message message = Message.builder()
                    .setMessageType(Message.TCP_FILE_DOWNLOAD)
                    .setSender(ClientMain.getCurrentUserName())
                    .setReceiver(SystemUser.Server.getStr())
                    .setJsonMessage(new JSONObject().set("username", userName))
                    .setAttachmentType(AttachmentType.IMAGE_AVATAR);
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
            System.err.println("从服务端加载用户 " + userName + " 头像时发生错误: " + e.getMessage());
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
    private String getUserAvatarPath(String userName) {
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
    private String getUserAvatarPathFromServer(String userName) {
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

}
