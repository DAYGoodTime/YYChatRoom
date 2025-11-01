package com.yychat.client.tcp;

import cn.hutool.core.io.FileUtil;
import com.yychat.common.model.AttachmentType;
import com.yychat.common.model.Message;
import com.yychat.common.model.MessageType;
import com.yychat.common.model.SystemUser;

import java.io.File;
import java.util.Optional;

public class TCPExample {

    public static void main(String[] args) {
        TCPClient client = new TCPClient();
        client.connect();
        if (client.isConnected()) {
            File file = new File("D:\\void风花节4k.jpg");
            Message message = Message.builder()
                    .setMessageType(MessageType.TCP_ACK)
                    .setSender("DAY")
                    .setReceiver(SystemUser.Server.getStr())
                    .setAttachment(FileUtil.readBytes(file), byte[].class, AttachmentType.IMAGE_AVATAR);
            Optional<Message> response = client.sendMessage(message);
            if (response.isPresent()) {
                File newFile = new File("D:\\bg114514.jpg");
                FileUtil.touch(newFile);
                FileUtil.writeBytes(response.get().getAttachment(byte[].class), newFile);
            }
        }
    }
}
