package com.yychat.control;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yychat.api.Connection;
import com.yychat.model.Message;
import com.yychat.model.MessageType;
import com.yychat.model.Receiver;
import com.yychat.model.User;
import com.yychat.ClientMain;

import java.io.*;
import java.net.*;

/**
 * UDP版本的客户端连接类，替代原有的TCP架构
 * 基于无连接的UDP协议进行通信
 */
public class YYchatClientConnectionUDP implements Connection {
    private static DatagramSocket datagramSocket;
    private static InetSocketAddress serverAddress;
    private static ClientReceiverThreadUDP receiveThread;
    private volatile boolean isConnected = false;

    public YYchatClientConnectionUDP() {
        try {
            // 创建UDP socket，客户端使用随机端口
            datagramSocket = new DatagramSocket();
            serverAddress = new InetSocketAddress("localhost", 5678);

            System.out.println("UDP客户端连接创建成功");
            System.out.println("本地端口: " + datagramSocket.getLocalPort());
            System.out.println("服务器地址: " + serverAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送消息到服务器
     */
    private void sendMessageToServer(Message message) {
        try {
            // 序列化Message和User对象
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.flush();

            byte[] data = bos.toByteArray();

            // 创建UDP数据包
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress);

            // 发送数据包
            datagramSocket.send(packet);

            System.out.println("发送消息到服务器: " + JSONUtil.toJsonStr(message));

            oos.close();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收服务器消息
     */
    private Message receiveMessageFromServer() {
        try {
            byte[] buffer = new byte[1024 * 64]; // 64KB缓冲区
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            // 接收数据包
            datagramSocket.receive(packet);

            // 反序列化消息
            ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bis);

            Message message = (Message) ois.readObject();

            ois.close();
            bis.close();

            return message;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 登录验证
     */
    public boolean loginValidate(User user) {
        boolean loginSuccess = false;

        try {
            Message message = new Message();
            message.setSender(user.getUserName());
            message.setReceiver(Receiver.Server.getStr());
            message.setMessageType(MessageType.USER_LOGIN_REQUEST);
            message.setJsonMessage(new JSONObject(user));
            // 发送登录请求
            sendMessageToServer(message);

            // 接收服务器响应
            Message responseMessage = receiveMessageFromServer();

            if (responseMessage != null
                    && responseMessage.getMessageType().equals(MessageType.LOGIN_VALIDATE_SUCCESS)
                    && responseMessage.isJsonMessage() && responseMessage.getJson().toBean(User.class).getUserName()!=null
            ) {
                loginSuccess = true;
                isConnected = true;
                ClientMain.setCurrentUser(responseMessage.getJson().toBean(User.class));
                // 启动接收线程
                startReceiveThread();
                System.out.println("UDP登录验证成功");
            } else {
                System.out.println("UDP登录验证失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return loginSuccess;
    }

    /**
     * 用户注册
     */
    public boolean userSignup(User user) {
        boolean result = false;

        try {
            Message signupMessage = new Message();
            signupMessage.setSender(user.getUserName());
            signupMessage.setReceiver(Receiver.Server.getStr());
            signupMessage.setMessageType(MessageType.USER_SIGNUP_REQUEST);
            signupMessage.setJsonMessage(new JSONObject(user));
            // 发送注册请求
            sendMessageToServer(signupMessage);

            // 接收服务器响应
            Message responseMessage = receiveMessageFromServer();

            if (responseMessage != null && responseMessage.getMessageType().equals(MessageType.USER_SIGNUP_SUCCESS)) {
                result = true;
                System.out.println("UDP注册成功");
            } else {
                System.out.println("UDP注册失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 发送聊天消息
     */
    public void sendChatMessage(Message message) {
        if (!isConnected) {
            System.out.println("客户端未连接，无法发送消息");
            return;
        }

        try {
            // 直接使用UDP发送
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.flush();

            byte[] data = bos.toByteArray();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress);
            datagramSocket.send(packet);

            System.out.println("发送消息: " + JSONUtil.toJsonStr(message));

            oos.close();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 请求在线好友列表
     */
    public void requestOnlineFriends(String username) {
        if (!isConnected) {
            System.out.println("客户端未连接，无法请求好友列表");
            return;
        }

        try {
            Message message = new Message();
            message.setSender(username);
            message.setReceiver(Receiver.Server.getStr());
            message.setMessageType(MessageType.REQUEST_ONLINE_FRIENDS);

            sendMessageToServer(message); // 不需要User对象
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 请求在线好友列表
     */
    public void requestFriends(String username) {
        if (!isConnected) {
            System.out.println("客户端未连接，无法请求好友列表");
            return;
        }

        try {
            Message message = new Message();
            message.setSender(username);
            message.setReceiver(Receiver.Server.getStr());
            message.setMessageType(MessageType.REQUEST_FRIEND_LIST);

            sendMessageToServer(message); // 不需要User对象
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 启动接收线程
     */
    private void startReceiveThread() {
        if (receiveThread == null || !receiveThread.isAlive()) {
            receiveThread = new ClientReceiverThreadUDP(datagramSocket);
            receiveThread.start();
            System.out.println("UDP消息接收线程已启动");
        }
    }

    /**
     * 停止接收线程
     */
    public void stopReceiveThread() {
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        //向服务器发出下线
        Message message = new Message();
        message.setSender(ClientMain.UserName);
        message.setReceiver(Receiver.Server.getStr());
        message.setMessageType(MessageType.EXIT);
        sendMessageToServer(message);
        try {Thread.sleep(100);} catch (InterruptedException ignored) {}
        isConnected = false;
        //停止接收
        stopReceiveThread();
        try {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
            System.out.println("UDP客户端连接已关闭");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestUnknownFriends(String username) {
        if (!isConnected) {
            System.out.println("客户端未连接，无法请求好友列表");
            return;
        }
        try {
            Message message = new Message();
            message.setSender(username);
            message.setReceiver(Receiver.Server.getStr());
            message.setMessageType(MessageType.REQUEST_USER_LIST);
            sendMessageToServer(message); // 不需要User对象
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取Socket状态
     */
    public boolean isConnected() {
        return isConnected && datagramSocket != null && !datagramSocket.isClosed();
    }

    /**
     * 获取本地端口
     */
    public int getLocalPort() {
        return datagramSocket != null ? datagramSocket.getLocalPort() : -1;
    }

    /**
     * 同步请求用户头像 - 带超时处理
     * @param targetUserName 目标用户名
     * @param timeoutMillis 超时时间（毫秒）
     * @return 头像数据，如果请求失败返回null
     */
    public byte[] requestUserAvatar(String targetUserName, int timeoutMillis) {
        if (!isConnected) {
            System.out.println("客户端未连接，无法请求头像");
            return null;
        }

        // 创建临时的DatagramSocket用于同步请求，避免与主通信冲突
        DatagramSocket requestSocket = null;
        try {
            requestSocket = new DatagramSocket();
            InetSocketAddress serverAddr = new InetSocketAddress("localhost", 5678);

            // 1. 发送头像请求
            Message requestMessage = new Message();
            requestMessage.setSender(ClientMain.UserName);
            requestMessage.setReceiver(targetUserName);
            requestMessage.setMessageType(MessageType.REQUEST_AVATAR_DOWNLOAD);
            requestMessage.setContent("REQUEST_AVATAR_DOWNLOAD");

            sendUDPMessage(requestSocket, serverAddr, requestMessage);
            System.out.println("已发送头像请求，目标用户: " + targetUserName);

            // 2. 等待响应
            Message responseMessage = receiveUDPMessage(requestSocket, timeoutMillis);

            if (responseMessage == null) {
                System.out.println("请求用户 " + targetUserName + " 头像超时");
                return null;
            }

            // 3. 处理响应
            return processAvatarResponse(responseMessage);

        } catch (SocketTimeoutException e) {
            System.out.println("请求用户 " + targetUserName + " 头像超时: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("请求用户 " + targetUserName + " 头像时发生错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (requestSocket != null && !requestSocket.isClosed()) {
                requestSocket.close();
            }
        }
    }

    /**
     * 同步请求用户头像路径信息
     * @param targetUserName 目标用户名
     * @param timeoutMillis 超时时间（毫秒）
     * @return 头像路径信息，如果请求失败返回null
     */
    public String requestUserAvatarPath(String targetUserName, int timeoutMillis) {
        if (!isConnected) {
            System.out.println("客户端未连接，无法请求头像路径");
            return null;
        }

        DatagramSocket requestSocket = null;
        try {
            requestSocket = new DatagramSocket();
            InetSocketAddress serverAddr = new InetSocketAddress("localhost", 5678);

            // 发送头像路径请求
            Message requestMessage = new Message();
            requestMessage.setSender(ClientMain.UserName);
            requestMessage.setReceiver(targetUserName);
            requestMessage.setMessageType(MessageType.REQUEST_AVATAR);
            requestMessage.setContent("REQUEST_AVATAR_PATH");

            sendUDPMessage(requestSocket, serverAddr, requestMessage);
            System.out.println("已发送头像路径请求，目标用户: " + targetUserName);

            // 等待响应
            Message responseMessage = receiveUDPMessage(requestSocket, timeoutMillis);

            if (responseMessage == null) {
                System.out.println("请求用户 " + targetUserName + " 头像路径超时");
                return null;
            }

            // 处理响应，返回头像路径
            if (responseMessage.getMessageType().equals(MessageType.RESPONSE_AVATAR)) {
                return responseMessage.getContent();
            }

        } catch (SocketTimeoutException e) {
            System.out.println("请求用户 " + targetUserName + " 头像路径超时: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("请求用户 " + targetUserName + " 头像路径时发生错误: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (requestSocket != null && !requestSocket.isClosed()) {
                requestSocket.close();
            }
        }

        return null;
    }

    /**
     * 内部方法：发送UDP消息
     */
    private void sendUDPMessage(DatagramSocket socket, InetSocketAddress address, Message message) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(message);
        oos.flush();

        byte[] data = bos.toByteArray();
        DatagramPacket packet = new DatagramPacket(data, data.length, address);
        socket.send(packet);

        oos.close();
        bos.close();
    }

    /**
     * 内部方法：接收UDP消息（带超时）
     */
    private Message receiveUDPMessage(DatagramSocket socket, int timeoutMillis) throws IOException, ClassNotFoundException {
        socket.setSoTimeout(timeoutMillis);

        byte[] buffer = new byte[1024 * 64]; // 64KB缓冲区
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        socket.receive(packet);

        ByteArrayInputStream bis = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
        ObjectInputStream ois = new ObjectInputStream(bis);

        Message message = (Message) ois.readObject();

        ois.close();
        bis.close();

        return message;
    }

    /**
     * 内部方法：处理头像响应
     */
    private byte[] processAvatarResponse(Message responseMessage) {
        try {
            String messageType = responseMessage.getMessageType();

            if (messageType.equals(MessageType.AVATAR_DOWNLOAD_SUCCESS)) {
                // 头像下载成功，返回头像数据
                return responseMessage.getAvatarData();
            } else if (messageType.equals(MessageType.AVATAR_DOWNLOAD_FAILURE)) {
                System.out.println("服务端返回头像下载失败: " + responseMessage.getContent());
            } else {
                System.out.println("收到未知类型的头像响应: " + messageType);
            }

        } catch (Exception e) {
            System.err.println("处理头像响应时发生错误: " + e.getMessage());
            e.printStackTrace();
        }

        return null;
    }
}