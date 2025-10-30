package com.yychat.client.udp;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.yychat.client.ClientMain;
import com.yychat.client.service.UserService;
import com.yychat.common.model.Message;
import com.yychat.common.model.SystemUser;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * UDP版本的客户端连接类，替代原有的TCP架构
 * 基于无连接的UDP协议进行通信
 */
public class UDPClientConnection {
    private static DatagramSocket datagramSocket;
    private static InetSocketAddress serverAddress;
    private static final long SYNC_TIME_OUT_TIME_DURATION = 10;
    private final static ConcurrentHashMap<String, CompletableFuture<Message>> syncTaskMap = new ConcurrentHashMap<>();
    protected ExecutorService threadPool = Executors.newCachedThreadPool();


    public UDPClientConnection() {
        try {
            // 创建UDP socket，客户端使用随机端口
            datagramSocket = new DatagramSocket();
            serverAddress = new InetSocketAddress("localhost", 5678);

            System.out.println("客户端UDP连接创建成功");
            System.out.println("本地端口: " + datagramSocket.getLocalPort());
            System.out.println("服务器地址: " + serverAddress);
            // 启动接收线程
            startReceiveThread();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ConcurrentHashMap<String, CompletableFuture<Message>> getTaskMap() {
        return syncTaskMap;
    }

    /**
     * 发送消息到服务器(异步，从回调处处理)
     */
    public void sendMessageToServer(Message message) {
        sendMessage(message, SystemUser.Server.getStr());
    }

    /**
     * 发送消息到服务器(同步，会等待。上限1分钟)
     */
    public Message sendMessageToServerSync(Message message) {
        return sendMessageSync(message, SystemUser.Server.getStr(), null);
    }

    /**
     * 发送消息到服务器(同步，会等待。上限自义定)
     */
    public Message sendMessageToServerSync(Message message, long timeOut) {
        return sendMessageSync(message, SystemUser.Server.getStr(), timeOut);
    }

    //内部发送消息函数
    private Message sendMessageSync(Message message, String receiver, Long timeOut) {
        long maxTimeOut = timeOut == null ? SYNC_TIME_OUT_TIME_DURATION : timeOut;
        message.setSyncMessage(true);
        String taskId = UUID.randomUUID().toString();
        message.setSyncTaskId(taskId);
        message.setReceiver(receiver);
        CompletableFuture<Message> task = new CompletableFuture<>();
        syncTaskMap.put(taskId, task);
        try {
            sendMessage(message, null);
            return task.get(maxTimeOut, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            System.out.println("同步获取超时 任务id:" + taskId);
        }
        return null;
    }

    /**
     * 发送UDP消息
     *
     * @param message  消息对象
     * @param receiver 接受者，为空则为消息对象当中的接受者
     */
    private void sendMessage(Message message, String receiver) {
        if (receiver != null) {
            message.setReceiver(receiver);
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.flush();
            byte[] data = bos.toByteArray();
            if (data.length > 60 * 1024) {
                System.out.println("数据包过大，无法发送!");
                return;
            }
            // 创建UDP数据包
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress);
            // 发送数据包
            datagramSocket.send(packet);
            System.out.println("发送消息到服务器: " + JSONUtil.toJsonStr(message));
            oos.close();
            bos.close();
        } catch (Exception e) {
            System.err.println("发送消息过程中出现异常");
            e.printStackTrace();
        }
    }

    /**
     * 发送聊天消息
     */
    public void sendChatMessage(Message message) {
        sendMessage(message, null);
    }

    /**
     * 启动接收线程
     */
    private void startReceiveThread() {
        if (!threadPool.isShutdown()) {
            new UDPClientReceiverThread(datagramSocket, this).start();
            System.out.println("UDP消息接收线程已启动");
        }
    }

    /**
     * 停止接收线程
     */
    public void stopReceiveThread() {
        if (!threadPool.isShutdown()) {
            threadPool.shutdownNow();
        }
    }

    /**
     * 关闭连接
     */
    public void close() {
        //向服务器发出下线
        if (UserService.userLogin) {
            sendMessageToServer(Message.builder().setMessageType(Message.EXIT).setSender(ClientMain.getCurrentUser().getUserName()));
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        //停止接收
        stopReceiveThread();
        try {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
            System.out.println("客户端连接已关闭");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}