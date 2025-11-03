package com.yychat.server.tcp.handler;

import cn.hutool.json.JSONObject;
import com.yychat.common.model.ChatMessage;
import com.yychat.common.model.Message;
import com.yychat.common.model.Page;
import com.yychat.common.model.SystemUser;
import com.yychat.server.util.DBUtil;

public class UserRequestHandler {


    public static Message handelUserMessageHistoryRequest(Message request) {
        if (!request.isJsonMessage()) {
            return logError("消息格式错误", request);
        }
        String sender = request.getJson().getStr("sender", "");
        String receiver = request.getJson().getStr("receiver", "");
        int index = request.getJson().getInt("index", 0);
        int pageSize = request.getJson().getInt("page_size", 20);
        Page<ChatMessage> page = DBUtil.getPrivateMessageHistory(sender, receiver, index, pageSize);
        return Message.builder()
                .setMessageType(request.getMessageType())
                .setSender(SystemUser.Server.getStr())
                .setReceiver(request.getSender())
                .setJsonMessage(new JSONObject().set("page", page));
    }

    private static Message logError(String message, Message request) {
        Message response = Message.builder()
                .setMessageType(request.getMessageType())
                .setSender(request.getSender())
                .setJsonMessage(new JSONObject().set("success", false).set("message", message));
        System.out.println(message);
        return response;
    }
}
