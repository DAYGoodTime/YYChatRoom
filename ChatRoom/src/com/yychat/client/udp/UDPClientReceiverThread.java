package com.yychat.client.udp;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yychat.client.udp.handler.ChatMessageHandler;
import com.yychat.client.udp.handler.UserServiceHandler;
import com.yychat.common.model.Message;
import com.yychat.common.model.MessageType;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.CompletableFuture;

/**
 * UDP版本的客户端接收线程，处理来自服务器的UDP消息
 */
public class UDPClientReceiverThread extends Thread {
    private final DatagramSocket datagramSocket;
    private volatile boolean isRunning = true;
    private final UDPClientConnection connection;

    public UDPClientReceiverThread(DatagramSocket socket, UDPClientConnection connection) {
        this.datagramSocket = socket;
        this.connection = connection;
    }

    @Override
    public void run() {
        while (!this.isInterrupted() && isRunning) {
            try {
                if (datagramSocket != null && !datagramSocket.isClosed()) {
                    // 创建接收缓冲区
                    byte[] buffer = new byte[1024 * 64]; // 64KB缓冲区
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    // 接收UDP数据包
                    datagramSocket.receive(packet);
                    // 处理接收到的消息 (使用线程池处理，避免阻塞其它请求)
                    connection.threadPool.execute(() -> processReceivedMessage(packet));
                } else {
                    break;
                }
            } catch (SocketException se) {
                if (this.isInterrupted() || datagramSocket.isClosed()) {
                    System.out.println("UDP客户端socket已关闭，停止接收线程");
                    break;
                } else {
                    System.out.println("UDP接收线程Socket异常: " + se.getMessage());
                    se.printStackTrace();
                }
            } catch (Exception e) {
                if (!this.isInterrupted()) {
                    e.printStackTrace();
                }
                break;
            }
        }

        System.out.println("UDP客户端接收线程已结束");
    }

    private void processReceivedMessage(DatagramPacket packet) {
        try {
            // 反序列化消息
            byte[] data = packet.getData();
            ByteArrayInputStream bis = new ByteArrayInputStream(data, 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bis);
            Message message = (Message) ois.readObject();
            System.out.println("客户端接收消息: " + JSONUtil.toJsonStr(message));
            if (message.isSyncMessage()) {
                processSyncMessage(message);
            } else {
                switch (message.getMessageType()) {
                    case MessageType.COMMON_CHAT_MESSAGE:
                        ChatMessageHandler.handleChatMessage(message);
                        break;
                    case MessageType.REQUEST_ONLINE_FRIENDS:
                        UserServiceHandler.handleResponseOnlineFriends(message);
                        break;
                    case MessageType.NEW_ONLINE_FRIEND:
                        UserServiceHandler.handleNewOnlineFriend(message);
                        break;

                    case MessageType.USER_ADD_NEW_FRIEND:
                        UserServiceHandler.handleAddNewFriendResponse(message);
                        break;
                    case MessageType.REQUEST_FRIEND_LIST:
                        UserServiceHandler.handleFriendListResponse(message);
                        break;
                    case MessageType.REQUEST_UNK_USER_LIST:
                        UserServiceHandler.handelUnknownFriendsResponse(message);
                        break;
                    default:
                        System.out.println("未处理的消息类型: " + message.getMessageType());
                        break;
                }
                ois.close();
                bis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processSyncMessage(Message message) {
        String taskId = message.getSyncTaskId();
        if (StrUtil.isBlank(taskId)) {
            System.out.println("无效的同步数据 task_id 为空");
            return;
        }
        CompletableFuture<Message> task = connection.getTaskMap().getOrDefault(taskId, null);
        if (task == null) {
            System.out.println("无效的同步数据,无对应task");
            return;
        }
        task.complete(message);
    }

    public void stopThread() {
        this.isRunning = false;
        this.interrupt();
    }
}