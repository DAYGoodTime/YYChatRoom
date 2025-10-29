package com.yychat.tcp;

import com.yychat.control.DBUtil;
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

@Deprecated
public class YYchatServer implements Runnable {
    private static final HashMap<String, Socket> userSocketMap = new HashMap<>();
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private ServerSocket serverSocket;
    private volatile boolean isRunning = false;
    private Thread serverThread;

    public void startServer() {
//        if (isRunning) {
//            System.out.println("服务器已经在运行中了");
//            return;
//        }
//        isRunning = true;
//        serverThread = new Thread(this);
//        serverThread.start();
//
//        // Add shutdown hook for graceful shutdown
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            stopServer();
//        }));
    }

    public void stopServer() {
//        if (!isRunning) {
//            System.out.println("服务器没有运行中");
//            return;
//        }
//        isRunning = false;
//        try {
//            if (serverSocket != null && !serverSocket.isClosed()) {
//                serverSocket.close();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        threadPool.shutdown(); // Allow currently queued tasks to complete
//        System.out.println("服务器已关闭");
    }

    @Override
    public void run() {
//        try {
//            serverSocket = new ServerSocket(5678);
//            System.out.println("服务器启动成功，正在监听5678端口...");
//
//            while (isRunning) {
//                try {
//                    Socket socket = serverSocket.accept();
//                    System.out.println("连接成功" + socket);
//                    handleClient(socket);
//                } catch (java.net.SocketException e) {
//                    if (!isRunning) {
//                        System.out.println("服务器通信已关闭");
//                        break;
//                    }
//                    e.printStackTrace();
//                }
//            }
//        } catch (Exception e) {
//            if(isRunning){
//               e.printStackTrace();
//            }
//        } finally {
//            stopServer();
//            System.out.println("服务器主线程已关闭");
//        }
    }

    private void handleClient(Socket socket) {
//        try {
//            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
//            Message message = (Message) in.readObject();
//            User user = (User) in.readObject();
//            System.out.println("登录信息:\r\nuserName:" + user.getUserName() + ",password:" + user.getPassword());
//
//            message.setSender("Server");
//            message.setReceiver(user.getUserName());
//            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
//
//            //登录请求
//            if (message.getMessageType().equals(MessageType.USER_LOGIN_REQUEST)) {
//                boolean loginSuccess = DBUtil.loginValidate(user.getUserName(), user.getPassword());
//                if (loginSuccess) {
//                    System.out.println("密码验证通过!");
//                    message.setMessageType(MessageType.LOGIN_VALIDATE_SUCCESS);
//                    out.writeObject(message);
//                    userSocketMap.put(user.getUserName(), socket);
//                    threadPool.execute(new ServerReceiverThread(socket));
//                    System.out.println("启动线程成功！");
//                } else {
//                    System.out.println("密码验证失败!");
//                    message.setMessageType(MessageType.LOGIN_VALIDATE_FAILURE);
//                    out.writeObject(message);
//                    socket.close();
//                }
//            }
//            //注册请求
//            else if (message.getMessageType().equals(MessageType.USER_SIGNUP_REQUEST)) {
//                int singupSuccess = -1;
//                if (DBUtil.hasUser(user.getUserName())) {
//                    //当前用户名已存在
//                    System.out.println("当前用户名已被注册！");
//                } else {
//                    singupSuccess = DBUtil.addNewUser(user);
//                }
//                if (singupSuccess == 1) {
//                    message.setMessageType(MessageType.USER_SIGNUP_SUCCESS);
//                } else {
//                    message.setMessageType(MessageType.USER_SIGNUP_FAILURE);
//                }
//                out.writeObject(message);
//                socket.close();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }
    public static Socket getUserSocket(String userName){
        return (Socket) userSocketMap.get(userName);
    }
    public static HashMap<String,Socket> getUserSocketMap(){
        return userSocketMap;
    }
}
