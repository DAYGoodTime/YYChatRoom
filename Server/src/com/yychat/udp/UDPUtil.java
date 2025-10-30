package com.yychat.udp;

import cn.hutool.json.JSONUtil;
import com.yychat.model.Message;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class UDPUtil {

    static DatagramSocket socket;

    static {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.println("UDP Socket初始化失败");
            throw new RuntimeException(e);
        }
    }

    public static void sendMessageToClient(InetSocketAddress clientAddress, Message message) {
        try {
            // 序列化消息
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(message);
            oos.flush();

            byte[] data = bos.toByteArray();

            // 创建数据包并发送
            DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress);
            if (socket.isClosed()) {
                socket = new DatagramSocket();
            }
            socket.send(packet);

            System.out.println("向 " + clientAddress + " 发送消息: " + JSONUtil.toJsonStr(message));

            oos.close();
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
