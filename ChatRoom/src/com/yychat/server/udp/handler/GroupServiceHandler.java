package com.yychat.server.udp.handler;

import cn.hutool.json.JSONObject;
import com.yychat.common.model.*;
import com.yychat.server.service.GroupService;
import com.yychat.server.udp.ServerReceiverThreadUDP;
import com.yychat.server.udp.YYChatUDPServer;
import com.yychat.server.view.StartServer;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 服务器端群组消息处理器
 * 负责处理群组相关的所有消息请求
 */
public class GroupServiceHandler {

    private final GroupService groupService;
    private final InetSocketAddress clientAddress;
    private final YYChatUDPServer udpServer;
    private final ServerReceiverThreadUDP receiverThread;

    public GroupServiceHandler(GroupService groupService, InetSocketAddress clientAddress, ServerReceiverThreadUDP receiverThread) {
        this.groupService = groupService;
        this.clientAddress = clientAddress;
        this.receiverThread = receiverThread;
        udpServer = StartServer.getUDPServer();
    }

    /**
     * 处理加入群组请求
     */
    public void handleJoinGroupRequest(Message message) {
        try {
            JSONObject requestData = message.getJson();
            int groupId = requestData.getInt("group_id", 0);
            String username = requestData.getStr("username", "");

            ServiceResponse<Group> response = groupService.joinGroup(groupId, username);
            buildResponseMessage(response, MessageType.GROUP_JOIN, message.getSender(), message);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(message, MessageType.GROUP_JOIN, "处理加入群组请求异常: " + e.getMessage());
        }
    }

    /**
     * 处理退出群组请求
     */
    public void handleLeaveGroupRequest(Message message) {
        try {
            JSONObject requestData = message.getJson();
            int groupId = requestData.getInt("group_id", 0);
            String username = requestData.getStr("username", "");
            ServiceResponse<String> response = groupService.leaveGroup(groupId, username);
            buildResponseMessage(response, MessageType.GROUP_LEAVE_RESPONSE, message.getSender(), message);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(message, MessageType.GROUP_LEAVE_RESPONSE, "处理退出群组请求异常: " + e.getMessage());
        }
    }

    /**
     * 处理群组信息更新请求
     */
    public void handleUpdateGroupInfoRequest(Message message) {
        try {
            JSONObject requestData = message.getJson();
            int groupId = requestData.getInt("group_id", 0);
            String groupName = requestData.getStr("group_name", "");
            String avatarPath = requestData.getStr("avatar_path", "");
            String requesterUsername = requestData.getStr("requester_username", "");

            ServiceResponse<Group> response = groupService.updateGroupInfo(groupId, groupName, avatarPath, requesterUsername);
            buildResponseMessage(response, MessageType.GROUP_INFO_UPDATE_RESPONSE, message.getSender(), message);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(message, MessageType.GROUP_INFO_UPDATE_RESPONSE, "处理群组信息更新请求异常: " + e.getMessage());
        }
    }

    /**
     * 处理删除群组请求
     */
    public void handleDeleteGroupRequest(Message message) {
        try {
            JSONObject requestData = message.getJson();
            int groupId = requestData.getInt("group_id", 0);
            String requesterUsername = requestData.getStr("requester_username", "");

            ServiceResponse<?> response = groupService.deleteGroup(groupId, requesterUsername);
            buildResponseMessage(response, MessageType.GROUP_DELETE_RESPONSE, message.getSender(), message);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(message, MessageType.GROUP_DELETE_RESPONSE, "处理删除群组请求异常: " + e.getMessage());
        }
    }


    /**
     * 处理添加群组成员请求
     */
    public void handleAddMemberRequest(Message message) {
        try {
            JSONObject requestData = message.getJson();
            int groupId = requestData.getInt("group_id", 0);
            String adderUsername = requestData.getStr("adderUsername", "");
            String targetUsername = requestData.getStr("targetUsername", "");

            ServiceResponse<?> response = groupService.addMember(groupId, adderUsername, targetUsername);
            buildResponseMessage(response, MessageType.GROUP_ADD_MEMBER_RESPONSE, message.getSender(), message);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(message, MessageType.GROUP_ADD_MEMBER_RESPONSE, "处理添加群组成员请求异常: " + e.getMessage());
        }
    }

    /**
     * 处理移除群组成员请求
     */
    public void handleRemoveMemberRequest(Message message) {
        try {
            JSONObject requestData = message.getJson();
            int groupId = requestData.getInt("groupId", 0);
            String removerUsername = requestData.getStr("removerUsername", "");
            String targetUsername = requestData.getStr("targetUsername", "");

            ServiceResponse<?> response = groupService.removeMember(groupId, removerUsername, targetUsername);
            buildResponseMessage(response, MessageType.GROUP_REMOVE_MEMBER_RESPONSE, message.getSender(), message);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(message, MessageType.GROUP_REMOVE_MEMBER_RESPONSE, "处理移除群组成员请求异常: " + e.getMessage());
        }
    }

