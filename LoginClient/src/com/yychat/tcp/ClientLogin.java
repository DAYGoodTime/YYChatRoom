//package com.yychat.tcp;
//
//import com.yychat.tcp.ShutdownHook;
//import com.yychat.model.Message;
//import com.yychat.model.MessageType;
//import com.yychat.model.User;
//import com.yychat.view.FriendList;
//
//import javax.swing.*;
//import java.awt.*;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.net.Socket;
//import java.util.HashMap;
//
//public class ClientLogin extends JFrame{
//    public static HashMap<String, FriendList> friendListHashMap = new HashMap<String, FriendList>();
//    private static ClientReceiverThread thread = null;
//    public ClientLogin(){
//        JLabel headImage = new JLabel(new ImageIcon("./res/head.gif"));
//        this.add(headImage,"North");
//        JLabel YYNumberText = new JLabel("YY号码：", JLabel.CENTER);
//        JLabel YYPasswordText = new JLabel("YY密码：", JLabel.CENTER);
//        JLabel ForgotPasswordTest = new JLabel("忘记密码", JLabel.CENTER);
//        ForgotPasswordTest.setForeground(Color.blue);
//        JLabel passwordProtectText = new JLabel("申请密码保护", JLabel.CENTER);
//
//        JButton clearPassword = new JButton(new ImageIcon("./res/clear.gif"));
//        JTextField numberTextBox = new JTextField();
//        JPasswordField passwordTextBox = new JPasswordField();
//
//        JCheckBox stealthLogin = new JCheckBox("隐身登录");
//        JCheckBox rememberPassword = new JCheckBox("记住密码");
//
//        JPanel body = new JPanel(new GridLayout(3,3));
//        body.add(YYNumberText);
//        body.add(numberTextBox);
//        body.add(clearPassword);
//        body.add(YYPasswordText);
//        body.add(passwordTextBox);
//        body.add(ForgotPasswordTest);
//        body.add(stealthLogin);
//        body.add(rememberPassword);
//        body.add(passwordProtectText);
//
//        JTabbedPane loginMode = new JTabbedPane();
//        loginMode.add(body,"YY号码");
//        JPanel phoneNumberPanel = new JPanel();
//        JPanel emailPanel = new JPanel();
//        loginMode.add(phoneNumberPanel,"手机号码");
//        loginMode.add(emailPanel,"电子邮箱");
//
//        this.add(loginMode,"Center");
//
//        JButton loginButton = new JButton(new ImageIcon("./res/login.gif"));
//        JButton registerButton = new JButton(new ImageIcon("./res/register.gif"));
//        JButton cancelButton = new JButton(new ImageIcon("./res/cancel.jpg"));
//        JPanel buttonPanel = new JPanel();
//
//        //登录
//        loginButton.addActionListener(event -> {
//            String name = numberTextBox.getText();
//            String password = new String(passwordTextBox.getPassword());
//            User user = new User(name, password);
//            if(new YYchatClientConnection().loginValidate(user)){
//                Message message = new Message();
//                message.setSender(name);
//                message.setReceiver("Server");
//
//                message.setMessageType(MessageType.REQUEST_FRIEND_LIST);
//                sendMessage(YYchatClientConnection.getSocket(), message);
//                String friendListName = null;
//                try{
//                    ObjectInputStream in = new ObjectInputStream(YYchatClientConnection.getSocket().getInputStream());
//                    Message tmp = (Message) in.readObject();
//                    friendListName = tmp.getContent();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                friendListHashMap.put(name,new FriendList(name,friendListName));
//
//
//                thread = new ClientReceiverThread(YYchatClientConnection.getSocket());
//                thread.start();
//                message.setMessageType(MessageType.REQUEST_ONLINE_FRIENDS);
//                sendMessage(YYchatClientConnection.getSocket(), message);
//                message.setMessageType(MessageType.NEW_ONLINE_FRIEND);
//                sendMessage(YYchatClientConnection.getSocket(), message);
//                this.dispose();
//            }
//            else{
//                JOptionPane.showMessageDialog(this,"密码错误！请重新登录！");
//            }
//        });
//
//        registerButton.addActionListener(event -> {
//            String name = numberTextBox.getText();
//            String password = new String(passwordTextBox.getPassword());
//            User user = new User(name, password);
//            if(new YYchatClientConnection().userSignup(user)){
//                JOptionPane.showMessageDialog(this,name + "注册成功！");
//            }
//            else{
//                JOptionPane.showMessageDialog(this,name + "注册失败！");
//            }
//        });
//        buttonPanel.add(loginButton);
//        buttonPanel.add(registerButton);
//        buttonPanel.add(cancelButton);
//
//        this.add(buttonPanel,"South");
//
//        Image icon = new ImageIcon("./res/duck2.gif").getImage();
//        this.setIconImage(icon);
//
//        this.setLocationRelativeTo(null);
//        this.setSize(350,250);
//        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        this.setTitle("YY聊天");
//        this.setVisible(true);
//    }
//
//    private void sendMessage(Socket socket, Message message){
//        ObjectOutputStream out = null;
//        try{
//            out = new ObjectOutputStream(socket.getOutputStream());
//            out.writeObject(message);
//        }catch(Exception e){
//            e.printStackTrace();
//        }
//    }
//
//    public static ClientReceiverThread getClientReceiverThread() {
//        return thread;
//    }
//
//    public static void main(String[] args) {
//        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
//        ClientLogin frame = new ClientLogin();
//    }
//
//
//}
