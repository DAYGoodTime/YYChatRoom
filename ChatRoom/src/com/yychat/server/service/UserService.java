package com.yychat.server.service;

import cn.hutool.json.JSONObject;
import com.yychat.common.model.*;
import com.yychat.server.udp.YYChatUDPServer;
import com.yychat.server.util.DBUtil;
import com.yychat.server.view.StartServer;

import java.net.InetSocketAddress;
import java.util.Optional;

public class UserService {

    private final InetSocketAddress clientAddress;

    public UserService(InetSocketAddress clientAddress) {
        this.clientAddress = clientAddress;
    }

    public void handelUserLoginReq(Message message) {
        YYChatUDPServer udpServer = StartServer.getUDPServer();
        User user = message.getJson().toBean(User.class);
        Message response = Message.builder()
                .setMessageType(message.getMessageType())
                .setSender(SystemUser.Server.getStr())
                .setReceiver(message.getSender());
        JSONObject json = new JSONObject();
        boolean success = true;
        boolean loginSuccess = DBUtil.loginValidate(user.getUserName(), user.getPassword());
        if (!loginSuccess) {
            success = false;
            json.set("success", false);
            json.set("message", "账号或密码错误");
        }
        ServiceResponse<User> serviceResponse = queryUserInfoByUserName(user.getUserName());
        if (!serviceResponse.isSuccess() && success) {
            json.set("success", false);
            json.set("message", "登录失败:" + serviceResponse.getMessage());
        }
        json.set("success", true);
        json.set("data", serviceResponse.getData());
        response.setJsonMessage(json);
        udpServer.sendMessageToClient(clientAddress, response, message);
        //设置上线用户
        System.out.println("用户 " + user.getUserName() + "已上线");
        udpServer.getUserAddressMap().put(user.getUserName(), clientAddress);
    }

    public void handelUserSignupReq(Message message) {
        Message response = Message.builder()
                .setMessageType(message.getMessageType())
                .setSender(SystemUser.Server.getStr())
                .setReceiver(message.getSender());
        JSONObject json = new JSONObject();
        User user = message.getJson().toBean(User.class);
        boolean founded = DBUtil.hasUser(user.getUserName());
        boolean success = true;
        if (founded) {
            success = false;
            json.set("success", false);
            json.set("message", "注册失败 : 该用户名已被注册");
        }
        if (!DBUtil.addNewUser(user) && success) {
            json.set("success", false);
            json.set("message", "注册失败 : 服务器异常");
        }
        json.set("success", true);
        json.set("data", user);
        response.setJsonMessage(json);
        StartServer.getUDPServer().sendMessageToClient(clientAddress, response, message);
    }


    public ServiceResponse<User> queryUserInfoByUserName(String username) {
        ServiceResponse<User> serviceResponse = new ServiceResponse<>(new User());
        User userInfo = DBUtil.getUserInfo(username);
        if (userInfo == null) {
            return serviceResponse.error("用户不存在");
        }
        return serviceResponse.success(userInfo);
    }



}
