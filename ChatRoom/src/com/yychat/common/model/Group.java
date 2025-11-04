package com.yychat.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;

    private int groupId;
    private String groupName;
    private String groupAvatarPath;
    private String creatorUsername;
    private LocalDateTime createTime;
    private int memberCount;
    private List<GroupMember> members;

    // 默认构造函数
    public Group() {
        this.createTime = LocalDateTime.now();
    }

    // 主要构造函数
    public Group(int groupId, String groupName, String groupAvatarPath, String creatorUsername) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupAvatarPath = groupAvatarPath;
        this.creatorUsername = creatorUsername;
        this.createTime = LocalDateTime.now();
    }

    public Group(String groupName, String groupAvatarPath, String creatorUsername) {
        this.groupName = groupName;
        this.groupAvatarPath = groupAvatarPath;
        this.creatorUsername = creatorUsername;
        this.createTime = LocalDateTime.now();
        this.memberCount = 0;
    }

    // 完整构造函数
    public Group(int groupId, String groupName, String groupAvatarPath, String creatorUsername,
                 LocalDateTime createTime, int memberCount, List<GroupMember> members) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupAvatarPath = groupAvatarPath;
        this.creatorUsername = creatorUsername;
        this.createTime = createTime;
        this.memberCount = memberCount;
        this.members = members;
    }

    // Builder模式
    public static class Builder {
        private Group group = new Group();

        public Builder groupId(int groupId) {
            group.setGroupId(groupId);
            return this;
        }

        public Builder groupName(String groupName) {
            group.setGroupName(groupName);
            return this;
        }

        public Builder groupAvatarPath(String groupAvatarPath) {
            group.setGroupAvatarPath(groupAvatarPath);
            return this;
        }

        public Builder creatorUsername(String creatorUsername) {
            group.setCreatorUsername(creatorUsername);
            return this;
        }

        public Builder createTime(LocalDateTime createTime) {
            group.setCreateTime(createTime);
            return this;
        }

        public Builder memberCount(int memberCount) {
            group.setMemberCount(memberCount);
            return this;
        }

        public Builder members(List<GroupMember> members) {
            group.setMembers(members);
            return this;
        }

        public Group build() {
            return group;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // Getter方法
    public int getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getGroupAvatarPath() {
        return groupAvatarPath;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public List<GroupMember> getMembers() {
        return members;
    }

    // Setter方法
    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public void setGroupAvatarPath(String groupAvatarPath) {
        this.groupAvatarPath = groupAvatarPath;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public void setMembers(List<GroupMember> members) {
        this.members = members;
    }

    // 辅助方法
    public boolean isOwner(String username) {
        return creatorUsername != null && creatorUsername.equals(username);
    }

    public boolean isMember(String username) {
        if (members == null || members.isEmpty()) {
            return false;
        }
        return members.stream()
                .anyMatch(member -> username.equals(member.getUsername()));
    }

    @Override
    public String toString() {
        return "Group{" +
                "groupId=" + groupId +
                ", groupName='" + groupName + '\'' +
                ", creatorUsername='" + creatorUsername + '\'' +
                ", memberCount=" + memberCount +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Group group = (Group) obj;
        return groupId == group.groupId;
    }

    @Override
    public int hashCode() {
        return groupId;
    }
}