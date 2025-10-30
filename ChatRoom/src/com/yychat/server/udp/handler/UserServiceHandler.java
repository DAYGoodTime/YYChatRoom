package com.yychat.server.udp.handler;

import cn.hutool.json.JSONObject;
import com.yychat.server.service.UserService;
import com.yychat.common.model.Message;
import com.yychat.common.model.ServiceResponse;
import com.yychat.common.model.SystemUser;
import com.yychat.common.model.User;
import com.yychat.server.view.StartServer;

import java.net.InetSocketAddress;

public class UserServiceHandler {

    private UserService userService;
    private InetSocketAddress clientAddress;

    public UserServiceHandler(UserService userService, InetSocketAddress clientAddress) {
        this.userService = userService;
        this.clientAddress = clientAddress;
    }

    public void handelReqUserInfo(Message message) {
        ServiceResponse<User> optionalUser = userService.queryUserInfoByUserName(message.getJson().getStr("username", ""));
        Message response = Message.builder()
                .setMessageType(message.getMessageType())
                .setSender(SystemUser.Server.getStr())
                .setReceiver(message.getSender());
        JSONObject obj = new JSONObject();
        if (!optionalUser.isSuccess()) {
            obj.set("success", false);
            obj.set("message", optionalUser.getMessage());
        } else {
            obj.set("success", true);
            obj.set("data", optionalUser.getData());
        }
        response.setJsonMessage(obj);
        StartServer.getUDPServer().sendMessageToClient(clientAddress, response, message);
    }
}
