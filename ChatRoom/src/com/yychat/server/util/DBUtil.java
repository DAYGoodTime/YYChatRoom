package com.yychat.server.util;

import com.yychat.common.model.FriendType;
import com.yychat.common.model.Group;
import com.yychat.common.model.GroupMember;
import com.yychat.common.model.Message;
import com.yychat.common.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class DBUtil {
    private static final String db_url = "jdbc:mysql://localhost:3306/yychat2022s?useUnicode=true&characterEncoding=utf-8";
    private static final String db_user = "root";
    private static final String db_pass = "kel123";
    private static Connection dataBase;

    public static boolean connectDB() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            dataBase = DriverManager.getConnection(db_url, db_user, db_pass);
        } catch (Exception e) {
            System.out.println("数据库连接失败");
            return false;
        }
        return true;
    }

    public static boolean loginValidate(String userName, String password) {
        boolean loginSuccess = false;
        String query = "select * from user where username=? and password=?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            statement.setString(2, password);
            ResultSet rs = statement.executeQuery();
            loginSuccess = rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return loginSuccess;
    }

    public static boolean hasUser(String user) {
        boolean hasUser = false;
        String query = "select * from user where username=?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, user);
            ResultSet rs = statement.executeQuery();
            hasUser = rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasUser;
    }

    //添加新用户（支持头像路径）
    public static boolean addNewUser(User user) {
        int result = -1;
        String insert = "insert into user(username,password,avatar_path) values(?,?,?)";
        PreparedStatement statement;
        try {
            statement = dataBase.prepareStatement(insert);
            statement.setString(1, user.getUserName());
            statement.setString(2, user.getPassword());
            // 使用用户对象中的avatarPath，如果没有则使用默认值
            String avatarPath = user.getAvatarPath();
            if (avatarPath == null || avatarPath.trim().isEmpty()) {
                avatarPath = "0.jpg";
            }
            statement.setString(3, avatarPath);
            result = statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result > 0;
    }

    public static List<String> getAllFriends(String userName, int friendType) {
        List<String> friendList = new ArrayList<>();
        String query = "select slaveUser from userRelation where masterUser=? and relation=?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            statement.setInt(2, friendType);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                friendList.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
        return friendList;
    }

    public static List<User> getUnknowUsers(String userName) {
        List<User> result = new ArrayList<>();
        String query = "select u.username,u.avatar_path from user as u " +
                "left join userrelation as ur on u.username = ur.slaveuser and ur.masteruser =? " +
                "where ur.masteruser is null and u.username !=?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            statement.setString(2, userName);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                result.add(new  User(rs.getString(1),null, rs.getString(2)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean isUsersFriend(String userName, String userFriend, FriendType friendType) {
        int friendTypeCode = friendType.getCode();
        boolean result = false;
        String query = "select * from userRelation where masterUser=? and slaveUser=? and relation=?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            statement.setString(2, userFriend);
            statement.setInt(3, friendTypeCode);
            ResultSet rs = statement.executeQuery();
            result = rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static int insertIntoFriend(String user, String friendOfUser, FriendType friendType) {
        int friendTypeCode = friendType.getCode();
        int count = 0;
        String query = "insert into userRelation(masterUser,slaveUser,relation) values(?,?,?)";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, user);
            statement.setString(2, friendOfUser);
            statement.setInt(3, friendTypeCode);
            count = statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public static boolean insertChatMessage(String from, String to, String content, LocalDateTime time) {
        boolean result = false;
        String query = "insert into message(sender,receiver,content,sendtime) values(?,?,?,?)";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, from);
            statement.setString(2, to);
            statement.setString(3, content);
            statement.setTimestamp(4, new Timestamp(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
            result = (statement.executeUpdate() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 更新用户头像路径
     *
     * @param userName   用户名
     * @param avatarPath 头像路径
     * @return 更新是否成功
     */
    public static boolean updateUserAvatar(String userName, String avatarPath) {
        boolean result = false;
        String query = "UPDATE user SET avatar_path=? WHERE username=?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, avatarPath);
            statement.setString(2, userName);
            result = (statement.executeUpdate() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取用户头像路径
     *
     * @param userName 用户名
     * @return 头像路径，如果用户不存在返回默认头像
     */
    public static String getUserAvatar(String userName) {
        String avatarPath = "0.jpg"; // 默认头像
        String query = "SELECT avatar_path FROM user WHERE username=?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                avatarPath = rs.getString("avatar_path");
                if (avatarPath == null || avatarPath.trim().isEmpty()) {
                    avatarPath = "0.jpg"; // 如果数据库中为空，返回默认头像
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return avatarPath;
    }

    /**
     * 获取用户完整信息（包括头像路径）
     *
     * @param userName 用户名
     * @return 用户对象，如果用户不存在返回null
     */
    public static User getUserInfo(String userName) {
        User user = null;
        String query = "SELECT username, avatar_path FROM user WHERE username=?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                user = new User();
                user.setUserName(rs.getString("username"));
                String avatarPath = rs.getString("avatar_path");
                if (avatarPath != null && !avatarPath.trim().isEmpty()) {
                    user.setAvatarPath(avatarPath);
                } else {
                    user.setAvatarPath("0.jpg"); // 默认头像
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

    // ================================
    // 群组管理相关方法
    // ================================

    /**
     * 创建群组
     *
     * @param group 群组对象
     * @return 创建成功返回群组ID，失败返回-1
     */
    public static int createGroup(Group group) {
        int groupId = -1;
        String insert = "INSERT INTO `groups`(group_name, group_avatar_path,creator_username,create_time) VALUES(?,?,?,?)";
        PreparedStatement statement = null;
        try {
            // 开始事务
            dataBase.setAutoCommit(false);
            // 插入群组基本信息
            statement = dataBase.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, group.getGroupName());
            statement.setString(2, group.getGroupAvatarPath());
            statement.setString(3, group.getCreatorUsername());
            statement.setTimestamp(4, new Timestamp(group.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
            int result = statement.executeUpdate();
            if (result > 0) {
                // 获取生成的群组ID
                ResultSet rs = statement.getGeneratedKeys();
                if (rs.next()) {
                    groupId = rs.getInt(1);
                    // 添加创建者为群主
                    addGroupMember(groupId, group.getCreatorUsername(), GroupMember.ROLE_OWNER);
                }
            }

            // 提交事务
            dataBase.commit();
        } catch (Exception e) {
            try {
                // 回滚事务
                dataBase.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                // 恢复自动提交
                dataBase.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return groupId;
    }

    /**
     * 获取用户的群组列表
     *
     * @param username 用户名
     * @return 群组列表
     */
    public static List<Group> getUserGroups(String username) {
        List<Group> groups = new ArrayList<>();
        String query = "SELECT g.group_id, g.group_name, g.group_avatar_path, " +
                "gm.role, gm.join_time, COUNT(gm2.username) as member_count " +
                "FROM `groups` g " +
                "JOIN group_members gm ON g.group_id = gm.group_id " +
                "LEFT JOIN group_members gm2 ON g.group_id = gm2.group_id " +
                "WHERE gm.username = ? " +
                "GROUP BY g.group_id, g.group_name, g.group_avatar_path, gm.role, gm.join_time";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, username);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                Group group = new Group();
                group.setGroupId(rs.getInt("group_id"));
                group.setGroupName(rs.getString("group_name"));
                group.setGroupAvatarPath(rs.getString("group_avatar_path"));
                group.setCreatorUsername(getGroupOwner(rs.getInt("group_id")));
                group.setMemberCount(rs.getInt("member_count"));

                // 获取创建时间（使用第一个成员加入时间作为参考）
                String creatorQuery = "SELECT join_time FROM group_members WHERE group_id = ? AND role = ?";
                PreparedStatement creatorStmt = dataBase.prepareStatement(creatorQuery);
                creatorStmt.setInt(1, rs.getInt("group_id"));
                creatorStmt.setString(2, GroupMember.ROLE_OWNER);
                ResultSet creatorRs = creatorStmt.executeQuery();
                if (creatorRs.next()) {
                    group.setCreateTime(creatorRs.getTimestamp("join_time").toLocalDateTime());
                }
                creatorRs.close();
                creatorStmt.close();

                groups.add(group);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groups;
    }

    /**
     * 根据群组ID获取群组信息
     *
     * @param groupId 群组ID
     * @return 群组对象，不存在返回null
     */
    public static Group getGroupById(int groupId) {
        Group group = null;
        String query = "SELECT group_id,group_name, group_avatar_path,creator_username,member_count,create_time FROM `groups` WHERE group_id = ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setInt(1, groupId);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                group = new Group();
                group.setGroupId(groupId);
                group.setGroupName(rs.getString("group_name"));
                group.setGroupAvatarPath(rs.getString("group_avatar_path"));
                group.setCreatorUsername(rs.getString("creator_username"));
                group.setMemberCount(rs.getInt("member_count"));
                group.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return group;
    }
    /**
     * 根据群组ID获取群组信息
     *
     * @param groupName 群组ID
     * @return 群组对象，不存在返回null
     */
    public static Group getGroupByName(String groupName) {
        Group group = null;
        String query = "SELECT group_id,group_name, group_avatar_path,creator_username,member_count,create_time FROM `groups` WHERE group_name = ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, groupName);
            ResultSet rs = statement.executeQuery();

            if (rs.next()) {
                group = new Group();
                group.setGroupId(rs.getInt("group_id"));
                group.setGroupName(rs.getString("group_name"));
                group.setGroupAvatarPath(rs.getString("group_avatar_path"));
                group.setCreatorUsername(rs.getString("creator_username"));
                group.setMemberCount(rs.getInt("member_count"));
                group.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return group;
    }
    /**
     * 更新群组信息
     *
     * @param group 群组对象
     * @return 更新是否成功
     */
    public static boolean updateGroupInfo(Group group) {
        boolean result = false;
        String update = "UPDATE `groups` SET group_name = ?, group_avatar_path = ? WHERE group_id = ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(update);
            statement.setString(1, group.getGroupName());
            statement.setString(2, group.getGroupAvatarPath());
            statement.setInt(3, group.getGroupId());
            result = (statement.executeUpdate() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 删除群组
     *
     * @param groupId 群组ID
     * @return 删除是否成功
     */
    public static boolean deleteGroup(int groupId) {
        boolean result = false;
        try {
            // 开始事务
            dataBase.setAutoCommit(false);

            // 删除群组成员关系
            String deleteMembers = "DELETE FROM group_members WHERE group_id = ?";
            PreparedStatement stmt1 = dataBase.prepareStatement(deleteMembers);
            stmt1.setInt(1, groupId);
            stmt1.executeUpdate();

            // 删除群组消息
            String deleteMessages = "DELETE FROM group_messages WHERE group_id = ?";
            PreparedStatement stmt2 = dataBase.prepareStatement(deleteMessages);
            stmt2.setInt(1, groupId);
            stmt2.executeUpdate();

            // 删除群组
            String deleteGroup = "DELETE FROM `groups` WHERE group_id = ?";
            PreparedStatement stmt3 = dataBase.prepareStatement(deleteGroup);
            stmt3.setInt(1, groupId);
            int deleteCount = stmt3.executeUpdate();

            if (deleteCount > 0) {
                result = true;
                dataBase.commit(); // 提交事务
            } else {
                dataBase.rollback(); // 回滚事务
            }

            stmt1.close();
            stmt2.close();
            stmt3.close();
        } catch (Exception e) {
            try {
                dataBase.rollback(); // 回滚事务
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                dataBase.setAutoCommit(true); // 恢复自动提交
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 添加群组成员
     *
     * @param groupId 群组ID
     * @param username 用户名
     * @param role 角色（默认为member）
     * @return 添加是否成功
     */
    public static boolean addGroupMember(int groupId, String username, String role) {
        boolean result = false;
        String insert = "INSERT INTO group_members(group_id, username, join_time, role) VALUES(?,?,?,?)";
        PreparedStatement statement = null;
        try {
            // 检查是否已经是成员
            if (isGroupMember(groupId, username)) {
                return false;
            }

            statement = dataBase.prepareStatement(insert);
            statement.setInt(1, groupId);
            statement.setString(2, username);
            statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
            statement.setString(4, role != null ? role : GroupMember.ROLE_MEMBER);
            result = (statement.executeUpdate() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 移除群组成员
     *
     * @param groupId 群组ID
     * @param username 用户名
     * @return 移除是否成功
     */
    public static boolean removeGroupMember(int groupId, String username) {
        boolean result = false;
        String delete = "DELETE FROM group_members WHERE group_id = ? AND username = ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(delete);
            statement.setInt(1, groupId);
            statement.setString(2, username);
            result = (statement.executeUpdate() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取群组成员列表
     *
     * @param groupId 群组ID
     * @return 成员列表
     */
    public static List<GroupMember> getGroupMembers(int groupId) {
        List<GroupMember> members = new ArrayList<>();
        String query = "SELECT id, username, join_time, role FROM group_members WHERE group_id = ? ORDER BY join_time";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setInt(1, groupId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                GroupMember member = new GroupMember();
                member.setId(rs.getLong("id"));
                member.setGroupId(groupId);
                member.setUsername(rs.getString("username"));
                member.setJoinTime(rs.getTimestamp("join_time").toLocalDateTime());
                member.setRole(rs.getString("role"));
                members.add(member);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return members;
    }

    /**
     * 检查用户是否为群组成员
     *
     * @param groupId 群组ID
     * @param username 用户名
     * @return 是否为成员
     */
    public static boolean isGroupMember(int groupId, String username) {
        boolean result = false;
        String query = "SELECT * FROM group_members WHERE group_id = ? AND username = ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setInt(1, groupId);
            statement.setString(2, username);
            ResultSet rs = statement.executeQuery();
            result = rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取群组成员数量
     *
     * @param groupId 群组ID
     * @return 成员数量
     */
    public static int getGroupMemberCount(int groupId) {
        int count = 0;
        String query = "SELECT COUNT(*) as count FROM group_members WHERE group_id = ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setInt(1, groupId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                count = rs.getInt("count");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    /**
     * 获取群主用户名
     *
     * @param groupId 群组ID
     * @return 群主用户名，如果不存在返回null
     */
    public static String getGroupOwner(int groupId) {
        String owner = null;
        String query = "SELECT username FROM group_members WHERE group_id = ? AND role = ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setInt(1, groupId);
            statement.setString(2, GroupMember.ROLE_OWNER);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                owner = rs.getString("username");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return owner;
    }

    /**
     * 获取群组创建时间
     *
     * @param groupId 群组ID
     * @return 创建时间
     */
    public static LocalDateTime getGroupCreateTime(int groupId) {
        LocalDateTime createTime = LocalDateTime.now();
        String query = "SELECT join_time FROM group_members WHERE group_id = ? AND role = ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setInt(1, groupId);
            statement.setString(2, GroupMember.ROLE_OWNER);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                createTime = rs.getTimestamp("join_time").toLocalDateTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return createTime;
    }

    /**
     * 保存群组消息
     *
     * @param message 消息对象
     * @param groupId 群组ID
     * @return 保存是否成功
     */
    public static boolean saveGroupMessage(Message message, int groupId) {
        boolean result = false;
        String insert = "INSERT INTO group_messages(sender_name, group_id, content, time) VALUES(?,?,?,?)";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(insert);
            statement.setString(1, message.getSender());
            statement.setInt(2, groupId);
            // 如果是JSON消息，保存JSON内容；否则保存普通文本内容
            if (message.isJsonMessage()) {
                statement.setString(3, message.getJson().toJSONString(0));
            } else {
                statement.setString(3, message.getContent());
            }
            statement.setTimestamp(4, new Timestamp(message.getTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
            result = (statement.executeUpdate() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 获取群组消息历史
     *
     * @param groupId 群组ID
     * @param limit 返回消息数量限制
     * @return 消息列表
     */
    public static List<Message> getGroupMessageHistory(int groupId, int limit) {
        List<Message> messages = new ArrayList<>();
        String query = "SELECT sender_name, content, time FROM group_messages WHERE group_id = ? ORDER BY time DESC LIMIT ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setInt(1, groupId);
            statement.setInt(2, limit);
            ResultSet rs = statement.executeQuery();

            // 从最新消息开始，需要反转顺序
            List<Message> tempList = new ArrayList<>();
            while (rs.next()) {
                Message message = Message.builder()
                .setSender(rs.getString("sender_name"))
                .setContent(rs.getString("content"))
                .setTime(rs.getTimestamp("time").toLocalDateTime());
                tempList.add(message);
            }

            // 反转列表，按时间正序返回
            for (int i = tempList.size() - 1; i >= 0; i--) {
                messages.add(tempList.get(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return messages;
    }

    /**
     * 搜索群组
     *
     * @param keyword 搜索关键词
     * @return 匹配的群组列表
     */
    public static List<Group> searchGroups(String keyword) {
        List<Group> groups = new ArrayList<>();
        String query = "SELECT DISTINCT g.group_id, g.group_name, g.group_avatar_path, " +
                "COUNT(gm.username) as member_count " +
                "FROM `groups` g " +
                "LEFT JOIN group_members gm ON g.group_id = gm.group_id " +
                "WHERE g.group_name LIKE ? " +
                "GROUP BY g.group_id, g.group_name, g.group_avatar_path " +
                "ORDER BY member_count DESC";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, "%" + keyword + "%");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                Group group = new Group();
                group.setGroupId(rs.getInt("group_id"));
                group.setGroupName(rs.getString("group_name"));
                group.setGroupAvatarPath(rs.getString("group_avatar_path"));
                group.setCreatorUsername(getGroupOwner(rs.getInt("group_id")));
                group.setMemberCount(rs.getInt("member_count"));
                groups.add(group);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groups;
    }

    /**
     * 更新群组成员角色
     *
     * @param groupId 群组ID
     * @param username 用户名
     * @param newRole 新角色
     * @return 更新是否成功
     */
    public static boolean updateMemberRole(int groupId, String username, String newRole) {
        boolean result = false;
        String update = "UPDATE group_members SET role = ? WHERE group_id = ? AND username = ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(update);
            statement.setString(1, newRole);
            statement.setInt(2, groupId);
            statement.setString(3, username);
            result = (statement.executeUpdate() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean hasGroup(String groupName) {
        boolean hasUser = false;
        String query = "select * from `groups` where group_name =?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, groupName);
            ResultSet rs = statement.executeQuery();
            hasUser = rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasUser;
    }
}
