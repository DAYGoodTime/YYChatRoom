package com.yychat.view;

import com.yychat.api.Connection;
import com.yychat.control.YYchatClientConnectionUDP;
import com.yychat.model.Message;
import com.yychat.model.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FriendChat extends JFrame implements KeyListener {
    private JButton sendButton = new JButton("发送");
    private JTextArea textArea = new JTextArea();
    private String sender;
    private String receiver;
    private YYchatClientConnectionUDP udpConnection = null; // UDP连接对象

    // UDP构造函数
    public FriendChat(String sender, String receiver,Connection udpConnection) {
        this.sender = sender;
        this.receiver = receiver;
        this.udpConnection = (YYchatClientConnectionUDP) udpConnection;

        // 设置文本区域为支持多色显示
        textArea.setEditable(false);
        textArea.setBackground(Color.WHITE);
        textArea.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        this.add(scrollPane, BorderLayout.CENTER);

        JTextField messageField = new JTextField(15);
        messageField.addKeyListener(this);

        sendButton.addActionListener(e -> {
            String msg = messageField.getText();
            if (msg.trim().isEmpty()) return;

            // 使用新的格式化方法显示发送的消息
            appendSendMessage(msg, sender);
            messageField.setText("");

            Message message = new Message();
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setMessageType(MessageType.COMMON_CHAT_MESSAGE);
            message.setContent(msg);

            try {
                if (this.udpConnection != null) {
                    this.udpConnection.sendChatMessage(message);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                // 错误消息使用红色
                textArea.setForeground(Color.RED);
                textArea.append("消息发送失败: " + ex.getMessage() + "\n");
                textArea.setForeground(Color.BLACK);
            }
        });

        sendButton.setForeground(Color.blue);

        JPanel sendPanel = new JPanel();
        sendPanel.add(messageField);
        sendPanel.add(sendButton);
        this.add(sendPanel, BorderLayout.SOUTH);

        this.setSize(480, 360);
        this.setLocationRelativeTo(null);
        this.setTitle( "与 " + receiver + " 的聊天界面");
        try {
            this.setIconImage(new ImageIcon("./res/duck2.gif").getImage());
        } catch (Exception e) {
            System.out.println("无法加载图标文件");
        }

        // 在setVisible(true)之前添加
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setResizable(true);

        // 优化文本区域滚动
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        this.setVisible(true);
    }

    /**
     * 格式化发送消息显示
     * @param message 消息内容
     * @param sender 发送者
     */
    private void appendSendMessage(String message, String sender) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // 添加时间（灰色）
        textArea.append("[" + currentTime + "]\n");
        textArea.setForeground(Color.GRAY);
        textArea.append("我");

        // 添加消息内容（蓝色）
        textArea.append(": ");
        textArea.setForeground(Color.BLUE);
        textArea.append(message + "\n");

        // 重置为默认颜色以便后续操作
        textArea.setForeground(Color.BLACK);
    }

    /**
     * 格式化接收消息显示
     * @param message 消息对象
     */
    private void appendReceiveMessage(Message message) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String currentTime = sdf.format(new Date());

        // 添加时间（灰色）
        textArea.append("[" + currentTime + "]\n");
        textArea.setForeground(Color.GRAY);

        // 添加发送者名称（绿色）
        textArea.append(message.getSender());

        // 添加消息内容（黑色）
        textArea.append(": ");
        textArea.setForeground(Color.BLACK);
        textArea.append(message.getContent() + "\n");

        // 重置为默认颜色
        textArea.setForeground(Color.BLACK);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            sendButton.doClick();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
    public void append(Message message){
        appendReceiveMessage(message);
    }
}
