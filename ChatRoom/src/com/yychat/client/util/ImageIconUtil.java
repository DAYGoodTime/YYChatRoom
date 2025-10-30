package com.yychat.client.util;

import com.yychat.client.service.AvatarService;

import javax.swing.*;

public class ImageIconUtil {

    public static final String DEFAULT_RES_PATH = "./res/";

    public static ImageIcon getStrangerIcon(){
        try {
            return new ImageIcon(DEFAULT_RES_PATH + "tortoise.gif");
        } catch (Exception e) {
            System.err.println("无法加载陌生人图标");
            return new ImageIcon(); // 返回空图标
        }
    }
    public static ImageIcon getWindowIcon(){
        try {
            return new ImageIcon(DEFAULT_RES_PATH + "duck2.gif");
        } catch (Exception e) {
            System.err.println("无法加载陌生人图标");
            return new ImageIcon(); // 返回空图标
        }
    }
}
