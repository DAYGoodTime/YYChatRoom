package com.yychat.server.util;

import cn.hutool.json.JSONUtil;
import com.yychat.common.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("CallToPrintStackTrace")
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

    //添加新用户
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
                result.add(new User(rs.getString(1), null, rs.getString(2)));
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

    public static long insertChatMessage(String from, String to, String content, LocalDateTime time) {
        String query = "insert into message(sender,receiver,content,sendtime) values(?,?,?,?)";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, from);
            statement.setString(2, to);
            statement.setString(3, content);
            statement.setTimestamp(4, new Timestamp(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
            boolean result = (statement.executeUpdate() > 0);
            if (!result) return -1L;
            ResultSet rs = statement.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1L;
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
        String avatarPath = null;
        String query = "SELECT avatar_path FROM user WHERE username=?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                avatarPath = rs.getString("avatar_path");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (avatarPath == null || avatarPath.trim().isEmpty()) {
            avatarPath = Constant.DEFAULT_AVATAR; // 如果数据库中为空，返回默认头像
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
                user.setAvatarPath(avatarPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return user;
    }

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
        String query = "SELECT g.group_id," +
                "       g.group_name," +
                "       g.group_avatar_path," +
                "       g.creator_username," +
                "       g.create_time," +
                "       COUNT(gm_all.id) AS member_count " +
                "FROM `groups` AS g " +
                "INNER JOIN group_members AS gm_day ON g.group_id = gm_day.group_id AND gm_day.username = ? " +
                "INNER JOIN group_members AS gm_all ON g.group_id = gm_all.group_id " +
                "GROUP BY g.group_id;";
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
                group.setCreatorUsername(rs.getString("creator_username"));
                group.setMemberCount(rs.getInt("member_count"));
                group.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
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
        String query = "SELECT group_id,group_name, group_avatar_path,creator_username,member_count,create_time " +
                "FROM `groups` WHERE group_name = ?";
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
     * @param groupId  群组ID
     * @param username 用户名
     * @param role     角色（默认为member）
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
     * @param groupId  群组ID
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
        String query = "SELECT gm.id, gm.username, gm.join_time, gm.role, u.avatar_path " +
                "FROM group_members gm " +
                "LEFT JOIN user u ON gm.username = u.username " +
                "WHERE gm.group_id = ? " +
                "ORDER BY gm.join_time";
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

                // 创建User对象并设置用户信息
                User user = new User();
                user.setUserName(rs.getString("username"));
                String avatarPath = rs.getString("avatar_path");
                user.setAvatarPath(avatarPath == null ? Constant.DEFAULT_AVATAR : avatarPath);
                member.setUser(user);

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
     * @param groupId  群组ID
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
     * 保存群组消息
     *
     * @param message 消息对象
     * @param groupId 群组ID
     * @return 保存是否成功
     */
    public static long saveGroupMessage(Message message, int groupId) {
        String insert = "INSERT INTO group_messages(sender_name, group_id, content, time) VALUES(?,?,?,?)";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, message.getSender());
            statement.setInt(2, groupId);
            // 如果是JSON消息，保存JSON内容；否则保存普通文本内容
            if (message.isJsonMessage()) {
                statement.setString(3, message.getJson().toJSONString(0));
            } else {
                statement.setString(3, message.getContent());
            }
            statement.setTimestamp(4, new Timestamp(message.getTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
            boolean result = (statement.executeUpdate() > 0);
            if (!result) return -1L;
            ResultSet rs = statement.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1L;
    }

    /**
     * 搜索群组
     *
     * @param keyword 搜索关键词
     * @return 匹配的群组列表
     */
    public static List<Group> searchGroups(String keyword) {
        List<Group> groups = new ArrayList<>();
        String query = "SELECT g.group_id," +
                "       g.group_name," +
                "       g.group_avatar_path," +
                "       g.creator_username," +
                "       g.create_time," +
                "       COUNT(gm_all.id) AS member_count " +
                "FROM `groups` AS g " +
                "INNER JOIN group_members AS gm_all ON g.group_id = gm_all.group_id " +
                "where group_name LIKE ?" +
                "GROUP BY g.group_id;";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, keyword + "%");
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                Group group = new Group();
                group.setGroupId(rs.getInt("group_id"));
                group.setGroupName(rs.getString("group_name"));
                group.setGroupAvatarPath(rs.getString("group_avatar_path"));
                group.setCreatorUsername(rs.getString("creator_username"));
                group.setMemberCount(rs.getInt("member_count"));
                group.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
                groups.add(group);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return groups;
    }

    /**
     * 获取好友消息历史记录（分页）
     *
     * @param user1    用户1
     * @param user2    用户2
     * @param pageSize 每页大小
     * @param index    页码（从1开始）
     * @return 分页的好友消息历史
     */
    public static Page<ChatMessage> getPrivateMessageHistory(String user1, String user2, int index, int pageSize) {
        Page<ChatMessage> page = new Page<>(pageSize, index);
        List<ChatMessage> messages = new ArrayList<>();

        // 计算偏移量
        int offset = (index - 1) * pageSize;

        // 查询消息总数
        String countQuery = "SELECT COUNT(*) as total FROM message WHERE (sender=? AND receiver=?) OR (sender=? AND receiver=?)";
        long total = 0;
        PreparedStatement countStmt = null;
        try {
            countStmt = dataBase.prepareStatement(countQuery);
            countStmt.setString(1, user1);
            countStmt.setString(2, user2);
            countStmt.setString(3, user2);
            countStmt.setString(4, user1);
            ResultSet countRs = countStmt.executeQuery();
            if (countRs.next()) {
                total = countRs.getLong("total");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 查询分页消息数据（按时间正序）
        String query = "SELECT id, sender, receiver, content, sendtime FROM message WHERE (sender=? AND receiver=?) OR (sender=? AND receiver=?) ORDER BY sendtime desc LIMIT ? OFFSET ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, user1);
            statement.setString(2, user2);
            statement.setString(3, user2);
            statement.setString(4, user1);
            statement.setInt(5, pageSize);
            statement.setInt(6, offset);
            ResultSet rs = statement.executeQuery();

            // 直接按正序添加
            while (rs.next()) {
                ChatMessage message = new ChatMessage(
                        rs.getLong("id"),
                        rs.getString("sender"),
                        rs.getString("receiver"),
                        JSONUtil.parseObj(rs.getString("content")),
                        rs.getTimestamp("sendtime").toLocalDateTime()
                );
                messages.add(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (countStmt != null) {
                try {
                    countStmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return page.setTotal(total).setList(messages);
    }

    /**
     * 获取群组消息历史记录（分页）
     *
     * @param groupId  群组ID
     * @param pageSize 每页大小
     * @param index    页码（从1开始）
     * @return 分页的群组消息历史
     */
    public static Page<ChatMessage> getGroupMessageHistoryPage(int groupId, int index, int pageSize) {
        Page<ChatMessage> page = new Page<>(pageSize, index);
        List<ChatMessage> messages = new ArrayList<>();

        // 计算偏移量
        int offset = (index - 1) * pageSize;

        // 查询消息总数
        String countQuery = "SELECT COUNT(*) as total FROM group_messages WHERE group_id = ?";
        long total = 0;
        PreparedStatement countStmt = null;
        try {
            countStmt = dataBase.prepareStatement(countQuery);
            countStmt.setInt(1, groupId);
            ResultSet countRs = countStmt.executeQuery();
            if (countRs.next()) {
                total = countRs.getLong("total");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 查询分页消息数据（按时间正序）
        String query = "SELECT id, sender_name, content, `time` FROM group_messages WHERE group_id = ? ORDER BY `time` desc LIMIT ? OFFSET ?";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setInt(1, groupId);
            statement.setInt(2, pageSize);
            statement.setInt(3, offset);
            ResultSet rs = statement.executeQuery();

            // 直接按正序添加
            while (rs.next()) {
                ChatMessage message = new ChatMessage(
                        rs.getLong("id"),
                        rs.getString("sender_name"),
                        String.valueOf(groupId), // 群组ID作为receiver
                        JSONUtil.parseObj(rs.getString("content")),
                        rs.getTimestamp("time").toLocalDateTime()
                );
                messages.add(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (countStmt != null) {
                try {
                    countStmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        return page.setTotal(total).setList(messages);
    }

    /**
     * 更新群组成员角色
     *
     * @param groupId  群组ID
     * @param username 用户名
     * @param newRole  新角色
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

    /**
     * 获取群当中的群成员
     *
     * @param groupId  群id
     * @param username 用户名
     */
    public static Optional<GroupMember> getGroupMember(int groupId, String username) {
        String query = "SELECT id,role,join_time FROM group_members WHERE group_id = ?  AND username = ?";
        PreparedStatement statement;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setInt(1, groupId);
            statement.setString(2, username);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                GroupMember member = new GroupMember();
                member.setGroupId(groupId);
                member.setUsername(username);
                member.setId(rs.getLong("id"));
                member.setRole(rs.getString("role"));
                member.setJoinTime(rs.getTimestamp("join_time").toLocalDateTime());
                return Optional.of(member);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }
}
