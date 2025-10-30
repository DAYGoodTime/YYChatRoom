package com.yychat.server.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP服务器控制类，用于文件传输
 */
public class YYChatTCPServer {
    private static final int TCP_PORT = 3457; // TCP端口，可配置
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean running = false;

    public YYChatTCPServer() {
        try {
            serverSocket = new ServerSocket(TCP_PORT);
            threadPool = Executors.newCachedThreadPool();
            running = true;
            System.out.println("TCP服务器启动在端口: " + TCP_PORT);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("TCP服务器启动失败");
        }
    }

    public void start() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("TCP连接接受: " + socket.getInetAddress());
                ServerReceiverThreadTCP thread = new ServerReceiverThreadTCP(socket);
                threadPool.execute(thread);
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (threadPool != null) {
            threadPool.shutdown();
        }
    }
}