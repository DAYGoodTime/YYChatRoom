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

    //添加新用户
    public static int addNewUser(User user){
        int result = -1;
        String insert = "insert into user(username,password) values(?,?)";
        PreparedStatement statement;
        try{
            statement = dataBase.prepareStatement(insert);
            statement.setString(1, user.getUserName());
            statement.setString(2, user.getPassword());
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
        String query="insert into message(from_user,to_user,content,sendtime) values(?,?,?,?)";
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

    public static void main(String[] args) {
        //TESTING DRIVER
        boolean b = loginValidate("day", "kel123");
        if(b){
            System.out.println("Success");
        }else
            System.out.println("Fail");
    }
}
