package com.yychat.control;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 头像文件管理系统
 * 负责服务器端头像文件的存储、管理和访问
 */
public class AvatarFileManager {
    private static final String AVATAR_BASE_DIR = "avatars";
    private static final String DEFAULT_AVATAR_DIR = "default";
    private static final String USER_AVATAR_DIR = "users";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif");

    /**
     * 初始化头像目录结构
     */
    public static void initializeAvatarDirectories() {
        try {
            Path baseDir = Paths.get(AVATAR_BASE_DIR);
            Path defaultDir = baseDir.resolve(DEFAULT_AVATAR_DIR);
            Path userDir = baseDir.resolve(USER_AVATAR_DIR);

            // 创建必要的目录
            Files.createDirectories(baseDir);
            Files.createDirectories(defaultDir);
            Files.createDirectories(userDir);

            System.out.println("头像目录结构初始化完成:");
            System.out.println("  基础目录: " + baseDir.toAbsolutePath());
            System.out.println("  默认头像目录: " + defaultDir.toAbsolutePath());
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
        Path defaultDir = Paths.get(AVATAR_BASE_DIR, DEFAULT_AVATAR_DIR);

        for (String avatarFile : defaultAvatars) {
            Path avatarPath = defaultDir.resolve(avatarFile);
            if (!Files.exists(avatarPath)) {
                System.out.println("警告: 默认头像文件不存在: " + avatarPath);
            }
        }
    }

    /**
     * 保存用户头像文件
     * @param userName 用户名
     * @param sourceFile 源文件
     * @param fileName 原始文件名
     * @return 保存后的文件路径，如果失败返回null
     */
    public static String saveUserAvatar(String userName, File sourceFile, String fileName) {
        try {
            // 验证文件
            if (!validateAvatarFile(sourceFile, fileName)) {
                return null;
            }

            // 生成唯一文件名
            String extension = getFileExtension(fileName);
            String uniqueFileName = userName + "_" + System.currentTimeMillis() + "." + extension;

            // 用户头像目录
            Path userDir = Paths.get(AVATAR_BASE_DIR, USER_AVATAR_DIR, userName);
            Files.createDirectories(userDir);

            // 目标文件路径
            Path targetPath = userDir.resolve(uniqueFileName);

            // 复制文件
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String relativePath = USER_AVATAR_DIR + "/" + userName + "/" + uniqueFileName;
            System.out.println("用户 " + userName + " 头像已保存: " + relativePath);

            return relativePath;

        } catch (IOException e) {
            System.err.println("保存用户头像失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 保存头像文件（从字节数据）
     * @param userName 用户名
     * @param avatarData 头像字节数据
     * @param fileName 文件名
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
            String extension = getFileExtension(fileName);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                System.err.println("不支持的文件格式: " + extension);
                return null;
            }

            // 生成唯一文件名
            String uniqueFileName = userName + "_" + System.currentTimeMillis() + "." + extension;

            // 用户头像目录
            Path userDir = Paths.get(AVATAR_BASE_DIR, USER_AVATAR_DIR, userName);
            Files.createDirectories(userDir);

            // 目标文件路径
            Path targetPath = userDir.resolve(uniqueFileName);

            // 写入文件
            Files.write(targetPath, avatarData);

            String relativePath = USER_AVATAR_DIR + "/" + userName + "/" + uniqueFileName;
            System.out.println("用户 " + userName + " 头像已保存: " + relativePath);

            return relativePath;

        } catch (IOException e) {
            System.err.println("保存用户头像失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取头像文件的绝对路径
     * @param avatarPath 相对路径
     * @return 绝对路径，如果文件不存在返回null
     */
    public static String getAvatarFilePath(String avatarPath) {
        if (avatarPath == null || avatarPath.trim().isEmpty()) {
            return getDefaultAvatarPath();
        }

        // 处理默认头像
        if (avatarPath.matches("^[0-5]\\.jpg$")) {
            return getDefaultAvatarPath(avatarPath);
        }

        // 处理用户头像
        Path fullPath = Paths.get(AVATAR_BASE_DIR, avatarPath);
        if (Files.exists(fullPath)) {
            return fullPath.toAbsolutePath().toString();
        }

        // 如果文件不存在，返回默认头像
        System.out.println("头像文件不存在: " + fullPath + "，使用默认头像");
        return getDefaultAvatarPath();
    }

    /**
     * 获取默认头像路径
     * @return 默认头像的绝对路径
     */
    public static String getDefaultAvatarPath() {
        return getDefaultAvatarPath("0.jpg");
    }

    /**
     * 获取指定默认头像路径
     * @param avatarFile 默认头像文件名
     * @return 默认头像的绝对路径
     */
    public static String getDefaultAvatarPath(String avatarFile) {
        Path defaultPath = Paths.get(AVATAR_BASE_DIR, DEFAULT_AVATAR_DIR, avatarFile);
        if (Files.exists(defaultPath)) {
            return defaultPath.toAbsolutePath().toString();
        }

        // 如果默认头像不存在，尝试从classpath加载
        try {
            return "res/" + avatarFile;
        } catch (Exception e) {
            System.err.println("无法加载默认头像: " + avatarFile);
            return null;
        }
    }

    /**
     * 删除用户头像文件
     * @param userName 用户名
     * @param avatarPath 头像路径
     * @return 删除是否成功
     */
    public static boolean deleteUserAvatar(String userName, String avatarPath) {
        try {
            if (avatarPath == null || avatarPath.trim().isEmpty()) {
                return false;
            }

            // 不允许删除默认头像
            if (avatarPath.matches("^[0-5]\\.jpg$")) {
                System.out.println("不允许删除默认头像: " + avatarPath);
                return false;
            }

            Path filePath = Paths.get(AVATAR_BASE_DIR, avatarPath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("已删除用户头像: " + avatarPath);
                return true;
            }

            return false;

        } catch (IOException e) {
            System.err.println("删除头像文件失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 验证头像文件
     * @param file 文件
     * @param fileName 文件名
     * @return 验证是否通过
     */
    private static boolean validateAvatarFile(File file, String fileName) {
        if (!file.exists() || !file.isFile()) {
            System.err.println("文件不存在或不是有效文件");
            return false;
        }

        if (file.length() > MAX_FILE_SIZE) {
            System.err.println("文件大小超过限制: " + file.length() + " bytes");
            return false;
        }

        String extension = getFileExtension(fileName);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            System.err.println("不支持的文件格式: " + extension);
            return false;
        }

        return true;
    }

    /**
     * 获取文件扩展名
     * @param fileName 文件名
     * @return 扩展名（不包含点）
     */
    private static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 获取头像文件的字节数据
     * @param avatarPath 头像路径
     * @return 头像字节数据，如果读取失败返回null
     */
    public static byte[] getAvatarBytes(String avatarPath) {
        try {
            String fullPath = getAvatarFilePath(avatarPath);
            if (fullPath == null) {
                return null;
            }

            Path filePath = Paths.get(fullPath);
            return Files.readAllBytes(filePath);

        } catch (IOException e) {
            System.err.println("读取头像文件失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 列出用户的所有头像文件
     * @param userName 用户名
     * @return 头像文件路径列表
     */
    public static List<String> listUserAvatars(String userName) {
        try {
            Path userDir = Paths.get(AVATAR_BASE_DIR, USER_AVATAR_DIR, userName);
            if (!Files.exists(userDir)) {
                return java.util.Collections.emptyList();
            }

            return Files.list(userDir)
                    .filter(Files::isRegularFile)
                    .map(path -> USER_AVATAR_DIR + "/" + userName + "/" + path.getFileName().toString())
                    .collect(java.util.stream.Collectors.toList());

        } catch (IOException e) {
            System.err.println("列出用户头像失败: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    /**
     * 清理孤立的头像文件（删除没有对应用户的头像）
     */
    public static void cleanupOrphanedAvatars() {
        try {
            Path userDir = Paths.get(AVATAR_BASE_DIR, USER_AVATAR_DIR);
            if (!Files.exists(userDir)) {
                return;
            }

            Files.walk(userDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String relativePath = userDir.relativize(path).toString();
                        // 这里可以添加更多逻辑来检查用户是否仍然存在
                        System.out.println("发现头像文件: " + relativePath);
                    });

        } catch (IOException e) {
            System.err.println("清理孤立头像文件失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试方法
     */
    public static void main(String[] args) {
        initializeAvatarDirectories();
        System.out.println("头像文件管理系统测试完成");

        // 列出默认头像
        String defaultPath = getDefaultAvatarPath();
        System.out.println("默认头像路径: " + defaultPath);

        // 测试清理功能
        cleanupOrphanedAvatars();
    }
}