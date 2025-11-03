package com.yychat.server.service;

import com.yychat.common.model.Constant;
import com.yychat.common.model.Group;
import com.yychat.common.model.GroupMember;
import com.yychat.common.model.ServiceResponse;
import com.yychat.server.util.DBUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务器端群组服务类
 * 负责处理群组相关的业务逻辑
 */
public class GroupService {

    private static final GroupService instance = new GroupService();

    public static GroupService getInstance() {
        return instance;
    }

    private GroupService() {
    }

    /**
     * 创建群组
     */
    public ServiceResponse<Group> createGroup(String groupName, String creatorUsername, String avatarPath) {
        ServiceResponse<Group> serviceResponse = new ServiceResponse<>(new Group());
        try {
            // 参数验证
            if (groupName == null || groupName.trim().isEmpty()) {
                return serviceResponse.error("群组名称不能为空");
            }

            if (creatorUsername == null || creatorUsername.trim().isEmpty()) {
                return serviceResponse.error("创建者用户名不能为空");
            }
            if(DBUtil.hasGroup(groupName)){
                return serviceResponse.error("该群名已被使用");
            }

            // 使用默认头像路径
            String groupAvatarPath = avatarPath != null ? avatarPath : Constant.DEFAULT_AVATAR;

            // 创建群组对象
            Group group = new Group();
            group.setGroupName(groupName.trim());
            group.setGroupAvatarPath(groupAvatarPath);
            group.setCreatorUsername(creatorUsername);
            group.setCreateTime(java.time.LocalDateTime.now());

            // 调用数据库操作
            int groupId = DBUtil.createGroup(group);
            if (groupId > 0) {
                group.setGroupId(groupId);
                group.setMemberCount(1);
                return serviceResponse.success(group);
            } else {
                return serviceResponse.error("群组创建失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getLocalizedMessage());
        }
    }

    /**
     * 加入群组
     */
    public ServiceResponse<Group> joinGroup(int groupId, String username) {
        ServiceResponse<Group> serviceResponse = new ServiceResponse<>(new Group());
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }
            if (username == null || username.trim().isEmpty()) {
                return serviceResponse.error("用户名不能为空");
            }
            // 检查群组是否存在
            Group group = DBUtil.getGroupById(groupId);
            if (group == null) {
                return serviceResponse.error("群组不存在");
            }

            // 检查是否已经是成员
            if (DBUtil.isGroupMember(groupId, username)) {
                return serviceResponse.error("您已经是群组成员");
            }

            // 添加群组成员
            boolean success = DBUtil.addGroupMember(groupId, username, GroupMember.ROLE_MEMBER);
            if (success) {
                // 重新获取群组信息
                Group updatedGroup = DBUtil.getGroupById(groupId);
                updatedGroup.setMemberCount(DBUtil.getGroupMemberCount(groupId));
                return serviceResponse.success(updatedGroup);
            } else {
                return serviceResponse.error("数据库异常");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getLocalizedMessage());
        }
    }

    /**
     * 退出群组
     */
    public ServiceResponse<String> leaveGroup(int groupId, String username) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            if (username == null || username.trim().isEmpty()) {
                return serviceResponse.error("用户名不能为空");
            }

            // 检查群组是否存在
            Group group = DBUtil.getGroupById(groupId);
            if (group == null) {
                return serviceResponse.error("群组不存在");
            }

            // 检查是否为成员
            if (!DBUtil.isGroupMember(groupId, username)) {
                return serviceResponse.error("您不是群组成员");
            }

            // 群主不能退出群组（除非删除整个群组）
            if (group.isOwner(username)) {
                return serviceResponse.error("群主不能退出群组，如需解散请使用删除群组功能");
            }

