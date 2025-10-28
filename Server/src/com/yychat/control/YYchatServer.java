package com.yychat.control;

import com.yychat.model.Message;
import com.yychat.model.MessageType;
import com.yychat.model.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YYchatServer {
    private static HashMap<String,Socket> userSocketMap = new HashMap<>();
    private static ExecutorService threadPool = Executors.newFixedThreadPool(10);

    public YYchatServer(){
        try{
            ServerSocket serverSocket = new ServerSocket(3456);
            System.out.println("服务器启动成功，正在监听3456端口...");
            while(true){
                Socket socket = serverSocket.accept();
                System.out.println("连接成功" + socket);
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                Message message = (Message) in.readObject();
                User user = (User) in.readObject();
                System.out.println("登录信息:\r\nuserName:" + user.getUserName()+",password:"+user.getPassword());


                message.setSender("Server");
                message.setReceiver(user.getUserName());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

                //登录请求
                if(message.getMessageType().equals(MessageType.USER_LOGIN_REQUEST)){
                    boolean loginSuccess = DBUtil.loginValidate(user.getUserName(), user.getPassword());
                    if(loginSuccess){
                        System.out.println("密码验证通过!");
                        message.setMessageType(MessageType.LOGIN_VALIDATE_SUCCESS);
                        out.writeObject(message);
                        userSocketMap.put(user.getUserName(), socket);
                        new ServerReceiverThread(scoket).start();
                        System.out.println("启动线程成功！");
                    }
                    else{
                        System.out.println("密码验证失败!");
                        message.setMessageType(MessageType.LOGIN_VALIDATE_FAILURE);
                        out.writeObject(message);
                        socket.close();
                    }
                }
                //注册请求
                else if(message.getMessageType().equals(MessageType.USER_SIGNUP_REQUEST)){
                    int singupSuccess = -1;
                    if(DBUtil.hasUser(user.getUserName())){
                        //当前用户名已存在
                        System.out.println("当前用户名已被注册！");
                    }
                    else{
                        singupSuccess = DBUtil.addNewUser(user);
                    }
                    if(singupSuccess == 1){
                        message.setMessageType(MessageType.USER_SIGNUP_SUCCESS);
                    }
                    else{
                        message.setMessageType(MessageType.USER_SIGNUP_FAILURE);
                    }
                    out.writeObject(message);
                    socket.close();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    public static Socket getUserSocket(String userName){
        return (Socket) userSocketMap.get(userName);
    }
    public static HashMap<String,Socket> getUserSocketMap(){
        return userSocketMap;
    }
}
