package com.yychat.server.tcp.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.yychat.common.model.*;
import com.yychat.server.service.AvatarFileManager;
import com.yychat.server.service.GroupService;

public class GroupRequestHandler {


    public static Message handelGroupSaveRequest(Message message) {
        String groupName = message.getJson().getStr("group_name", "default_group");
        String groupAvatarPath = null;
        if (message.getAttachmentType().equals(AttachmentType.GROUP_AVATAR)){
            //有头像文件，需要保存
            String fileName = message.getJson().getStr("avatar_file_name");
            if(StrUtil.isBlank(fileName)){
                return logError("群组头像地址为空",message);
            }
            groupAvatarPath = AvatarFileManager.saveGroupAvatarFromBytes(groupName,message.getAttachment(byte[].class),fileName);
            if(StrUtil.isBlank(groupAvatarPath)){
                return logError("群组头像保存失败",message);
            }
        }
        boolean update = message.getJson().getBool("update",false);
        Group group;
        if(update){
            //更新群组头像
            int groupId = message.getJson().getInt("group_id",-1);
            String requestUsername = message.getJson().getStr("username");
            ServiceResponse<Group> response = GroupService.getInstance().updateGroupInfo(groupId, groupName, groupAvatarPath, requestUsername);
            if(!response.isSuccess()){
                return logError("更新群信息失败",message);
            }
            group = response.getData();
        }else {
            //创建新群组
            ServiceResponse<Group> response = GroupService.getInstance().createGroup(
                    groupName,
                    message.getJson().getStr("creator_username",""),
                    groupAvatarPath
            );
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


    private static Message logError(String message, Message request) {
        Message response = Message.builder()
                .setMessageType(request.getMessageType())
                .setSender(request.getSender())
                .setJsonMessage(new JSONObject().set("success", false).set("message", message));
        System.out.println(message);
        return response;
    }
}
