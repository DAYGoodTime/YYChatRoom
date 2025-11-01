package com.yychat.common.util;

public class StringUtil {

    /**
     * 格式化字节大小
     */
    public static String formatFileSize(long size) {
        if(size <= 0) return "未知大小";
        String result = "";
        if (size < 1024) {
            result = size + " B";
        } else if (size < 1024 * 1024) {
            result = String.format("%.1f KB", size / 1024.0);
        } else {
            result = String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
        return result;
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名（不包含点）
     */
    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
}
