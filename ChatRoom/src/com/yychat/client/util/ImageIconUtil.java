package com.yychat.client.util;

import com.yychat.common.model.Constant;

import javax.swing.*;

public class ImageIconUtil {

    public static final String DEFAULT_RES_PATH = "./res/";

    public static ImageIcon getStrangerIcon() {
        try {
            return new ImageIcon(DEFAULT_RES_PATH + "tortoise.gif");
        } catch (Exception e) {
            System.err.println("无法加载陌生人图标");
            return new ImageIcon(); // 返回空图标
        }
    }

    public static ImageIcon getWindowIcon() {
        try {
            return new ImageIcon(DEFAULT_RES_PATH + "duck2.gif");
        } catch (Exception e) {
            System.err.println("无法加载陌生人图标");
            return new ImageIcon(); // 返回空图标
        }
    }

    public static ImageIcon getDefaultIcon() {
        try {
            return new ImageIcon(Constant.DEFAULT_AVATAR_PATH + Constant.DEFAULT_AVATAR);
        } catch (Exception e) {
            System.err.println("无法加载默认图标");
            return new ImageIcon(); // 返回空图标
        }
    }
}
