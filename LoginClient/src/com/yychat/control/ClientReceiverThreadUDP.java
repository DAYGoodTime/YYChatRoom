package com.yychat.control;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.yychat.api.MessageThread;
import com.yychat.model.Message;
import com.yychat.model.MessageType;
import com.yychat.view.ClientMain;
import com.yychat.view.FriendChat;
import com.yychat.view.FriendList;

import javax.swing.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;

/**
 * UDP版本的客户端接收线程，处理来自服务器的UDP消息
 */
public class ClientReceiverThreadUDP extends MessageThread {
    private DatagramSocket datagramSocket;
    private volatile boolean isRunning = true;

    public ClientReceiverThreadUDP(DatagramSocket socket) {
        this.datagramSocket = socket;
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

                    // 处理接收到的消息
                    processReceivedMessage(packet);
                } else {
                    break;
                }
            } catch (SocketException se) {
                if (this.isInterrupted() && datagramSocket != null && datagramSocket.isClosed()) {
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

            switch (message.getMessageType()) {
                case MessageType.COMMON_CHAT_MESSAGE:
                    handleChatMessage(message);
                    break;

                case MessageType.RESPONSE_ONLINE_FRIENDS:
                    handleResponseOnlineFriends(message);
                    break;

                case MessageType.NEW_ONLINE_TO_ALL_FRIENDS:
                    handleNewOnlineFriend(message);
                    break;

                case MessageType.USER_ADD_NEW_FRIEND_SUCCESS:
                    handleAddNewFriendSuccess(message);
                    break;

                case MessageType.USER_ADD_NEW_FRIEND_FAILURE_ALREADY_FRIEND:
                    handleAddNewFriendFailureAlreadyFriend(message);
                    break;

                case MessageType.USER_ADD_NEW_FRIEND_FAILURE_NO_USER:
                    handleAddNewFriendFailureNoUser(message);
                    break;

                case MessageType.RESPONSE_FRIEND_LIST:
                    handleResponseFriendList(message);
                    break;

                case MessageType.IS_FRIEND_ONLINE_SUCCESS:
                    handleIsFriendOnlineSuccess(message);
                    break;

                case MessageType.IS_FRIEND_ONLINE_FAILURE:
                    handleIsFriendOnlineFailure(message);
                    break;

                default:
                    System.out.println("未处理的UDP消息类型: " + message.getMessageType());
                    break;
            }

            ois.close();
            bis.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleChatMessage(Message message) {
        try {
            String receiver = message.getReceiver();
            String sender = message.getSender();
            String chatKey = receiver + "to" + sender;

            System.out.println("收到来自 " + sender + " 的消息: " + message.getContent());

            // 查找或创建聊天窗口
            FriendChat chat = FriendList.getFriendChat(chatKey);
            if (chat != null) {
                chat.append(message);
            } else {
                System.out.println("请打开 " + chatKey + " 的聊天界面");
                // 可以在这里添加自动打开聊天窗口的逻辑
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleResponseOnlineFriends(Message message) {
        try {
            String receiver = message.getReceiver();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null && message.isJsonMessage()) {
                JSONArray list = message.getJson().getJSONArray("list");
                System.out.println("收到在线好友列表: " + list.toJSONString(0));
                if(!list.isEmpty()){
                    friendList.activeOnlineFriendIcon(list.toList(String.class));
                }
            } else {
                System.out.println("未找到好友列表窗口");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleNewOnlineFriend(Message message) {
        try {
            String receiver = message.getReceiver();
            String sender = message.getSender();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null) {
                System.out.println("新好友上线通知: " + sender);
                friendList.activeNewOnlineFriendIcon(sender);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddNewFriendSuccess(Message message) {
        try {
            String receiver = message.getReceiver();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null) {
                System.out.println("添加好友成功");
                JOptionPane.showMessageDialog(null, "添加好友成功！");
                friendList.addNewFriend(message.getContent());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddNewFriendFailureAlreadyFriend(Message message) {
        try {
            String receiver = message.getReceiver();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null) {
                System.out.println("添加好友失败：已经是好友");
                JOptionPane.showMessageDialog(null, "添加失败！该用户已经是你的朋友！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddNewFriendFailureNoUser(Message message) {
        try {
            String receiver = message.getReceiver();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null) {
                System.out.println("添加好友失败：用户不存在");
                JOptionPane.showMessageDialog(null, "添加失败！没有该用户");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleResponseFriendList(Message message) {
        try {
            String receiver = message.getReceiver();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null && message.isJsonMessage()) {
                System.out.println("收到好友列表: " + message.getJson().getJSONArray("list").toJSONString(0));
                friendList.setFriendList(message.getJson().getJSONArray("list").toList(String.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleIsFriendOnlineSuccess(Message message) {
        try {
            String receiver = message.getReceiver();
            System.out.println("好友 " + message.getContent() + " 在线");
            // 可以在这里添加好友在线状态显示
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleIsFriendOnlineFailure(Message message) {
        try {
            String receiver = message.getReceiver();
            System.out.println("好友 " + message.getContent() + " 不在线");
            // 可以在这里添加好友离线状态显示
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopThread() {
        this.isRunning = false;
        this.interrupt();
    }
}