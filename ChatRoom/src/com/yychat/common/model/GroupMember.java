package com.yychat.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class GroupMember implements Serializable {
    private static final long serialVersionUID = 1L;

    // 群组成员角色常量
    public static final String ROLE_OWNER = "owner";        // 群主
    public static final String ROLE_ADMIN = "admin";        // 管理员
    public static final String ROLE_MEMBER = "member";      // 普通成员

    private long id;
    private int groupId;
    private String username;
    private LocalDateTime joinTime;
    private String role;
    private boolean isOnline;

    // 默认构造函数
    public GroupMember() {
        this.joinTime = LocalDateTime.now();
        this.role = ROLE_MEMBER;
        this.isOnline = false;
    }

    // 主要构造函数
    public GroupMember(int groupId, String username) {
        this();
        this.groupId = groupId;
        this.username = username;
    }

    // 完整构造函数
    public GroupMember(long id, int groupId, String username, LocalDateTime joinTime, String role) {
        this.id = id;
        this.groupId = groupId;
        this.username = username;
        this.joinTime = joinTime;
        this.role = role;
        this.isOnline = false;
    }

    // Builder模式
    public static class Builder {
        private GroupMember member = new GroupMember();

        public Builder id(long id) {
            member.setId(id);
            return this;
        }

        public Builder groupId(int groupId) {
            member.setGroupId(groupId);
            return this;
        }

        public Builder username(String username) {
            member.setUsername(username);
            return this;
        }

        public Builder joinTime(LocalDateTime joinTime) {
            member.setJoinTime(joinTime);
            return this;
        }

        public Builder role(String role) {
            member.setRole(role);
            return this;
        }

        public Builder isOnline(boolean isOnline) {
            member.setIsOnline(isOnline);
            return this;
        }

        public GroupMember build() {
            return member;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getter方法
    public long getId() {
        return id;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getJoinTime() {
        return joinTime;
    }

    public String getRole() {
        return role;
    }

    public boolean isOnline() {
        return isOnline;
    }

    // Setter方法
    public void setId(long id) {
        this.id = id;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setIsOnline(boolean isOnline) {
        this.isOnline = isOnline;
    }

    // 角色判断方法
    public boolean isOwner() {
        return ROLE_OWNER.equals(role);
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role) || ROLE_OWNER.equals(role);
    }

    public boolean isMember() {
        return ROLE_MEMBER.equals(role);
    }

    // 权限判断方法
    public boolean canManage() {
        return isAdmin();
    }

    public boolean canRemoveMember(String targetUsername) {
        // 群主可以移除任何人，管理员可以移除普通成员
        if (isOwner()) {
            return true;
        }
        if (isAdmin() && !targetUsername.equals(username)) {
            return true;
        }
        return false;
    }

    public boolean canTransferOwner() {
        return isOwner();
    }
}