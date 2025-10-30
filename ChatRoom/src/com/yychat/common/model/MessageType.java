package com.yychat.common.model;

public interface MessageType {
    String EXIT = "-1";                     //退出
    String USER_LOGIN_REQUEST = "1";        //登录请求
    String USER_SIGNUP_REQUEST = "2";       //注册新用户请求
    String COMMON_CHAT_MESSAGE = "3";       //聊天消息
    String REQUEST_ONLINE_FRIENDS = "4";    //请求在线好友
    String NEW_ONLINE_FRIEND = "5";         //通知服务器新好友上线
    String NEW_OFFLINE_FRIEND = "6";        //通知服务器新好友上线
    String REQUEST_FRIEND_LIST = "7";       //请求当前用户的好友列表
    String USER_ADD_NEW_FRIEND = "8";       //添加好友请求
    String REQUEST_UNK_USER_LIST = "9";     //请求陌生用户列表
    String REQUEST_AVATAR_PATH = "10";      //请求用户头像路径
    String REQUEST_USER_INFO = "11";        //请求好友信息

    // TCP传输消息类型
    String TCP_ACK = "100";// TCP处理确认
    String TCP_FILE_UPLOAD = "101"; //TCP文件上传
    String TCP_FILE_DOWNLOAD = "102";


}