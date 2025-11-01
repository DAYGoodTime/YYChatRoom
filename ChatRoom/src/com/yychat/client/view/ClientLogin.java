package com.yychat.client.view;


import com.yychat.common.model.ServiceResponse;
import com.yychat.client.service.UserService;

import javax.swing.*;
import java.awt.*;

/**
 * UDP版本的客户端登录界面
 * 支持UDP协议的无连接通信
 */
public class ClientLogin extends JFrame {

    protected JLabel headImage;
    protected JPanel codeLoginBody; //yy号码登录面板
    protected JTextField yyCodeTextField;
    protected JPasswordField yyCodePasswordField;

    protected JTabbedPane loginModeTabPanel; //登录方式标签页

    protected JPanel actionPanel; //下方按钮
    protected JButton loginButton;
    protected JButton registerButton;
    protected JButton cancelButton;

    protected FriendList friendListWindow;

    public ClientLogin(boolean showWindow) {
        initBasicUI();
        initListener();

        // 设置窗口属性
        this.setLocationRelativeTo(null);
        this.setSize(400, 300);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(showWindow);
    }

    public void initBasicUI() {
        // 设置窗口标题
        setTitle("YY聊天");
        // 创建UI组件
        headImage = new JLabel(new ImageIcon("./res/head.gif"));
        this.add(headImage, "North");
        //主体Body
        JLabel YYNumberText = new JLabel("YY号码：", JLabel.CENTER);
        JLabel YYPasswordText = new JLabel("YY密码：", JLabel.CENTER);
        JLabel ForgotPasswordTest = new JLabel("忘记密码", JLabel.CENTER);
        ForgotPasswordTest.setForeground(Color.blue);
        JLabel passwordProtectText = new JLabel("申请密码保护", JLabel.CENTER);
        JButton clearPassword = new JButton(new ImageIcon("./res/clear.gif"));
        yyCodeTextField = new JTextField();
        yyCodePasswordField = new JPasswordField();
        JCheckBox stealthLogin = new JCheckBox("隐身登录");
        JCheckBox rememberPassword = new JCheckBox("记住密码");

        codeLoginBody = new JPanel(new GridLayout(4, 3));
        codeLoginBody.add(YYNumberText);
        codeLoginBody.add(yyCodeTextField);
        codeLoginBody.add(clearPassword);
        codeLoginBody.add(YYPasswordText);
        codeLoginBody.add(yyCodePasswordField);
        codeLoginBody.add(ForgotPasswordTest);
        codeLoginBody.add(stealthLogin);
        codeLoginBody.add(rememberPassword);
        codeLoginBody.add(passwordProtectText);
        codeLoginBody.add(new JLabel()); // 空单元格
        codeLoginBody.add(new JLabel()); // 空单元格

        //登录标签页
        loginModeTabPanel = new JTabbedPane();
        loginModeTabPanel.add(codeLoginBody, "YY号码");
        JPanel phoneNumberPanel = new JPanel();
        JPanel emailPanel = new JPanel();
        loginModeTabPanel.add(phoneNumberPanel, "手机号码");
        loginModeTabPanel.add(emailPanel, "电子邮箱");
        this.add(loginModeTabPanel, "Center");

        //下方按钮
        loginButton = new JButton(new ImageIcon("./res/login.gif"));
        registerButton = new JButton(new ImageIcon("./res/register.gif"));
        cancelButton = new JButton(new ImageIcon("./res/cancel.jpg"));

        actionPanel = new JPanel();
        actionPanel.add(loginButton);
        actionPanel.add(registerButton);
        actionPanel.add(cancelButton);
        this.add(actionPanel, "South");

        // 设置窗口图标
        try {
            Image icon = new ImageIcon("./res/duck2.gif").getImage();
            this.setIconImage(icon);
        } catch (Exception e) {
            System.out.println("无法加载图标文件");
        }
    }

    public void initListener() {
        // 登录按钮事件
        loginButton.addActionListener(event -> {
            String name = yyCodeTextField.getText();
            String password = new String(yyCodePasswordField.getPassword());
            login(name, password);
            // 关闭登录窗口
            this.dispose();
        });

        // 注册按钮事件
        registerButton.addActionListener(event -> {
            String name = yyCodeTextField.getText();
            String password = new String(yyCodePasswordField.getPassword());
            ServiceResponse<?> response = UserService.getInstance().registerUser(name, password);
            if (!response.isSuccess()) {
                JOptionPane.showMessageDialog(this, response.getMessage(), "注册失败", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(this, "注册成功");
        });
        // 取消按钮
        cancelButton.addActionListener(event -> {
            System.exit(0);
        });
    }

    public FriendList getFriendList() {
        return friendListWindow;
    }

    public void login(String name, String password) {
        UserService userService = UserService.getInstance();
        ServiceResponse<?> response = userService.loginByUserName(name, password);
        if (!response.isSuccess()) {
            JOptionPane.showMessageDialog(this, response.getMessage());
            return;
        }
        // 创建好友列表窗口
        friendListWindow = new FriendList();

        //请求好友列表
        userService.requestFriends();
        // 请求在线好友
        userService.requestOnlineFriends();
        //请求陌生人
        userService.requestUnknownFriends();
        // 通知服务器有新用户上线
        userService.broadcastNewFriendOnline();

        // 创建并显示用户信息窗口
//        MyInfo myInfo = new MyInfo(friendListWindow);
//        myInfo.setVisible(true);
    }

}