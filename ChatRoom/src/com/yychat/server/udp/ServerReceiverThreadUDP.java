package com.yychat.server.udp;

import cn.hutool.json.JSONUtil;
import com.yychat.common.model.Message;
import com.yychat.common.model.MessageType;
import com.yychat.server.service.GroupService;
import com.yychat.server.service.UserService;
import com.yychat.server.udp.handler.GroupServiceHandler;
import com.yychat.server.udp.handler.UserServiceHandler;
import com.yychat.server.util.DBUtil;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;

/**
 * UDP版本的服务器接收线程，处理无连接UDP消息
 * 由于UDP是无连接的，此线程主要处理特定用户的消息流
 */
public class ServerReceiverThreadUDP implements Runnable {
    private InetSocketAddress clientAddress;
    private volatile boolean isRunning = true;
    private final YYChatUDPServer serverThread;
    private final UserService userService;
    private final UserServiceHandler userServiceHandler;
    private final GroupServiceHandler groupServiceHandler;

    // 重载构造函数，用于直接处理接收到的数据包
    public ServerReceiverThreadUDP(DatagramPacket packet, YYChatUDPServer serverThread) {
        this.clientAddress = (InetSocketAddress) packet.getSocketAddress();
        this.serverThread = serverThread;
        userService = new UserService(this.clientAddress);
        userServiceHandler = new UserServiceHandler(userService, clientAddress,this);
        groupServiceHandler = new GroupServiceHandler(GroupService.getInstance(),this.clientAddress,this);
        try {
            // 处理接收到的数据包
            processReceivedPacket(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // 由于无连接特性，此线程主要用于处理后续消息
        while (isRunning && !Thread.currentThread().isInterrupted()) {
            try {
                // 接收逻辑在这里实现
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        // 清理资源
        stop();
    }

    private void processReceivedPacket(DatagramPacket packet) {
        try {
            // 反序列化接收到的消息
            byte[] data = packet.getData();
            ByteArrayInputStream bis = new ByteArrayInputStream(data, 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bis);

            Message message = (Message) ois.readObject();

            System.out.println("处理来自 " + clientAddress + " 的消息: " + JSONUtil.toJsonStr(message));

            switch (message.getMessageType()) {
                case MessageType.USER_LOGIN_REQUEST:
                    userService.handelUserLoginReq(message);
                    break;
                case MessageType.USER_SIGNUP_REQUEST:
                    userService.handelUserSignupReq(message);
                    break;
                case MessageType.COMMON_CHAT_MESSAGE:
                    handleChatMessage(message);
                    break;
                case MessageType.REQUEST_USER_INFO:
                    userServiceHandler.handelReqUserInfo(message);
                    break;
                case MessageType.EXIT:
                    userServiceHandler.handleUserExit(message);
                    break;
                case MessageType.REQUEST_ONLINE_FRIENDS:
                    userServiceHandler.handleRequestOnlineFriends(message);
                    break;
                case MessageType.NEW_ONLINE_FRIEND:
                    userServiceHandler.handleNewOnlineFriend(message);
                    break;
                case MessageType.REQUEST_FRIEND_LIST:
                    userServiceHandler.handleRequestFriendList(message);
                    break;
                case MessageType.USER_ADD_NEW_FRIEND:
                    userServiceHandler.handleAddNewFriend(message);
                    break;
                case MessageType.REQUEST_UNK_USER_LIST:
                    userServiceHandler.handelRequestUnkUsers(message);
                    break;
                case MessageType.REQUEST_AVATAR_PATH:
                    userServiceHandler.handleRequestAvatar(message);
                    break;
                case MessageType.GROUP_CHAT_MESSAGE:
                    groupServiceHandler.handleGroupChatMessage(message);
                    break;
                case MessageType.GROUP_JOIN:
                    groupServiceHandler.handleJoinGroupRequest(message);
                    break;
                case MessageType.GROUP_USER_LEAVE:
                    groupServiceHandler.handleLeaveGroupRequest(message);
                    break;
                case MessageType.GROUP_DELETE_REQUEST:
                    groupServiceHandler.handleDeleteGroupRequest(message);
                    break;
                case MessageType.GROUP_ADD_MEMBER:
                    groupServiceHandler.handleAddMemberRequest(message);
                    break;
                case MessageType.GROUP_REMOVE_MEMBER:
                    groupServiceHandler.handleRemoveMemberRequest(message);
                    break;
                case MessageType.GROUP_TRANSFER_OWNER:
                    groupServiceHandler.handleTransferOwnerRequest(message);
                    break;
                case MessageType.GROUP_MEMBERS:
                    groupServiceHandler.handleGroupMembersRequest(message);
                    break;
                case MessageType.USER_GROUPS:
                    groupServiceHandler.handleUserGroupsRequest(message);
                    break;
                case MessageType.GROUP_SEARCH:
                    groupServiceHandler.handleGroupSearchRequest(message);
                    break;
                default:
                    System.out.println("未处理的消息类型: " + message.getMessageType());
                    break;
            }
            ois.close();
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleChatMessage(Message message) {
        // 记录聊天消息到数据库
        long id = DBUtil.insertChatMessage(message.getSender(), message.getReceiver(), message.getJson().toJSONString(0), message.getTime());
        if(id == -1){
            System.out.println("消息插入失败，跳过");
            return;
        }
        message.setJsonMessage(message.getJson().set("message_id", id));
        // 获取接收方的地址
        InetSocketAddress receiverAddress = serverThread.getUserAddress(message.getReceiver());
        if (receiverAddress != null) {
            // 转发消息给接收方
            serverThread.sendMessageToClient(receiverAddress, message, null);
        }
    }

    public void stop() {
        this.isRunning = false;
        Thread.currentThread().interrupt();
    }
}