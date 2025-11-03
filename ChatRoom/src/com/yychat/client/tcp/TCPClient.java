package com.yychat.client.tcp;

import cn.hutool.json.JSONUtil;
import com.yychat.client.ClientMain;
import com.yychat.common.model.Message;
import com.yychat.common.model.MessageType;

import javax.swing.*;
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
    private static final int READ_TIMEOUT = 60 * 1000;   // 60秒读取超时

    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private volatile boolean connected = false;
    private volatile boolean running = false;

    // 响应处理器映射
    private final ConcurrentHashMap<String, CompletableFuture<Message>> responseHandlers = new ConcurrentHashMap<>();
    private Thread messageReceiverThread;

    // 心跳机制相关字段
    private ScheduledExecutorService heartbeatExecutor = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> heartbeatTask;
    private int heartbeatFailureCount = 0;
    private static final int HEARTBEAT_INTERVAL = 30; // 30秒
    private static final int MAX_HEARTBEAT_FAILURES = 3; // 最大心跳失败次数

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

            // 启动心跳机制
            startHeartbeat();

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

        // 停止心跳机制
        stopHeartbeat();

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
     * 发送消息并等待响应 (TCP长连接模式)
     *
     * @param message        要发送的消息
     * @param timeoutSeconds 超时时间(秒)
     * @return 接收到的响应消息
     */
    public Optional<Message> sendMessage(Message message, int timeoutSeconds) {
        try {
            // 检查连接状态，如果未连接则尝试连接
            if (!isConnected()) {
                if (!connect()) {
                    System.out.println("TCP连接未建立，连接失败");
                    return Optional.empty();
                }
            }

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
                return Optional.ofNullable(response);
            }

        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            System.out.println("TCP发送失败: " + e.getMessage());
            // 连接可能已断开，尝试重连
            disconnect();
            return Optional.empty();
        }
    }

    /**
     * 启动消息接收线程
     */
    private void startMessageReceiver() {
        messageReceiverThread = new Thread(() -> {
            try {
                while (running && connected) {
                    try {
                        Object obj = ois.readObject();
                        if (obj instanceof Message) {
                            Message response = (Message) obj;
                            System.out.println("接收到TCP响应 " + JSONUtil.toJsonStr(response));

                            // 在独立线程中处理响应，提高并发性能
                            CompletableFuture.runAsync(() -> handleResponse(response));
                        }
                    } catch (SocketException e) {
                        if (running) {
                            System.err.println("Socket异常，连接可能已断开: " + e.getMessage());
                        }
                        break;
                    } catch (Exception e) {
                        if (running) {
                            System.err.println("接收消息失败: " + e.getMessage());
                            // 连接异常时尝试重连
                            checkConnectionHealth();
                        }
                        break;
                    }
                }
            } finally {
                if (running) {
                    System.out.println("TCP消息接收线程意外结束，尝试重连...");
                    checkConnectionHealth();
                } else {
                    System.out.println("TCP消息接收线程正常结束");
                }
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
        if (taskId != null) {
            // 使用computeIfPresent确保原子性操作
            responseHandlers.computeIfPresent(taskId, (key, future) -> {
                future.complete(response);
                return null; // 移除处理器
            });
            return;
        }

        // 处理心跳响应
        if (MessageType.TCP_HEARTBEAT.equals(response.getMessageType())) {
            synchronized (this) {
                heartbeatFailureCount = 0; // 重置失败计数
//                System.out.println("收到心跳响应");
            }
            return;
        }

        // 处理其他响应消息...
    }

    /**
     * 启动心跳机制
     */
    private void startHeartbeat() {
        heartbeatTask = heartbeatExecutor.scheduleWithFixedDelay(() -> {
            if (isConnected()) {
                try {
                    Message heartbeat = Message.builder()
                            .setSender(ClientMain.getCurrentUserName())
                            .setMessageType(MessageType.TCP_HEARTBEAT);
                    synchronized (oos) {
                        oos.writeObject(heartbeat);
                        oos.flush();
                    }
                    heartbeatFailureCount = 0; // 重置失败计数
//                    System.out.println("发送TCP心跳包");
                } catch (IOException e) {
                    heartbeatFailureCount++;
                    System.err.println("心跳发送失败 (" + heartbeatFailureCount + "/" + MAX_HEARTBEAT_FAILURES + ")");

                    if (heartbeatFailureCount >= MAX_HEARTBEAT_FAILURES) {
                        System.err.println("心跳失败次数过多，断开连接");
                        disconnect();
                    }
                }
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }

    /**
     * 停止心跳机制
     */
    private void stopHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isCancelled()) {
            heartbeatTask.cancel(false);
        }
        heartbeatFailureCount = 0;
    }

    /**
     * 确保连接可用，如果断开则自动重连
     */
    public boolean ensureConnected() {
        if (isConnected()) {
            return true;
        }
        return connect();
    }

    /**
     * 带重试的连接机制
     */
    private boolean reconnectWithRetry(int maxRetries) {
        connected = false;
        for (int i = 0; i < maxRetries; i++) {
            // 指数退避：1s, 2s, 4s...
            try {
                Thread.sleep((1 << i) * 1000);
                if (connect()) {
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return false;
    }

    /**
     * 获取连接状态
     */
    public boolean isConnected() {
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }

    /**
     * 带重试功能的消息发送
     *
     * @param message        要发送的消息
     * @param timeoutSeconds 超时时间(秒)
     * @param maxRetries     最大重试次数
     * @return 接收到的响应消息
     */
    public Optional<Message> sendMessageWithRetry(Message message, int timeoutSeconds, int maxRetries) {
        // 定期清理过期的响应处理器
        cleanupExpiredResponseHandlers();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                if (!ensureConnected()) {
                    System.out.println("连接检查失败，尝试重连 (第" + attempt + "次)");
                    continue;
                }
                return sendMessage(message, timeoutSeconds);
            } catch (Exception e) {
                System.err.println("发送失败 (第" + attempt + "次): " + e.getMessage());
                if (attempt == maxRetries) {
                    System.err.println("重试次数已用完，发送失败");
                    disconnect(); // 清理损坏的连接
                    return Optional.empty();
                }
                // 短暂等待后重试
                try {
                    Thread.sleep(1000 * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 清理过期的响应处理器
     */
    private void cleanupExpiredResponseHandlers() {
        // 每隔100次发送清理一次
        if (responseHandlers.size() > 50) {
            responseHandlers.entrySet().removeIf(entry -> {
                CompletableFuture<Message> future = entry.getValue();
                return future.isDone() || future.isCancelled();
            });
        }
    }

    /**
     * 连接健康检查和自动重连
     */
    private void checkConnectionHealth() {
        System.out.println("检测到连接断开，尝试重连...");
        if (reconnectWithRetry(3)) {
            System.out.println("重连成功");
        } else {
            JOptionPane.showMessageDialog(ClientMain.getMainWindow(), "连接失败", "TCP连接失败", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    /**
     * 关闭所有资源
     */
    public void shutdown() {
        disconnect();
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdown();
            try {
                if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    heartbeatExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}