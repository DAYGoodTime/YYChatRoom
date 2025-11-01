package com.yychat.client.tcp;

import cn.hutool.json.JSONUtil;
import com.yychat.common.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * TCP客户端核心连接管理类
 * 负责TCP连接管理、消息发送接收、响应处理
 */
public class TCPClient {
    private static final String TCP_HOST = "localhost";
    private static final int TCP_PORT = 3457;
    private static final int CONNECT_TIMEOUT = 5000; // 5秒连接超时
    private static final int READ_TIMEOUT = 30000;   // 30秒读取超时

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private volatile boolean connected = false;
    private volatile boolean running = false;

    // 响应处理器映射
    private final ConcurrentHashMap<String, CompletableFuture<Message>> responseHandlers = new ConcurrentHashMap<>();
    private Thread messageReceiverThread;

    public TCPClient() {
    }

    /**
     * 连接到TCP服务器
     */
    public synchronized boolean connect() {
        if (connected) {
            return true;
        }

        try {
            socket = new Socket(TCP_HOST, TCP_PORT);
            socket.setSoTimeout(READ_TIMEOUT);
            socket.setKeepAlive(true);

            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            connected = true;
            running = true;

            // 启动消息接收线程
            startMessageReceiver();
            return true;

        } catch (IOException e) {
            System.err.println("TCP连接失败: " + e.getMessage());
            disconnect();
            return false;
        }
    }

    /**
     * 断开TCP连接
     */
    public synchronized void disconnect() {
        running = false;
        connected = false;

        if (messageReceiverThread != null && messageReceiverThread.isAlive()) {
            messageReceiverThread.interrupt();
        }

        try {
            if (ois != null) ois.close();
            if (oos != null) oos.close();
            if (socket != null) socket.close();
            responseHandlers.clear();
        } catch (IOException e) {
            System.err.println("断开连接时发生错误: " + e.getMessage());
        }
    }

    /**
     * 发送消息并等待响应 (TCP同步模式)
     *
     * @param message 要发送的消息
     * @return 接收到的响应消息
     */
    public Optional<Message> sendMessage(Message message) {
        return sendMessage(message, 30); // 默认30秒超时
    }

    /**
     * 发送消息并等待响应 (TCP同步模式)
     *
     * @param message        要发送的消息
     * @param timeoutSeconds 超时时间(秒)
     * @return 接收到的响应消息
     */
    public Optional<Message> sendMessage(Message message, int timeoutSeconds) {

        try {
            connect();
            synchronized (oos) {
                // 生成唯一的任务ID用于匹配响应
                final String taskId = UUID.randomUUID().toString();
                message.setSyncTaskId(taskId);
                System.out.println("发送TCP请求到服务端 " + JSONUtil.toJsonStr(message));
                CompletableFuture<Message> task = new CompletableFuture<>();
                responseHandlers.put(taskId, task);
                oos.writeObject(message);
                oos.flush();
                Message response = task.get(timeoutSeconds, TimeUnit.SECONDS);
                disconnect();
                return Optional.ofNullable(response);
            }

        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            System.out.println("网络异常");
            return Optional.empty();
        }
    }

    /**
     * 启动消息接收线程
     */
    private void startMessageReceiver() {
        messageReceiverThread = new Thread(() -> {
            while (running && connected) {
                try {
                    Object obj = ois.readObject();
                    if (obj instanceof Message) {
                        Message response = (Message) obj;
                        System.out.println("接收到TCP响应 " + JSONUtil.toJsonStr(response));
                        handleResponse(response);
                    }
                } catch (Exception e) {
                    if (running) {
                        System.err.println("接收消息失败: " + e.getMessage());
                        if (e instanceof SocketException) {
                            break;
                        }
                    }
                    break;
                }
            }
            if (running) {
                System.out.println("TCP消息接收线程结束");
            }
        }, "TCP-Client-Receiver");

        messageReceiverThread.setDaemon(true);
        messageReceiverThread.start();
    }

    /**
     * 处理接收到的响应消息
     */
    private void handleResponse(Message response) {
        String taskId = response.getSyncTaskId();
        if (taskId != null && responseHandlers.containsKey(taskId)) {
            responseHandlers.remove(taskId).complete(response);
        }
        //keep doing other
    }

    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }
}