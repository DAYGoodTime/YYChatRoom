package com.yychat.client.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.yychat.client.ClientMain;
import com.yychat.client.udp.UDPClientConnection;
import com.yychat.common.model.*;
import com.yychat.common.model.AttachmentType;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * 客户端群组服务类
 * 负责处理群组相关的客户端业务逻辑
 */
public class GroupService {

    private static final GroupService instance = new GroupService();

    public static GroupService getInstance() {
        return instance;
    }

    private UDPClientConnection getConnection() {
        return ClientMain.getUDPConnection();
    }

    /**
     * 创建群组
     */
    public ServiceResponse<Group> createGroup(String groupName, String avatarPath) {
        ServiceResponse<Group> serviceResponse = new ServiceResponse<>(new Group());
        try {
            // 参数验证
            if (StrUtil.isEmpty(groupName)) {
                return serviceResponse.error("群组名称不能为空");
            }
            File avatarFile = new File(avatarPath);
            if (!avatarFile.exists()) {
                return serviceResponse.error("头像不存在");
            }

            JSONObject requestData = new JSONObject();
            requestData.set("group_info", new Group(groupName, avatarPath, ClientMain.getCurrentUserName()));
            requestData.set("avatar_file_name", avatarFile.getName());
            requestData.set("request_user", ClientMain.getCurrentUserName());
            requestData.set("update", false);
            Message message = Message.builder()
                    .setMessageType(MessageType.TCP_GROUP_SAVE)
                    .setSender(ClientMain.getCurrentUserName())
                    .setJsonMessage(requestData)
                    .setAttachment(FileUtil.readBytes(avatarFile), byte[].class, AttachmentType.GROUP_AVATAR);
            Optional<Message> response = ClientMain.getTCPConnection().sendMessage(message);
            if (!response.isPresent()) {
                return serviceResponse.error("服务器失联!");
            }
            if (!response.get().getJson().getBool("success", false)) {
                return serviceResponse.error(response.get().getJson().getStr("message", "创建群组失败"));
            }
            Group group = response.get().getJson().getBean("data", Group.class);
            if (!response.get().isJsonMessage() || group == null) {
                return serviceResponse.error("创建群组数据异常");
            }
            return serviceResponse.success(group);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 加入群组
     */
    public ServiceResponse<Group> joinGroup(int groupId) {
        ServiceResponse<Group> serviceResponse = new ServiceResponse<>(new Group());
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            UDPClientConnection conn = getConnection();
            String currentUsername = ClientMain.getCurrentUserName();

            JSONObject requestData = new JSONObject();
            requestData.set("group_id", groupId);
            requestData.set("username", currentUsername);

            Message message = Message.builder()
                    .setMessageType(MessageType.GROUP_JOIN)
                    .setSender(currentUsername)
                    .setJsonMessage(requestData);

            Message response = conn.sendMessageToServerSync(message);
            if (response == null) {
                return serviceResponse.error("服务器失联!");
            }

            if (!response.getJson().getBool("success", false)) {
                return serviceResponse.error(response.getJson().getStr("message", "加入群组失败"));
            }

            Group group = response.getJson().getBean("data", Group.class);
            if (!response.isJsonMessage() || group == null) {
                return serviceResponse.error("加入群组数据异常");
            }

            return serviceResponse.success(group);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 退出群组
     */
    public ServiceResponse<String> leaveGroup(int groupId) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            UDPClientConnection conn = getConnection();
            String currentUsername = ClientMain.getCurrentUserName();

            JSONObject requestData = new JSONObject();
            requestData.set("group_id", groupId);
            requestData.set("username", currentUsername);

            Message message = Message.builder()
                    .setMessageType(MessageType.GROUP_USER_LEAVE)
                    .setSender(currentUsername)
                    .setJsonMessage(requestData);

            Message response = conn.sendMessageToServerSync(message);
            if (response == null) {
                return serviceResponse.error("服务器失联!");
            }

            if (!MessageType.GROUP_USER_LEAVE.equals(response.getMessageType()) ||
                    !response.getJson().getBool("success", false)) {
                return serviceResponse.error(response.getJson().getStr("message", "退出群组失败"));
            }

            return serviceResponse.success("成功退出群组");
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 更新群组信息
     */
    public ServiceResponse<Group> updateGroupInfo(Group group, String username) {
        ServiceResponse<Group> serviceResponse = new ServiceResponse<>(new Group());
        try {
            // 参数验证
            if (group.getGroupId() <= 0) {
                return serviceResponse.error("群组ID无效");
            }
            String currentUsername = ClientMain.getCurrentUserName();
            JSONObject requestData = new JSONObject();
            boolean hasAvatar = group.getGroupAvatarPath() != null;
            if (hasAvatar) {
                File avatarFile = new File(group.getGroupAvatarPath());
                if (!avatarFile.exists()) {
                    return serviceResponse.error("头像不存在");
                }
                requestData.set("avatar_file_name", avatarFile.getName());
            }
            requestData.set("group_info", group);
            requestData.set("request_user", username);
            requestData.set("update", true);
            Message message = Message.builder()
                    .setMessageType(MessageType.TCP_GROUP_SAVE)
                    .setSender(currentUsername)
                    .setJsonMessage(requestData);
            if (hasAvatar) {
                message.setAttachment(FileUtil.readBytes(group.getGroupAvatarPath()), byte[].class, AttachmentType.GROUP_AVATAR);
            }
            Optional<Message> optMess = ClientMain.getTCPConnection().sendMessage(message);
            if (!optMess.isPresent()) {
                return serviceResponse.error("服务器失联!");
            }
            if (!optMess.get().isJsonMessage() || !optMess.get().getJson().getBool("success", false)) {
                return serviceResponse.error(optMess.get().getJson().getStr("message", "更新群组信息失败"));
            }
            Group responseGroup = optMess.get().getJson().getBean("data", Group.class);
            if (responseGroup == null) {
                return serviceResponse.error("更新群组信息数据异常");
            }
            return serviceResponse.success(responseGroup);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 删除群组
     */
    public ServiceResponse<String> deleteGroup(int groupId) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            UDPClientConnection conn = getConnection();
            String currentUsername = ClientMain.getCurrentUserName();

            JSONObject requestData = new JSONObject();
            requestData.set("group_id", groupId);
            requestData.set("requester_username", currentUsername);

            Message message = Message.builder()
                    .setMessageType(MessageType.GROUP_DELETE_REQUEST)
                    .setSender(currentUsername)
                    .setJsonMessage(requestData);

            Message response = conn.sendMessageToServerSync(message);
            if (response == null) {
                return serviceResponse.error("服务器失联!");
            }

            if (!MessageType.GROUP_DELETE_RESPONSE.equals(response.getMessageType()) ||
                    !response.getJson().getBool("success", false)) {
                return serviceResponse.error(response.getJson().getStr("message", "删除群组失败"));
            }

            return serviceResponse.success("成功删除群组");
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 移除群组成员
     */
    public ServiceResponse<String> removeMember(int groupId, String targetUsername) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            if (StrUtil.isEmpty(targetUsername)) {
                return serviceResponse.error("目标用户名不能为空");
            }

            UDPClientConnection conn = getConnection();
            String currentUsername = ClientMain.getCurrentUserName();

            JSONObject requestData = new JSONObject();
            requestData.set("group_id", groupId);
            requestData.set("remover_username", currentUsername);
            requestData.set("target_username", targetUsername);

            Message message = Message.builder()
                    .setMessageType(MessageType.GROUP_REMOVE_MEMBER)
                    .setSender(currentUsername)
                    .setJsonMessage(requestData);

            Message response = conn.sendMessageToServerSync(message);
            if (response == null) {
                return serviceResponse.error("服务器失联!");
            }

            if (!MessageType.GROUP_REMOVE_MEMBER.equals(response.getMessageType()) ||
                    !response.getJson().getBool("success", false)) {
                return serviceResponse.error(response.getJson().getStr("message", "移除成员失败"));
            }

            return serviceResponse.success("成功移除成员: " + targetUsername);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 转让群主身份
     */
    public ServiceResponse<String> transferOwnership(int groupId, String newOwner) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            if (StrUtil.isEmpty(newOwner)) {
                return serviceResponse.error("新群主用户名不能为空");
            }

            UDPClientConnection conn = getConnection();
            String currentUsername = ClientMain.getCurrentUserName();

            JSONObject requestData = new JSONObject();
            requestData.set("group_id", groupId);
            requestData.set("current_owner", currentUsername);
            requestData.set("new_owner", newOwner);

            Message message = Message.builder()
                    .setMessageType(MessageType.GROUP_TRANSFER_OWNER)
                    .setSender(currentUsername)
                    .setJsonMessage(requestData);

            Message response = conn.sendMessageToServerSync(message);
            if (response == null) {
                return serviceResponse.error("服务器失联!");
            }

            if (!response.getJson().getBool("success", false)) {
                return serviceResponse.error(response.getJson().getStr("message", "转让群主身份失败"));
            }

            return serviceResponse.success("成功转让群主身份给: " + newOwner);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }


    /**
     * 设置成员是否为管理员
     */
    public ServiceResponse<String> setGroupMemberIsAdmin(int groupId, String targetUser,boolean isAdmin) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            if (StrUtil.isEmpty(targetUser)) {
                return serviceResponse.error("目标用户名不能为空");
            }

            UDPClientConnection conn = getConnection();
            String currentUsername = ClientMain.getCurrentUserName();

            JSONObject requestData = new JSONObject();
            requestData.set("group_id", groupId);
            requestData.set("current_user", currentUsername);
            requestData.set("target_user", targetUser);
            requestData.set("is_admin", isAdmin);

            Message message = Message.builder()
                    .setMessageType(MessageType.GROUP_SET_ADMIN)
                    .setSender(currentUsername)
                    .setJsonMessage(requestData);

            Message response = conn.sendMessageToServerSync(message);
            if (response == null) {
                return serviceResponse.error("服务器失联!");
            }
            if (!response.getJson().getBool("success", false)) {
                return serviceResponse.error(response.getJson().getStr("message", "转让群主身份失败"));
            }
            return serviceResponse.success(null);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 获取群组成员列表
     */
    public ServiceResponse<List<GroupMember>> getGroupMembers(int groupId) {
        ServiceResponse<List<GroupMember>> serviceResponse = new ServiceResponse<>(new java.util.ArrayList<>());
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            UDPClientConnection conn = getConnection();
            String currentUsername = ClientMain.getCurrentUserName();

            JSONObject requestData = new JSONObject();
            requestData.set("group_id", groupId);
            requestData.set("requester_username", currentUsername);

            Message message = Message.builder()
                    .setMessageType(MessageType.GROUP_MEMBERS)
                    .setSender(currentUsername)
                    .setJsonMessage(requestData);

            Message response = conn.sendMessageToServerSync(message);
            if (response == null) {
                return serviceResponse.error("服务器失联!");
            }

            if (!response.getJson().getBool("success", false)) {
                return serviceResponse.error(response.getJson().getStr("message", "获取群组成员列表失败"));
            }

            List<GroupMember> members = response.getJson().getBeanList("data", GroupMember.class);
            if (!response.isJsonMessage()) {
                return serviceResponse.error("获取群组成员列表数据异常");
            }

            return serviceResponse.success(members);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 获取用户的群组列表
     */
    public ServiceResponse<List<Group>> getUserGroups() {
        ServiceResponse<List<Group>> serviceResponse = new ServiceResponse<>(new java.util.ArrayList<>());
        try {
            UDPClientConnection conn = getConnection();
            String currentUsername = ClientMain.getCurrentUserName();

            JSONObject requestData = new JSONObject();
            requestData.set("username", currentUsername);

            Message message = Message.builder()
                    .setMessageType(MessageType.USER_GROUPS)
                    .setSender(currentUsername)
                    .setJsonMessage(requestData);

            Message response = conn.sendMessageToServerSync(message);
            if (response == null) {
                return serviceResponse.error("服务器失联!");
            }

            if (!response.getJson().getBool("success", false)) {
                return serviceResponse.error(response.getJson().getStr("message", "获取用户群组列表失败"));
            }

            List<Group> groups = response.getJson().getBeanList("data", Group.class);
            if (!response.isJsonMessage()) {
                return serviceResponse.error("获取用户群组列表数据异常");
            }

            return serviceResponse.success(groups);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 搜索群组
     */
    public ServiceResponse<List<Group>> searchGroups(String keyword) {
        ServiceResponse<List<Group>> serviceResponse = new ServiceResponse<>(new java.util.ArrayList<>());
        try {
            // 参数验证
            if (StrUtil.isEmpty(keyword)) {
                return serviceResponse.error("搜索关键词不能为空");
            }

            UDPClientConnection conn = getConnection();
            String currentUsername = ClientMain.getCurrentUserName();

            JSONObject requestData = new JSONObject();
            requestData.set("keyword", keyword.trim());

            Message message = Message.builder()
                    .setMessageType(MessageType.GROUP_SEARCH)
                    .setSender(currentUsername)
                    .setJsonMessage(requestData);

            Message response = conn.sendMessageToServerSync(message);
            if (response == null) {
                return serviceResponse.error("服务器失联!");
            }

            if (!MessageType.GROUP_SEARCH.equals(response.getMessageType()) ||
                    !response.getJson().getBool("success", false)) {
                return serviceResponse.error(response.getJson().getStr("message", "搜索群组失败"));
            }

            List<Group> groups = response.getJson().getBeanList("data", Group.class);
            if (!response.isJsonMessage()) {
                return serviceResponse.error("搜索群组数据异常");
            }

            return serviceResponse.success(groups);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }
}