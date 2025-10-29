package com.yychat.view;

import com.yychat.model.Message;
import com.yychat.model.MessageType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.HashMap;

/**
 * 好友列表窗口类，支持TCP和UDP协议
 * 对于UDP，需要通过ClientLoginUDP传递连接对象
 */

public class FriendList extends JFrame {
    final int FRIENDCOUNT = 50;
    final int STRANGERCOUNT = 20;
    private static String Name;
    JLabel[] friendLabel;
    JPanel friendListPanel = null;
    JPanel friendPanel = null;
    private static HashMap<String, FriendChat> friendChatMap = new HashMap<String,FriendChat>();
    private boolean isUdpMode = false;
    private Object udpConnection = null; // 保存UDP连接对象

    // TCP构造函数
    public FriendList(String name, String friendListName) {
        this(name, friendListName, false, null);
    }

    // UDP构造函数
    public FriendList(String name, String friendListName, boolean isUdpMode, Object udpConnection) {
        Name = name;
        this.isUdpMode = isUdpMode;
        this.udpConnection = udpConnection;
        friendPanel = new JPanel(new BorderLayout());
        JPanel addFriendPanel = new JPanel(new GridLayout(2,1));
        JButton friendButton1 = new JButton("我的好友");
        JButton strangerButton1 = new JButton("陌生人");
        JButton addFriendButton = new JButton("添加好友");

        addFriendButton.addActionListener(e->{
            String newFriendName = JOptionPane.showInputDialog("请输入新好友的名字：");
            System.out.println("new friend:" + newFriendName);
            if(newFriendName != null){
                Message message = new Message();
                message.setSender(Name);
                message.setReceiver("Server");
                message.setContent(newFriendName);
                message.setMessageType(MessageType.USER_ADD_NEW_FRIEND);
                try{
                    if(isUdpMode && udpConnection != null){
                        // UDP模式发送消息
                        com.yychat.control.YYchatClientConnectionUDP udpConn =
                            (com.yychat.control.YYchatClientConnectionUDP) udpConnection;
                        udpConn.sendChatMessage(message);
                    }
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        });

        addFriendPanel.add(addFriendButton);
        addFriendPanel.add(friendButton1);
        //TCP
        if(!isUdpMode)
            setOldFriendList(friendListName);

        JButton blackListButton1 = new JButton("黑名单");
        friendPanel.add(addFriendPanel, BorderLayout.NORTH);

        JPanel strangerBlackPanel = new JPanel(new GridLayout(2,1));
        strangerBlackPanel.add(strangerButton1);
        strangerBlackPanel.add(blackListButton1);
        friendPanel.add(strangerBlackPanel, BorderLayout.SOUTH);

        JPanel strangerPanel = new JPanel(new BorderLayout());
        JButton friendButton2 = new JButton("我的好友");
        JButton strangerButton2 = new JButton("陌生人");
        JPanel friendStrangerPanel = new JPanel(new GridLayout(2,1));
        friendStrangerPanel.add(friendButton2);
        friendStrangerPanel.add(strangerButton2);
        strangerPanel.add(friendStrangerPanel, BorderLayout.NORTH);

        JLabel[] strangerLabel = new JLabel[STRANGERCOUNT];
        JPanel strangerListPanel = new JPanel(new GridLayout(STRANGERCOUNT,1));
        for(int i = 0; i < STRANGERCOUNT; i++){
            ImageIcon icon = new ImageIcon("./res/tortoise.gif");
            strangerLabel[i] = new JLabel(i + "号陌生人",icon,JLabel.LEFT);
            strangerListPanel.add(strangerLabel[i]);
        }
        JScrollPane strangerListScrollPane = new JScrollPane(strangerListPanel);
        strangerPanel.add(strangerListScrollPane, BorderLayout.CENTER);

        JButton blackListButton2 = new JButton("黑名单");
        strangerPanel.add(blackListButton2, BorderLayout.SOUTH);

        CardLayout cardLayout = new CardLayout();
        this.setLayout(cardLayout);
        this.add(friendPanel, "Card1");
        this.add(strangerPanel, "Card2");
        cardLayout.show(this.getContentPane(),"Card1");

        strangerButton1.addActionListener(e -> cardLayout.show(this.getContentPane(),"Card2"));
        friendButton2.addActionListener(e -> cardLayout.show(this.getContentPane(),"Card1"));

        this.setIconImage(new ImageIcon("./res/duck2.gif").getImage());
        this.setTitle(name + "好友列表");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setBounds(800, 600,350,250);
        this.setVisible(true);
    }

    public static void main(String[] args) {
        FriendList friendList = new FriendList("abc"," ");
    }

    public static FriendChat getFriendChat(String name){
        return (FriendChat)friendChatMap.get(name);
    }

    public static String getUserName(){
        return Name;
    }

    public void activeOnlineFriendIcon(String s){
        if(!s.isEmpty()){
            String[] onlineFriendsName = s.split(" ");
            for (String string : onlineFriendsName) {
                for (JLabel jLabel : friendLabel) {
                    if (jLabel != null && jLabel.getText().equals(string)) {
                        jLabel.setEnabled(true);
                    }
                }
            }
        }
    }
    public void activeNewOnlineFriendIcon(String s){
        if(!s.isEmpty()){
            for (JLabel jLabel : friendLabel) {
                if (jLabel != null && jLabel.getText().equals(s)) {
                    jLabel.setEnabled(true);
                }
            }
        }
    }

    public void addNewFriend(String friendName){
        ImageIcon icon = new ImageIcon("res/"+ 2 +".jpg");
        JLabel label = new JLabel(friendName,icon,JLabel.LEFT);
        label.setEnabled(false);
        label.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() ==2 && e.getSource() instanceof JLabel){
                    JLabel label = (JLabel) e.getSource();
                    String friendName = label.getText();
                    if(isUdpMode && udpConnection != null){
                        // UDP模式下创建聊天窗口
                        FriendChat chat = new FriendChat(Name, friendName, true, udpConnection);
                        friendChatMap.put(Name + "to" + friendName, chat);
                    } else {
                        // TCP模式下创建聊天窗口
                        FriendChat chat = new FriendChat(Name, friendName);
                        friendChatMap.put(Name + "to" + friendName, chat);
                    }
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {}
            @Override
            public void mouseReleased(MouseEvent e) {}
            @Override
            public void mouseEntered(MouseEvent e) {
                if(e.getSource() instanceof JLabel){
                    JLabel label = (JLabel) e.getSource();
                    label.setForeground(Color.red);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                if(e.getSource() instanceof JLabel){
                    JLabel label = (JLabel) e.getSource();
                    label.setForeground(Color.black);
                }
            }
        });

        friendListPanel.add(label);
        friendListPanel.revalidate();
        friendListPanel.repaint();
    }

    /**
     * 设置好友列表
     */
    public void setFriendList(java.util.List<String> friendList) {
        if (friendList != null && !friendList.isEmpty()) {
            int friendListSize = friendList.size();
            // 清理现有的好友列表
            friendListPanel.removeAll();
            friendListPanel = new JPanel(new GridLayout(0,Math.max(friendListSize,1)));
            friendLabel = new JLabel[friendListSize];

            for(int i = 0; i < friendListSize; i++) {
                String friendName = friendList.get(i);
                if(friendName != null && !friendName.isEmpty()) {
                    ImageIcon icon = new ImageIcon("res/" + i % 6 + ".jpg");
                    friendLabel[i] = new JLabel(friendName, icon, JLabel.LEFT);
                    if(!friendLabel[i].getText().equals(Name)) {
                        friendLabel[i].setEnabled(false);
                    }
                    friendLabel[i].addMouseListener(new MouseListener() {
                        public void mouseClicked(MouseEvent e) {
                            if(e.getClickCount() == 2 && e.getSource() instanceof JLabel){
                                JLabel label = (JLabel) e.getSource();
                                String friendName = label.getText();
                                // UDP模式下创建聊天窗口
                                FriendChat chat = new FriendChat(Name, friendName, true, udpConnection);
                                friendChatMap.put(Name + "to" + friendName, chat);
                            }
                        }
                        @Override
                        public void mousePressed(MouseEvent e) {}
                        @Override
                        public void mouseReleased(MouseEvent e) {}
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            if(e.getSource() instanceof JLabel){
                                JLabel label = (JLabel) e.getSource();
                                label.setForeground(Color.red);
                            }
                        }
                        @Override
                        public void mouseExited(MouseEvent e) {
                            if(e.getSource() instanceof JLabel){
                                JLabel label = (JLabel) e.getSource();
                                label.setForeground(Color.black);
                            }
                        }
                    });
                    friendListPanel.add(friendLabel[i]);
                }
            }

            friendListPanel.revalidate();
            friendListPanel.repaint();
        }
    }

