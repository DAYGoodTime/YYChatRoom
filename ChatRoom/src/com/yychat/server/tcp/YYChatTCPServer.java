package com.yychat.server.tcp;

import com.yychat.server.view.StartServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP服务器控制类，用于文件传输
 */
public class YYChatTCPServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean running = false;

    public YYChatTCPServer() {
        try {
            int tcp_port = StartServer.getServerConfig().getServer_port_tcp();
            serverSocket = new ServerSocket(tcp_port);
            threadPool = Executors.newCachedThreadPool();
            running = true;
            System.out.println("TCP服务器启动在端口: " + tcp_port);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("TCP服务器启动失败");
        }
    }

    public void start() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
//                System.out.println("TCP连接接受: " + socket.getInetAddress());
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