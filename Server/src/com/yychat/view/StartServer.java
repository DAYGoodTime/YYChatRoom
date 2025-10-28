package com.yychat.view;

import com.yychat.control.YYchatServer;

import javax.swing.*;
import java.awt.*;

public class StartServer extends JFrame {
    public StartServer(){
        JButton start = new JButton("启动服务器");
        start.setFont(new Font("宋体",Font.BOLD,25));
        start.addActionListener(e->{
            YYchatServer server = new YYchatServer();
        });


        JButton stop = new JButton("停止服务器");
        stop.setFont(new Font("宋体",Font.BOLD,25));

        this.setLayout(new GridLayout(1,2));
        this.add(start);
        this.add(stop);
        this.setSize(400,100);
        this.setLocationRelativeTo(null);
        this.setIconImage(new ImageIcon("./res/duck2.gif").getImage());
        this.setTitle("YYchat服务器");
        this.setVisible(true);
    }

    public static void main(String[] args) {
        new StartServer();
    }
}
