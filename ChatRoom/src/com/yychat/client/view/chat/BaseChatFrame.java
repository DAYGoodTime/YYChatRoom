package com.yychat.client.view.chat;

import com.yychat.client.util.ImageIconUtil;
import com.yychat.common.model.User;

import javax.swing.*;
import java.awt.*;

/**
 * 聊天窗口框架类
 * 继承 JFrame，处理所有窗口相关操作
 * 子类需要实现 createChatPanel() 方法来添加具体的聊天面板
 */
public abstract class BaseChatFrame extends JFrame {

    protected User sender;
    protected String chatTitle;
    private int width;
    private int height;

    public BaseChatFrame(String title, User sender) {
        this.sender = sender;
        this.chatTitle = title;
    }
    public BaseChatFrame(String title, User sender,int width,int height) {
        this.sender = sender;
        this.chatTitle = title;
        this.width = width;
        this.height = height;
    }

    /**
     * 初始化窗口设置
     */
    protected void initializeFrame() {
        setTitle(chatTitle);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(true);
        setSize(width, height);
        setLocationRelativeTo(null);
        setIconImage(ImageIconUtil.getWindowIcon().getImage());

        // 在Swing事件线程中设置可见
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
        });
    }

    /**
     * 设置布局
     * 子类需要重写此方法来添加聊天面板
     */
    protected void setupLayout() {
        // 创建主面板，使用BorderLayout
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 添加聊天面板（由子类实现）
        JPanel chatPanel = createChatPanel();
        if (chatPanel != null) {
            mainPanel.add(chatPanel, BorderLayout.CENTER);
        }

        add(mainPanel);
    }

    /**
     * 创建聊天面板
     * 子类必须实现此方法
     * @return 聊天面板组件
     */
    protected abstract JPanel createChatPanel();

    /**
     * 高亮当前聊天窗口（从最小化状态恢复并激活）
     */
    public void highlightChatWindow() {
        // 使用SwingUtilities确保在EDT中执行UI操作
        SwingUtilities.invokeLater(() -> {
            try {
                // 1. 如果窗口被最小化，将其恢复
                if (getState() == JFrame.ICONIFIED) {
                    setState(JFrame.NORMAL);
                }
                // 2. 确保窗口可见
                if (!isVisible()) {
                    setVisible(true);
                }
                // 3. 将窗口移到前台
                toFront();
                // 4. 请求窗口获得焦点
                requestFocus();
            } catch (Exception e) {
                System.err.println("高亮聊天窗口时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 获取聊天标题
     */
    public String getChatTitle() {
        return chatTitle;
    }

    /**
     * 设置聊天标题
     */
    public void setChatTitle(String title) {
        this.chatTitle = title;
        setTitle(title);
    }

    /**
     * 获取发送者
     */
    public User getSender() {
        return sender;
    }
}