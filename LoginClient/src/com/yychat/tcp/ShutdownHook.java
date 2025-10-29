package com.yychat.tcp;

import com.yychat.model.Message;
import com.yychat.model.MessageType;
import com.yychat.view.FriendList;

import java.io.ObjectOutputStream;

public class ShutdownHook extends Thread implements Runnable{

    @Override
    public void run() {
        System.out.println("正在处理未关闭的连接");
        try {
            ClientReceiverThread thread = ClientLogin.getClientReceiverThread();
            if(thread != null) {
                thread.interrupt();
            }
            if(YYchatClientConnection.getSocket() != null
                    && YYchatClientConnection.getSocket().isConnected()) {
                Message message = new Message();
                message.setMessageType(MessageType.EXIT);
                message.setSender(FriendList.getUserName());
                ObjectOutputStream out = new ObjectOutputStream(YYchatClientConnection.getSocket().getOutputStream());
                out.writeObject(message);
                //out.close();
                YYchatClientConnection.getSocket().close();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("连接清理完毕");
    }
}
