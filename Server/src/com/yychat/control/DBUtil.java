package com.yychat.control;

import com.yychat.model.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    public static boolean loginValidate(String userName,String password){
        boolean loginSuccess = false;
        String query = "select * from user where username=? and password=?";
        PreparedStatement statement = null;
        try{
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            statement.setString(2, password);
            ResultSet rs = statement.executeQuery();
            loginSuccess = rs.next();
        }catch (Exception e){
            e.printStackTrace();
        }
        return loginSuccess;
    }

    public static boolean hasUser(String user){
        boolean hasUser = false;
        String query = "select * from user where username=?";
        PreparedStatement statement = null;
        try{
            statement = dataBase.prepareStatement(query);
            statement.setString(1, user);
            ResultSet rs = statement.executeQuery();
            hasUser = rs.next();
        }catch (Exception e){
            e.printStackTrace();
        }
        return hasUser;
    }

    //添加新用户（支持头像路径）
    public static int addNewUser(User user){
        int result = -1;
        String insert = "insert into user(username,password,avatar_path) values(?,?,?)";
        PreparedStatement statement;
        try{
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
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public static List<String> getAllFriends(String userName,int friendType){
        List<String> friendList = new ArrayList<>();
        String query = "select slaveUser from userRelation where masterUser=? and relation=?";
        PreparedStatement statement = null;
        try{
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            statement.setInt(2, friendType);
            ResultSet rs = statement.executeQuery();
            while(rs.next()){
                friendList.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return friendList;
    }

    public static List<String> getUnknowUsers(String userName){
        List<String> result = new ArrayList<>();
        String query = "select u.username from user as u " +
                "left join userrelation as ur on u.username = ur.slaveuser and ur.masteruser =? " +
                "where ur.masteruser is null and u.username !=?";
        PreparedStatement statement = null;
        try{
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            statement.setString(2, userName);
            ResultSet rs = statement.executeQuery();
            while(rs.next()){
                result.add(rs.getString(1));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean isUsersFriend(String userName,String userFriend,int friendType){
        boolean result = false;
        String query = "select * from userRelation where masterUser=? and slaveUser=? and relation=?";
        PreparedStatement statement = null;
        try{
            statement = dataBase.prepareStatement(query);
            statement.setString(1, userName);
            statement.setString(2, userFriend);
            statement.setInt(3, friendType);
            ResultSet rs = statement.executeQuery();
            result = rs.next();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static int insertIntoFriend(String user,String friendOfUser,int friendType){
        int count = 0;
        String query = "insert into userRelation(masterUser,slaveUser,relation) values(?,?,?)";
        PreparedStatement statement = null;
        try {
            statement = dataBase.prepareStatement(query);
            statement.setString(1, user);
            statement.setString(2, friendOfUser);
            statement.setInt(3, friendType);
            count = statement.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public static boolean insertChatMessage(String from, String to, String content, LocalDateTime time){
        boolean result = false;
        String query="insert into message(sender,receiver,content,sendtime) values(?,?,?,?)";
        PreparedStatement statement=null;
        try{
            statement = dataBase.prepareStatement(query);
            statement.setString(1, from);
            statement.setString(2, to);
            statement.setString(3, content);
            statement.setTimestamp(4,new java.sql.Timestamp(time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
            result = (statement.executeUpdate() > 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 更新用户头像路径
     * @param userName 用户名
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

    public static void main(String[] args) {
        //TESTING DRIVER
        boolean b = connectDB();
        if(b){
            System.out.println("Success");
        }else
            System.out.println("Fail");

        System.out.println(getUnknowUsers("day"));
    }
}