    /**
     * 处理转让群主请求
     */
    public void handleTransferOwnerRequest(Message message) {
        try {
            JSONObject requestData = message.getJson();
            int groupId = requestData.getInt("groupId", 0);
            String currentOwner = requestData.getStr("currentOwner", "");
            String newOwner = requestData.getStr("newOwner", "");

            ServiceResponse<?> response = groupService.transferOwnership(groupId, currentOwner, newOwner);
            buildResponseMessage(response, MessageType.GROUP_TRANSFER_OWNER_RESPONSE, message.getSender(), message);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(message, MessageType.GROUP_TRANSFER_OWNER_RESPONSE, "处理转让群主请求异常: " + e.getMessage());
        }
    }

    /**
     * 处理获取群组成员列表请求
     */
    public void handleGroupMembersRequest(Message message) {
        try {
            JSONObject requestData = message.getJson();
            int groupId = requestData.getInt("groupId", 0);
            String requesterUsername = requestData.getStr("requesterUsername", "");

            ServiceResponse<List<GroupMember>> response = groupService.getGroupMembers(groupId, requesterUsername);
            buildResponseMessage(response, MessageType.GROUP_MEMBERS_RESPONSE, message.getSender(), message);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(message, MessageType.GROUP_MEMBERS_RESPONSE, "处理获取群组成员列表请求异常: " + e.getMessage());
        }
    }

    /**
     * 处理获取用户群组列表请求
     */
    public void handleUserGroupsRequest(Message message) {
        try {
            JSONObject requestData = message.getJson();
            String username = requestData.getStr("username", "");

            ServiceResponse<List<Group>> response = groupService.getUserGroups(username);
            buildResponseMessage(response, MessageType.USER_GROUPS_RESPONSE, message.getSender(), message);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(message, MessageType.USER_GROUPS_RESPONSE, "处理获取用户群组列表请求异常: " + e.getMessage());
        }
    }

    /**
     * 处理搜索群组请求
     */
    public void handleGroupSearchRequest(Message message) {
        try {
            JSONObject requestData = message.getJson();
            String keyword = requestData.getStr("keyword", "");

            ServiceResponse<List<Group>> response = groupService.searchGroups(keyword);
            buildResponseMessage(response, MessageType.GROUP_SEARCH_RESPONSE, message.getSender(), message);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorResponse(message, MessageType.GROUP_SEARCH_RESPONSE, "处理搜索群组请求异常: " + e.getMessage());
        }
    }

    /**
     * 处理群组聊天消息
     */
    public void handleGroupChatMessage(Message message) {
        try {
            // 处理群组消息的发送逻辑
            // 注意：群组消息不需要响应，而是广播给所有群组成员
            System.out.println("收到群组消息 from " + message.getSender() + ": " + message.getContent());
            // 这里可以使用GroupService来处理消息发送
            // 但由于群组消息已经在GroupService中处理了广播逻辑，
            // 所以这里主要是做一些额外的处理或日志记录

            // 如果需要确认消息，可以发送一个简单的确认响应
            if (message.isSyncMessage()) {
                Message ackMessage = Message.builder()
                        .setMessageType(MessageType.GROUP_CHAT_MESSAGE_RESPONSE)
                        .setSender(SystemUser.Server.getStr())
                        .setReceiver(message.getSender())
                        .setContent("消息已发送");

                udpServer.sendMessageToClient(clientAddress, ackMessage, message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================================
    // 辅助方法
    // ================================

    /**
     * 构建响应消息
     */
    private void buildResponseMessage(ServiceResponse<?> response, String messageType, String receiver, Message originalMessage) {
        Message responseMessage = Message.builder()
                .setMessageType(messageType)
                .setSender(SystemUser.Server.getStr())
                .setReceiver(receiver);

        JSONObject json = new JSONObject();
        if (response.isSuccess()) {
            json.set("success", true);
            json.set("data", response.getData());
        } else {
            json.set("success", false);
            json.set("message", response.getMessage());
        }
        responseMessage.setJsonMessage(json);

        udpServer.sendMessageToClient(clientAddress, responseMessage, originalMessage);
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(Message originalMessage, String responseType, String errorMessage) {
        Message responseMessage = Message.builder()
                .setMessageType(responseType)
                .setSender(SystemUser.Server.getStr())
                .setReceiver(originalMessage.getSender());

        JSONObject json = new JSONObject();
        json.set("success", false);
        json.set("message", errorMessage);
        responseMessage.setJsonMessage(json);

        udpServer.sendMessageToClient(clientAddress, responseMessage, originalMessage);
    }
}