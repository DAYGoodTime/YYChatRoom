package com.yychat.common.model;

public interface MessageType {
    String EXIT = "-1";                     //退出
    String USER_LOGIN_REQUEST = "1";        //登录请求
    String USER_SIGNUP_REQUEST = "2";       //注册新用户请求
    String COMMON_CHAT_MESSAGE = "3";       //聊天消息
    String REQUEST_ONLINE_FRIENDS = "4";    //请求在线好友
    String NEW_ONLINE_FRIEND = "5";         //通知服务器新好友上线
    String REQUEST_FRIEND_LIST = "7";       //请求当前用户的好友列表
    String USER_ADD_NEW_FRIEND = "8";       //添加好友请求
    String REQUEST_UNK_USER_LIST = "9";     //请求陌生用户列表
    String REQUEST_AVATAR_PATH = "10";      //请求用户头像路径
    String REQUEST_USER_INFO = "11";        //请求用户信息
    String USER_REMOVE_FRIEND = "12";        //删除该好友关系

    String GROUP_JOIN = "22";               // 加入群组
    String GROUP_USER_LEAVE = "24";         // 退出群组
    String GROUP_DELETE_REQUEST = "28";     // 删除群组请求
    String GROUP_DELETE_RESPONSE = "29";    // 删除群组响应

    // 群组成员管理消息类型
    String GROUP_ADD_MEMBER = "30";         // 添加成员
    String GROUP_ADD_MEMBER_RESPONSE = "31"; // 添加成员响应
    String GROUP_REMOVE_MEMBER = "32";      // 移除成员
    String GROUP_TRANSFER_OWNER = "34";     // 转让群主
    String GROUP_SET_ADMIN = "35";       // 设置管理员

    // 群组查询消息类型
    String GROUP_MEMBERS = "36";              // 获取群组成员
    String USER_GROUPS = "38";                 // 获取用户群组列表
    String GROUP_SEARCH = "40";       // 搜索群组
    // 群组聊天消息
    String GROUP_CHAT_MESSAGE = "41";       // 群组聊天消息

    // TCP传输消息类型
    String TCP_ACK = "100";// TCP处理确认
    String TCP_FILE_UPLOAD = "101"; //TCP文件上传
    String TCP_FILE_DOWNLOAD = "102";
    String TCP_HEARTBEAT = "103"; // TCP心跳消息
    String TCP_HEARTBEAT_ACK = "104"; // TCP心跳响应
    String TCP_GROUP_SAVE = "120"; // 保存群组请求(更新信息或创建群组)
    String TCP_USER_MESSAGE_REQUEST = "121"; //获取用户历史聊天记录
    String TCP_GROUP_MESSAGE_REQUEST = "122";//获取群组历史聊天记录
}