package com.yychat.server.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.yychat.common.model.AttachmentType;
import com.yychat.common.model.Constant;
import com.yychat.common.model.Message;
import com.yychat.common.model.SystemUser;
import com.yychat.common.util.StringUtil;
import com.yychat.server.util.DBUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 头像文件管理系统
 * 负责服务器端头像文件的存储、管理和访问
 */
public class AvatarFileManager {
    private static final String AVATAR_BASE_DIR = "avatars";
    private static final String USER_AVATAR_DIR = "users";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");

    /**
     * 初始化头像目录结构
     */
    public static void initializeAvatarDirectories() {
        try {
            Path baseDir = Paths.get(AVATAR_BASE_DIR);
            Path userDir = baseDir.resolve(USER_AVATAR_DIR);

            // 创建必要的目录
            Files.createDirectories(baseDir);
            Files.createDirectories(userDir);

            System.out.println("头像目录结构初始化完成:");
            System.out.println("  基础目录: " + baseDir.toAbsolutePath());
            System.out.println("  默认头像目录: " + Paths.get(Constant.DEFAULT_AVATAR_PATH).toAbsolutePath());
            System.out.println("  用户头像目录: " + userDir.toAbsolutePath());

            // 检查默认头像文件是否存在
            checkDefaultAvatars();

        } catch (IOException e) {
            System.err.println("初始化头像目录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 检查默认头像文件
     */
    private static void checkDefaultAvatars() {
        String[] defaultAvatars = {"0.jpg", "1.jpg", "2.jpg", "3.jpg", "4.jpg", "5.jpg"};
        Path defaultDir = Paths.get(Constant.DEFAULT_AVATAR_PATH);

        for (String avatarFile : defaultAvatars) {
            Path avatarPath = defaultDir.resolve(avatarFile);
            if (!Files.exists(avatarPath)) {
                System.out.println("警告: 默认头像文件不存在: " + avatarPath);
            }
        }
    }


    /**
     * 保存头像文件（从字节数据）
     *
     * @param userName   用户名
     * @param avatarData 头像字节数据
     * @param fileName   文件名
     * @return 保存后的文件路径，如果失败返回null
     */
    public static String saveUserAvatarFromBytes(String userName, byte[] avatarData, String fileName) {
        try {
            // 验证数据
            if (avatarData == null || avatarData.length == 0) {
                System.err.println("头像数据为空");
                return null;
            }

            if (avatarData.length > MAX_FILE_SIZE) {
                System.err.println("头像文件大小超过限制: " + avatarData.length + " bytes");
                return null;
            }

            // 验证文件扩展名
            String extension = StringUtil.getFileExtension(fileName);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                System.err.println("不支持的文件格式: " + extension);
                return null;
            }

            // 生成唯一文件名
            String uniqueFileName = userName + "_" + System.currentTimeMillis() + "." + extension;

            // 用户头像目录
            Path userDir = Paths.get(AVATAR_BASE_DIR, USER_AVATAR_DIR);
            Files.createDirectories(userDir);

            // 目标文件路径
            Path targetPath = userDir.resolve(uniqueFileName);

            // 写入文件
            Files.write(targetPath, avatarData);

            String avatarPath = Constant.USER_CUSTOM_AVATAR_PATH + uniqueFileName;

            //写入数据库
            if (!DBUtil.updateUserAvatar(userName, avatarPath)) {
                System.out.println("数据库保存失败");
                return null;
            }
            System.out.println("用户 " + userName + " 头像已保存: " + avatarPath);

            return avatarPath;

        } catch (IOException e) {
            System.err.println("保存用户头像失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static String saveUserAvatarWithDefaultAvatar(String userName, String fileName) {
        //写入数据库
        if (!DBUtil.updateUserAvatar(userName, fileName)) {
            System.out.println("数据库保存失败");
            return null;
        }
        System.out.println("用户 " + userName + " 头像已保存: " + fileName);
        return fileName;
    }

    public static Message handelUserAvatarDownload(Message message) {
        Message response = Message.builder()
                .setMessageType(message.getMessageType())
                .setSender(SystemUser.Server.getStr())
                .setReceiver(message.getSender());
        JSONObject json = new JSONObject();
        if (!message.isJsonMessage()) {
            json.set("success", false);
            json.set("message", "无效的请求");
            response.setJsonMessage(json);
            return response;
        }
        String username = message.getJson().getStr("username", "");
        String avatarPath = DBUtil.getUserAvatar(username);
        Optional<byte[]> image = AvatarFileManager.getUserAvatarFromBytes(username, avatarPath);
        if (!image.isPresent()) {
            json.set("success", false);
            json.set("message", "服务器无法获取此头像");
            response.setJsonMessage(json);
            return response;
        }
        json.set("success", true);
        json.set("filename", avatarPath);
        response.setJsonMessage(json);
        response.setAttachment(image.get(), byte[].class, AttachmentType.IMAGE_AVATAR);
        return response;
    }

    private static Optional<byte[]> getUserAvatarFromBytes(String userName, String avatarPath) {

        if (avatarPath.contains(Constant.DEFAULT_AVATAR_PATH)) {
            //默认头像，返回空
            return Optional.empty();
        }
        String avatarRelativePath = avatarPath.replace(Constant.USER_CUSTOM_AVATAR_PATH, "");
        // 用户头像目录
        Path userDir = Paths.get(AVATAR_BASE_DIR, USER_AVATAR_DIR);
        // 目标文件路径
        Path targetPath = userDir.resolve(avatarRelativePath);
        File avatarFile = new File(targetPath.toString());
        if (!avatarFile.exists()) {
            System.out.println("从本地无法读取头像：" + avatarPath);
            return Optional.empty();
        }
        return Optional.of(FileUtil.readBytes(avatarFile));
    }

}