package com.yychat.view;

import com.yychat.api.Client;
import com.yychat.api.Connection;
import com.yychat.api.MessageThread;
import com.yychat.control.ClientReceiverThreadUDP;
import com.yychat.control.YYchatClientConnectionUDP;
import com.yychat.model.Message;
import com.yychat.model.MessageType;
import com.yychat.model.User;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * UDP版本的客户端登录界面
 * 支持UDP协议的无连接通信
 */
public class ClientLoginUDP extends JFrame implements Client {
    public static HashMap<String, FriendList> friendListHashMap = new HashMap<String, FriendList>();
    private static ClientReceiverThreadUDP thread = null;
    private YYchatClientConnectionUDP clientConnection;

    public ClientLoginUDP() {
        // 设置窗口标题
        setTitle("YY聊天 (UDP版本)");

        // 创建UI组件
        JLabel headImage = new JLabel(new ImageIcon("./res/head.gif"));
        this.add(headImage, "North");

        JLabel YYNumberText = new JLabel("YY号码：", JLabel.CENTER);
        JLabel YYPasswordText = new JLabel("YY密码：", JLabel.CENTER);
        JLabel ForgotPasswordTest = new JLabel("忘记密码", JLabel.CENTER);
        ForgotPasswordTest.setForeground(Color.blue);
        JLabel passwordProtectText = new JLabel("申请密码保护", JLabel.CENTER);

        JButton clearPassword = new JButton(new ImageIcon("./res/clear.gif"));
        JTextField numberTextBox = new JTextField();
        JPasswordField passwordTextBox = new JPasswordField();

        JCheckBox stealthLogin = new JCheckBox("隐身登录");
        JCheckBox rememberPassword = new JCheckBox("记住密码");

        // 协议选择面板
        JPanel protocolPanel = new JPanel(new FlowLayout());
        JRadioButton tcpRadioButton = new JRadioButton("TCP (可靠连接)", false);
        JRadioButton udpRadioButton = new JRadioButton("UDP (无连接)", true);
        ButtonGroup protocolGroup = new ButtonGroup();
        protocolGroup.add(tcpRadioButton);
        protocolGroup.add(udpRadioButton);
        protocolPanel.add(new JLabel("传输协议: "));
        protocolPanel.add(tcpRadioButton);
        protocolPanel.add(udpRadioButton);

        JPanel body = new JPanel(new GridLayout(4, 3));
        body.add(YYNumberText);
        body.add(numberTextBox);
        body.add(clearPassword);
        body.add(YYPasswordText);
        body.add(passwordTextBox);
        body.add(ForgotPasswordTest);
        body.add(stealthLogin);
        body.add(rememberPassword);
        body.add(passwordProtectText);
        body.add(new JLabel()); // 空单元格
        body.add(protocolPanel); // 协议选择
        body.add(new JLabel()); // 空单元格

        JTabbedPane loginMode = new JTabbedPane();
        loginMode.add(body, "YY号码");
        JPanel phoneNumberPanel = new JPanel();
        JPanel emailPanel = new JPanel();
        loginMode.add(phoneNumberPanel, "手机号码");
        loginMode.add(emailPanel, "电子邮箱");

        this.add(loginMode, "Center");

        JButton loginButton = new JButton(new ImageIcon("./res/login.gif"));
        JButton registerButton = new JButton(new ImageIcon("./res/register.gif"));
        JButton cancelButton = new JButton(new ImageIcon("./res/cancel.jpg"));
        JPanel buttonPanel = new JPanel();

        // UDP登录按钮事件
        loginButton.addActionListener(event -> {
            String name = numberTextBox.getText();
            String password = new String(passwordTextBox.getPassword());

            if (name.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "用户名和密码不能为空！");
                return;
            }

            User user = new User(name, password);

            try {
                // 创建UDP客户端连接
                clientConnection = new YYchatClientConnectionUDP();

                if (clientConnection.loginValidate(user)) {
                    // 登录成功后的处理
                    handleSuccessfulLogin(name, user);
                } else {
                    JOptionPane.showMessageDialog(this, "密码错误！请重新登录！");
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "UDP连接失败：" + e.getMessage());
            }
        });

        // UDP注册按钮事件
        registerButton.addActionListener(event -> {
            String name = numberTextBox.getText();
            String password = new String(passwordTextBox.getPassword());

            if (name.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "用户名和密码不能为空！");
                return;
            }

            User user = new User(name, password);

            try {
                // 创建UDP客户端连接进行注册
                YYchatClientConnectionUDP tempConnection = new YYchatClientConnectionUDP();

                if (tempConnection.userSignup(user)) {
                    JOptionPane.showMessageDialog(this, name + "UDP注册成功！");
                } else {
                    JOptionPane.showMessageDialog(this, name + "UDP注册失败！");
                }

                tempConnection.close();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "UDP注册失败：" + e.getMessage());
            }
        });

        // 取消按钮
        cancelButton.addActionListener(event -> {
            System.exit(0);
        });

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);

        this.add(buttonPanel, "South");

        // 设置窗口图标
        try {
            Image icon = new ImageIcon("./res/duck2.gif").getImage();
            this.setIconImage(icon);
        } catch (Exception e) {
            System.out.println("无法加载图标文件");
        }

        // 设置窗口属性
        this.setLocationRelativeTo(null);
        this.setSize(400, 300);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    /**
     * 处理成功登录后的逻辑
     */
    private void handleSuccessfulLogin(String username, User user) {
        try {
            // 创建好友列表窗口（UDP模式）
            FriendList friendList = new FriendList(username, "", true, clientConnection);
            friendListHashMap.put(username, friendList);

            // 请求在线好友
            clientConnection.requestOnlineFriends(username);

            // 通知服务器有新用户上线
            Message message = new Message();
            message.setSender(username);
            message.setReceiver("Server");
            message.setMessageType(MessageType.NEW_ONLINE_FRIEND);
            clientConnection.sendChatMessage(message);

            // 关闭登录窗口
            this.dispose();

            System.out.println("UDP用户 " + username + " 登录成功");

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "登录后处理失败：" + e.getMessage());
        }
    }

    @Override
    public JFrame getMainWindow() {
        return this;
    }

    @Override
    public MessageThread getMessageThread() {
        return thread;
    }

    @Override
    public Connection getConnection() {
        return clientConnection;
    }

    @Override
    public Map<String, FriendList> getFriendList() {
        return friendListHashMap;
    }
}