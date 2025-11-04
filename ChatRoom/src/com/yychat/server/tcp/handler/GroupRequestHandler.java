package com.yychat.server.tcp.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.yychat.common.model.*;
import com.yychat.server.service.AvatarFileManager;
import com.yychat.server.service.GroupService;
import com.yychat.server.util.DBUtil;

public class GroupRequestHandler {


    public static Message handelGroupSaveRequest(Message message) {
        Group requestGroup = message.getJson().getBean("group_info",Group.class);
        String groupAvatarPath = null;
        if (AttachmentType.GROUP_AVATAR.equals(message.getAttachmentType())){
            //有头像文件，需要保存
            String fileName = message.getJson().getStr("avatar_file_name");
            if(StrUtil.isBlank(fileName)){
                return logError("群组头像地址为空",message);
            }
            groupAvatarPath = AvatarFileManager.saveGroupAvatarFromBytes(requestGroup.getGroupName(),message.getAttachment(byte[].class),fileName);
            if(StrUtil.isBlank(groupAvatarPath)){
                return logError("群组头像保存失败",message);
            }
            requestGroup.setGroupAvatarPath(groupAvatarPath);
        }
        boolean update = message.getJson().getBool("update",false);
        Group group;
        if(update){
            //更新群组
            String requestUserName = message.getJson().getStr("request_user");
            ServiceResponse<Group> response = GroupService.getInstance().updateGroupInfo(requestGroup,requestUserName);
            if(!response.isSuccess()){
                return logError("更新群信息失败",message);
            }
            group = response.getData();
        }else {
            //创建新群组
            ServiceResponse<Group> response = GroupService.getInstance().createGroup(requestGroup);
            if(!response.isSuccess()){
                return logError("群组创建失败 :" +response.getMessage(),message);
            }
            group = response.getData();
        }
        Message response = Message.builder().setMessageType(message.getMessageType())
                .setSender(SystemUser.Server.getStr())
                .setReceiver(message.getSender());
        JSONObject json = new JSONObject().set("success", true);
        json.set("data", group);
        response.setJsonMessage(json);
        return response;
    }

    public static Message handelGroupMessageHistoryRequest(Message request) {
        if (!request.isJsonMessage()) {
            return logError("消息格式错误", request);
        }
        int groupId = request.getJson().getInt("group_id", -1);
        int index = request.getJson().getInt("index", 0);
        int pageSize = request.getJson().getInt("page_size", 20);
        Page<ChatMessage> page = DBUtil.getGroupMessageHistoryPage(groupId, index, pageSize);
        return Message.builder()
                .setMessageType(request.getMessageType())
                .setSender(SystemUser.Server.getStr())
                .setReceiver(request.getSender())
                .setJsonMessage(new JSONObject().set("page",page).set("success", true));
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
