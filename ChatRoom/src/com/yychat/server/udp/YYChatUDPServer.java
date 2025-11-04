package com.yychat.server.udp;

import cn.hutool.json.JSONUtil;
import com.yychat.common.model.GroupMember;
import com.yychat.common.model.Message;
import com.yychat.common.model.MessageType;
import com.yychat.server.util.DBUtil;
import com.yychat.server.view.StartServer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 服务器类
 * 基于无连接的UDP协议，支持无状态消息传输
 */
public class YYChatUDPServer implements Runnable {
    private static final HashMap<String, InetSocketAddress> userAddressMap = new HashMap<>();
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    private DatagramSocket datagramSocket;
    private volatile boolean isRunning = false;
    private Thread serverThread;

    public void startServer() {
        if (isRunning) {
            System.out.println("服务器已经在运行中了");
            return;
        }
        isRunning = true;
        serverThread = new Thread(this);
        serverThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(this::stopServer));
    }

    public void stopServer() {
        if (!isRunning) {
            System.out.println("服务器没有运行中");
            return;
        }
        isRunning = false;
        try {
            if (datagramSocket != null && !datagramSocket.isClosed()) {
                datagramSocket.close();
            }
            serverThread.interrupt();
            serverThread = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        threadPool.shutdown();

        System.out.println("UDP服务器已关闭");
    }

    @Override
    public void run() {
        try {
            int port = StartServer.getServerConfig().getServer_port_udp();
            datagramSocket = new DatagramSocket(port);
            System.out.println("服务器启动成功，正在监听" + port + "端口...");

            while (isRunning) {
                try {
                    // 创建接收数据包的缓冲区
                    byte[] buffer = new byte[1024 * 64]; // 64KB缓冲区
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    // 接收数据包
                    datagramSocket.receive(packet);
                    // 处理数据包
                    handleClient(packet);
                } catch (java.net.SocketException e) {
                    if (!isRunning) {
                        System.out.println("服务器通信已关闭");
                        break;
                    }
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            if (isRunning) {
                e.printStackTrace();
            }
        } finally {
            stopServer();
            System.out.println("服务器主线程已关闭");
        }
    }

    private void handleClient(DatagramPacket packet) {
        try {
            // 反序列化消息和用户对象
            byte[] data = packet.getData();
            ByteArrayInputStream bis = new ByteArrayInputStream(data, 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bis);
            threadPool.execute(new ServerReceiverThreadUDP(packet, this));
            ois.close();
            bis.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToClient(InetSocketAddress clientAddress, Message message, Message originMessage) {
        try {
            if (originMessage != null && originMessage.isSyncMessage()) {
                //要带上同步消息的task_id
                message.setSyncTaskId(originMessage.getSyncTaskId());
            }
            // 序列化消息
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.flush();

            byte[] data = bos.toByteArray();

            // 创建数据包并发送
            DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress);
            datagramSocket.send(packet);

            System.out.println("向 " + clientAddress + " 发送消息: " + JSONUtil.toJsonStr(message));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public InetSocketAddress getUserAddress(String userName) {
        return userAddressMap.get(userName);
    }

    public HashMap<String, InetSocketAddress> getUserAddressMap() {
        return userAddressMap;
    }

    /**
     * 广播消息给所有人(除消息发送者自己)
     */
    public void broadcastMessage(Message message) {
        Map<String, InetSocketAddress> map = getUserAddressMap();
        InetSocketAddress self = map.get(message.getSender());
        map.values().forEach(userAddress -> {
            if(!userAddress.equals(self))
                sendMessageToClient(userAddress,message,null);
        });
    }
    /**
     * 广播消息给指定群体(除消息发送者自己)
     */
    public void broadcastMessage(Message message, List<String> users) {
        users.removeIf(u->u.equals(message.getSender()));
        getUserAddressMap().entrySet().stream()
                .filter(entry-> users.contains(entry.getKey()))
                .forEach(entry -> sendMessageToClient(entry.getValue(),message,null));

    }
    /**
     * 广播群组消息给所有在线成员
     */
    public void broadcastGroupMessage(int groupId, Message message) {
        try {
            // 获取群组成员列表(除开自己)
            List<String> members = DBUtil.getGroupMembers(groupId).stream()
                    .map(GroupMember::getUsername)
                    .filter(u->!u.equals(message.getSender()))
                    .collect(Collectors.toList());
            Map<String, InetSocketAddress> userAddressMap = getUserAddressMap();
            userAddressMap.entrySet().stream()
                    .filter(entry -> members.contains(entry.getKey()))
                    .forEach(entry -> sendMessageToClient(entry.getValue(),message,null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}