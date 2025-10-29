package com.yychat.view;

import com.yychat.api.Connection;
import com.yychat.control.YYchatClientConnectionUDP;
import com.yychat.model.Message;
import com.yychat.model.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

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

        textArea.setForeground(Color.red);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        this.add(scrollPane, BorderLayout.CENTER);

        JTextField messageField = new JTextField(15);
        messageField.addKeyListener(this);

        sendButton.addActionListener(e -> {
            String msg = messageField.getText();
            if (msg.trim().isEmpty()) return;

            textArea.append(msg + "\n");
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
                textArea.append("消息发送失败: " + ex.getMessage() + "\n");
            }
        });

        sendButton.setForeground(Color.blue);

        JPanel sendPanel = new JPanel();
        sendPanel.add(messageField);
        sendPanel.add(sendButton);
        this.add(sendPanel, BorderLayout.SOUTH);

        this.setSize(350, 250);
        this.setLocationRelativeTo(null);
        this.setTitle(receiver + "聊天界面");
        try {
            this.setIconImage(new ImageIcon("./res/duck2.gif").getImage());
        } catch (Exception e) {
            System.out.println("无法加载图标文件");
        }
        this.setVisible(true);
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
        textArea.append(message.getTime().toString() + "\r\n" + message.getSender() + ": " + message.getContent() + "\r\n");
    }
}
