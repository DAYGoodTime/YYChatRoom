package com.yychat.server.udp.handler;

import cn.hutool.json.JSONObject;
import com.yychat.common.model.*;
import com.yychat.server.service.GroupService;
import com.yychat.server.udp.ServerReceiverThreadUDP;
import com.yychat.server.udp.YYChatUDPServer;
import com.yychat.server.util.DBUtil;
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
            String content = message.getJson().getStr("content", "");
            System.out.println("收到群组消息 from " + message.getSender() + ": " + content);
            int groupId = message.getJson().getInt("group_id", -1);
            if(groupId == -1) {
                System.out.println("无效的group_id");
                return;
            }

            //将消息插入数据库中
            long id = DBUtil.saveGroupMessage(message, groupId);
            Group group = DBUtil.getGroupById(groupId);
            if(id == -1){
                System.out.println("插入群组消息失败,groupId:" + groupId);
                return;
            }
            JSONObject json = message.getJson();
            json.set("group_info", group)
                    .set("message_id", id);
            message.setJsonMessage(json);
            //广播消息
            udpServer.broadcastGroupMessage(groupId,message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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