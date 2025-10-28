package com.yychat.model;

public interface MessageType {
    String LOGIN_VALIDATE_SUCCESS = "1";    //登录成功
    String LOGIN_VALIDATE_FAILURE = "2";    //登录失败
    String COMMON_CHAT_MESSAGE = "3";       //聊天消息
    String EXIT = "-1";                     //退出
    String REQUEST_ONLINE_FRIENDS = "4";    //请求在线好友
    String RESPONSE_ONLINE_FRIENDS = "5";   //请求在线好友的回应
    String NEW_ONLINE_TO_ALL_FRIENDS = "6"; //服务器广播给所有在线好友
    String NEW_ONLINE_FRIEND = "7";         //通知服务器新好友上线
    String USER_SIGNUP_REQUEST = "8";       //注册新用户请求
    String USER_SIGNUP_SUCCESS = "9";       //注册成功
    String USER_SIGNUP_FAILURE = "10";      //注册失败
    String USER_LOGIN_REQUEST = "11";       //登录请求
    String REQUEST_FRIEND_LIST = "12";      //请求当前用户的好友列表
    String RESPONSE_FRIEND_LIST = "13";     //对请求好友列表的回应
    String USER_ADD_NEW_FRIEND = "14";
    String USER_ADD_NEW_FRIEND_SUCCESS = "15";
    String USER_ADD_NEW_FRIEND_FAILURE_NO_USER = "16";
    String USER_ADD_NEW_FRIEND_FAILURE_ALREADY_FRIEND = "17";
    String IS_FRIEND_ONLINE = "18";
    String IS_FRIEND_ONLINE_SUCCESS = "19";
    String IS_FRIEND_ONLINE_FAILURE = "20";
}