    public void setOldFriendList(String friendListStr) {
        String[] friendList = friendListStr.split(" ");
        friendLabel = new JLabel[friendList.length];
        for(int i=0;i<friendList.length;i++){
            if(friendList[i] != null && !friendList[i].isEmpty()){
                ImageIcon icon = new ImageIcon("res/"+ i%6 +".jpg");
                friendLabel[i] = new JLabel(friendList[i],icon,JLabel.LEFT);
                if(!friendLabel[i].getText().equals(getUserName())){
                    friendLabel[i].setEnabled(false);
                }
                friendLabel[i].addMouseListener(new MouseListener() {
                    public void mouseClicked(MouseEvent e) {
                        if(e.getClickCount() == 2 && e.getSource() instanceof JLabel){
                            JLabel label = (JLabel) e.getSource();
                            String friendName = label.getText();
                            FriendChat chat = new FriendChat(getUserName(),friendName);
                            friendChatMap.put(getUserName() + "to" + friendName,chat);
                        }
                    }
                    @Override
                    public void mousePressed(MouseEvent e) {}
                    @Override
                    public void mouseReleased(MouseEvent e) {}
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if(e.getSource() instanceof JLabel){
                            JLabel label = (JLabel) e.getSource();
                            label.setForeground(Color.red);
                        }
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        if(e.getSource() instanceof JLabel){
                            JLabel label = (JLabel) e.getSource();
                            label.setForeground(Color.black);
                        }
                    }
                });
                friendListPanel.add(friendLabel[i]);
            }
        }
        JScrollPane friendListScrollPane = new JScrollPane(friendListPanel);
        friendPanel.add(friendListScrollPane, BorderLayout.CENTER);
    }
}
