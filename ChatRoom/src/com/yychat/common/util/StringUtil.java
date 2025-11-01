package com.yychat.common.util;

public class StringUtil {


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
}