            // 移除群组成员
            boolean success = DBUtil.removeGroupMember(groupId, username);
            if (success) {
                return serviceResponse.success("成功退出群组");
            } else {
                return serviceResponse.error("退出群组失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 更新群组信息
     */
    public ServiceResponse<Group> updateGroupInfo(int groupId, String groupName, String avatarPath, String requesterUsername) {
        ServiceResponse<Group> serviceResponse = new ServiceResponse<>(new Group());
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            if (requesterUsername == null || requesterUsername.trim().isEmpty()) {
                return serviceResponse.error("请求者用户名不能为空");
            }

            // 检查群组是否存在
            Group group = DBUtil.getGroupById(groupId);
            if (group == null) {
                return serviceResponse.error("群组不存在");
            }

            // 权限检查：只有群主和管理员可以更新群组信息
            if (!isGroupOwner(groupId, requesterUsername) && !isGroupAdmin(groupId, requesterUsername)) {
                return serviceResponse.error("权限不足，只有群主和管理员可以更新群组信息");
            }

            // 验证群组名称
            if (groupName != null && !groupName.trim().isEmpty()) {
                group.setGroupName(groupName.trim());
            }

            // 设置头像路径
            if (avatarPath != null && !avatarPath.trim().isEmpty()) {
                group.setGroupAvatarPath(avatarPath);
            }

            // 更新群组信息
            boolean success = DBUtil.updateGroupInfo(group);
            if (success) {
                Group updatedGroup = DBUtil.getGroupById(groupId);
                updatedGroup.setMemberCount(DBUtil.getGroupMemberCount(groupId));
                return serviceResponse.success(updatedGroup);
            } else {
                return serviceResponse.error("群组信息更新失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 删除群组（只有群主可以删除）
     */
    public ServiceResponse<?> deleteGroup(int groupId, String requesterUsername) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            if (requesterUsername == null || requesterUsername.trim().isEmpty()) {
                return serviceResponse.error("请求者用户名不能为空");
            }

            // 检查群组是否存在
            Group group = DBUtil.getGroupById(groupId);
            if (group == null) {
                return serviceResponse.error("群组不存在");
            }

            // 权限检查：只有群主可以删除群组
            if (!isGroupOwner(groupId, requesterUsername)) {
                return serviceResponse.error("权限不足，只有群主可以删除群组");
            }

            // 删除群组
            boolean success = DBUtil.deleteGroup(groupId);
            if (success) {
                return serviceResponse.success("群组删除成功");
            } else {
                return serviceResponse.error("群组删除失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getLocalizedMessage());
        }
    }

    /**
     * 添加群组成员
     */
    public ServiceResponse<?> addMember(int groupId, String adderUsername, String targetUsername) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            if (adderUsername == null || adderUsername.trim().isEmpty()) {
                return serviceResponse.error("操作者用户名不能为空");
            }

            if (targetUsername == null || targetUsername.trim().isEmpty()) {
                return serviceResponse.error("目标用户名不能为空");
            }

            // 检查群组是否存在
            Group group = DBUtil.getGroupById(groupId);
            if (group == null) {
                return serviceResponse.error("群组不存在");
            }

            // 权限检查：只有群主和管理员可以添加成员
            if (!isGroupOwner(groupId, adderUsername) && !isGroupAdmin(groupId, adderUsername)) {
                return serviceResponse.error("权限不足，只有群主和管理员可以添加成员");
            }

            // 检查目标用户是否存在
            if (!DBUtil.hasUser(targetUsername)) {
                return serviceResponse.error("目标用户不存在");
            }

            // 添加群组成员
            boolean success = DBUtil.addGroupMember(groupId, targetUsername, GroupMember.ROLE_MEMBER);
            if (success) {
                return serviceResponse.success("成功添加成员: " + targetUsername);
            } else {
                return serviceResponse.error("添加成员失败，可能已经是群组成员");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getLocalizedMessage());
        }
    }

    /**
     * 移除群组成员
     */
    public ServiceResponse<?> removeMember(int groupId, String removerUsername, String targetUsername) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            if (removerUsername == null || removerUsername.trim().isEmpty()) {
                return serviceResponse.error("操作者用户名不能为空");
            }

            if (targetUsername == null || targetUsername.trim().isEmpty()) {
                return serviceResponse.error("目标用户名不能为空");
            }

            // 检查群组是否存在
            Group group = DBUtil.getGroupById(groupId);
            if (group == null) {
                return serviceResponse.error("群组不存在");
            }

            // 检查目标用户是否为成员
            if (!DBUtil.isGroupMember(groupId, targetUsername)) {
                return serviceResponse.error("目标用户不是群组成员");
            }

            // 权限检查：群主可以移除任何人，管理员可以移除普通成员
            List<GroupMember> members = DBUtil.getGroupMembers(groupId);
            GroupMember remover = members.stream()
                    .filter(m -> removerUsername.equals(m.getUsername()))
                    .findFirst()
                    .orElse(null);

            GroupMember target = members.stream()
                    .filter(m -> targetUsername.equals(m.getUsername()))
                    .findFirst()
                    .orElse(null);

            if (remover == null) {
                return serviceResponse.error("您不是群组成员");
            }

            if (target == null) {
                return serviceResponse.error("目标用户不是群组成员");
            }

            // 权限检查
            if (!remover.canRemoveMember(targetUsername)) {
                return serviceResponse.error("权限不足，无法移除该成员");
            }

            // 移除群组成员
            boolean success = DBUtil.removeGroupMember(groupId, targetUsername);
            if (success) {
                return serviceResponse.success("成功移除成员: " + targetUsername);
            } else {
                return serviceResponse.error("移除成员失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 转让群主身份
     */
    public ServiceResponse<?> transferOwnership(int groupId, String currentOwner, String newOwner) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            if (currentOwner == null || currentOwner.trim().isEmpty()) {
                return serviceResponse.error("当前群主用户名不能为空");
            }

            if (newOwner == null || newOwner.trim().isEmpty()) {
                return serviceResponse.error("新群主用户名不能为空");
            }

            // 检查群组是否存在
            Group group = DBUtil.getGroupById(groupId);
            if (group == null) {
                return serviceResponse.error("群组不存在");
            }

            // 权限检查：只有当前群主可以转让
            if (!isGroupOwner(groupId, currentOwner)) {
                return serviceResponse.error("权限不足，只有群主可以转让群主身份");
            }

            // 检查新群主是否为成员
            if (!DBUtil.isGroupMember(groupId, newOwner)) {
                return serviceResponse.error("新群主必须是群组成员");
            }

            // 更新角色：原群主降为管理员，新群主升为群主
            boolean transferOwnerSuccess = DBUtil.updateMemberRole(groupId, newOwner, GroupMember.ROLE_OWNER);
            boolean demoteOwnerSuccess = DBUtil.updateMemberRole(groupId, currentOwner, GroupMember.ROLE_ADMIN);

            if (transferOwnerSuccess && demoteOwnerSuccess) {
                return serviceResponse.success("群主身份转让成功");
            } else {
                return serviceResponse.error("群主身份转让失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getLocalizedMessage());
        }
    }

    /**
     * 获取群组成员列表
     */
    public ServiceResponse<List<GroupMember>> getGroupMembers(int groupId, String requesterUsername) {
        ServiceResponse<List<GroupMember>> serviceResponse = new ServiceResponse<>(new ArrayList<>());
        try {
            // 参数验证
            if (groupId <= 0) {
                return serviceResponse.error("群组ID无效");
            }

            if (requesterUsername == null || requesterUsername.trim().isEmpty()) {
                return serviceResponse.error("请求者用户名不能为空");
            }

            // 检查群组是否存在
            Group group = DBUtil.getGroupById(groupId);
            if (group == null) {
                return serviceResponse.error("群组不存在");
            }

            // 检查请求者是否为群组成员
            if (!DBUtil.isGroupMember(groupId, requesterUsername)) {
                return serviceResponse.error("您不是群组成员，无权查看群组成员列表");
            }

            // 获取群组成员列表
            List<GroupMember> members = DBUtil.getGroupMembers(groupId);
            return serviceResponse.success(members);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 获取用户的群组列表
     */
    public ServiceResponse<List<Group>> getUserGroups(String username) {
        ServiceResponse<List<Group>> serviceResponse = new ServiceResponse<>(new ArrayList<>());
        try {
            // 参数验证
            if (username == null || username.trim().isEmpty()) {
                return serviceResponse.error("用户名不能为空");
            }

            // 获取用户群组列表
            List<Group> groups = DBUtil.getUserGroups(username);
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
        ServiceResponse<List<Group>> serviceResponse = new ServiceResponse<>(new ArrayList<>());
        try {
            // 参数验证
            if (keyword == null || keyword.trim().isEmpty()) {
                return serviceResponse.error("搜索关键词不能为空");
            }

            // 搜索群组
            List<Group> groups = DBUtil.searchGroups(keyword.trim());
            return serviceResponse.success(groups);
        } catch (Exception e) {
            e.printStackTrace();
            return serviceResponse.error("服务器异常: " + e.getMessage());
        }
    }

    /**
     * 检查是否为群主
     */
    private boolean isGroupOwner(int groupId, String username) {
        try {
            String owner = DBUtil.getGroupOwner(groupId);
            return owner != null && owner.equals(username);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查是否为群主或管理员
     */
    private boolean isGroupAdmin(int groupId, String username) {
        try {
            List<GroupMember> members = DBUtil.getGroupMembers(groupId);
            return members.stream()
                    .filter(member -> username.equals(member.getUsername()))
                    .anyMatch(GroupMember::isAdmin);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}