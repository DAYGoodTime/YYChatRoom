package com.yychat.client.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.yychat.client.ClientMain;
import com.yychat.client.udp.UDPClientConnection;
import com.yychat.common.model.Message;
import com.yychat.common.model.MessageType;
import com.yychat.common.model.ServiceResponse;
import com.yychat.common.model.User;

public class UserService {

    private static final UserService instance = new UserService();

    public static boolean userLogin = false;

    public static UserService getInstance() {
        return instance;
    }

    /**
     * 登录请求
     */
    public ServiceResponse<?> loginByUserName(String username, String password) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        if (StrUtil.isEmpty(username) || StrUtil.isEmpty(password)) {
            return serviceResponse.error("用户名或密码为空");
        }
        UDPClientConnection conn = ClientMain.getUDPConnection();
        User temp = new User(username, password);
        Message message = Message.builder()
                .setMessageType(Message.USER_LOGIN_REQUEST)
                .setSender(temp.getUserName())
                .setJsonMessage(new JSONObject(temp));
        Message response = conn.sendMessageToServerSync(message);
        if (response == null) return serviceResponse.error("服务器失联!");
        if (!MessageType.USER_LOGIN_REQUEST.equals(response.getMessageType()) || !response.getJson().getBool("success", false)) {
            return serviceResponse.error(response.getJson().getStr("message", ""));
        }
        User user = response.getJson().getBean("data", User.class);
        if (!response.isJsonMessage() || user == null)
            return serviceResponse.error("登录数据异常");
        ClientMain.setCurrentUser(user);
        userLogin = true;
        return serviceResponse.success("登录成功");
    }

    /**
     * 注册用户
     */
    public ServiceResponse<?> registerUser(String username, String password) {
        ServiceResponse<String> serviceResponse = new ServiceResponse<>("");
        if (StrUtil.isEmpty(username) || StrUtil.isEmpty(password)) {
            return serviceResponse.error("用户名或密码为空");
        }
        UDPClientConnection conn = ClientMain.getUDPConnection();
        User temp = new User(username, password);
        Message message = Message.builder()
                .setMessageType(Message.USER_SIGNUP_REQUEST)
                .setSender(temp.getUserName())
                .setJsonMessage(new JSONObject(temp));
        Message response = conn.sendMessageToServerSync(message);
        if (response == null) return serviceResponse.error("服务器失联!");
        if (!MessageType.USER_SIGNUP_REQUEST.equals(response.getMessageType()) || !response.getJson().getBool("success", false))
            return serviceResponse.error(response.getJson().getStr("message", ""));
        return serviceResponse.success("注册成功");
    }

    /**
     * 获取用户信息
     *
     * @param username 用户名
     */
    public ServiceResponse<User> queryUserInfoByUsername(String username) {
        ServiceResponse<User> serviceResponse = new ServiceResponse<>(new User());
        if (StrUtil.isEmpty(username)) {
            return serviceResponse.error("用户名为空");
        }
        Message response = ClientMain.getUDPConnection().sendMessageToServerSync(
                Message.builder()
                        .setMessageType(MessageType.REQUEST_USER_INFO)
                        .setSender(ClientMain.getCurrentUserName())
                        .setJsonMessage(new JSONObject().set("username", username)));
        if(response == null || !response.isJsonMessage()){
            return serviceResponse.error("服务端异常");
        }
        if (!response.getJson().getBool("success",false)) {
            return serviceResponse.error(response.getJson().getStr("message", ""));
        }
        return serviceResponse.success(response.getJson().getBean("data",User.class));
    }

    /**
     * 请求在线好友列表
     */
    public void requestFriends() {
        ClientMain.getUDPConnection().sendMessageToServer(
                Message.builder()
                        .setMessageType(Message.REQUEST_FRIEND_LIST)
                        .setSender(ClientMain.getCurrentUser().getUserName())
        );
    }

    /**
     * 请求在线好友列表
     */
    public void requestOnlineFriends() {
        ClientMain.getUDPConnection().sendMessageToServer(
                Message.builder()
                        .setMessageType(Message.REQUEST_ONLINE_FRIENDS)
                        .setSender(ClientMain.getCurrentUser().getUserName())
        );
    }

    /**
     * 请求陌生人列表
     */
    public void requestUnknownFriends() {
        ClientMain.getUDPConnection().sendMessageToServer(
                Message.builder()
                        .setMessageType(Message.REQUEST_UNK_USER_LIST)
                        .setSender(ClientMain.getCurrentUser().getUserName())
        );
    }

    /**
     * 广播好友已上线
     */
    public void broadcastNewFriendOnline() {
        ClientMain.getUDPConnection().sendMessageToServer(
                Message.builder()
                        .setSender(ClientMain.getCurrentUserName())
                        .setMessageType(Message.NEW_ONLINE_FRIEND)
        );
    }
}
