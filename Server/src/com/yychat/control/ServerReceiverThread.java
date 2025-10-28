package com.yychat.control;

import com.yychat.model.Message;
import com.yychat.model.MessageType;
import com.yychat.model.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Iterator;
import java.util.Set;

public class ServerReceiverThread implements Runnable {
    private Socket socket = null;
    public ServerReceiverThread(Socket socket) {
        this.socket = socket;
    }
    public void run() {
        while(!Thread.currentThread().isInterrupted()){
            try {
                if(!socket.isClosed()){
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    Message message = (Message) in.readObject();
                    switch (message.getMessageType()) {
                        case MessageType.COMMON_CHAT_MESSAGE:
                            System.out.println(message.getSender()
                                    + "对" + message.getReceiver()
                                    + "说：" + message.getContent());
                            message.setTime(new java.util.Date());
                            DBUtil.insertChatMessage(message.getSender(),message.getReceiver(),message.getContent(),message.getTime());
                            Socket receiverSocket = YYchatServer.getUserSocket(message.getReceiver());
                            System.out.println("接收方" + message.getReceiver() + "的socket对象" + receiverSocket);
                            if (receiverSocket != null) {
                                sendMessage(receiverSocket, message);
                            } else {
                                System.out.println(message.getReceiver() + "不在线上");
                            }
                            break;
                        case MessageType.EXIT:
                            System.out.println(message.getSender() + "socket关闭");
                            socket.close();
                            YYchatServer.getUserSocketMap().remove(message.getSender());
                            break;
                        case MessageType.REQUEST_ONLINE_FRIENDS:
                            Set<String> onlineFriendSet = YYchatServer.getUserSocketMap().keySet();
                            Iterator<String> it = onlineFriendSet.iterator();
                            StringBuilder onlineFriendBuilder = new StringBuilder();
                            while (it.hasNext()) {
                                onlineFriendBuilder.append(it.next());
                                if (it.hasNext()) {
                                    onlineFriendBuilder.append(",");
                                }
                            }
                            String onlineFriends = onlineFriendBuilder.toString();
                            message.setReceiver(message.getSender());
                            message.setSender("Server");
                            message.setMessageType(MessageType.RESPONSE_ONLINE_FRIENDS);
                            message.setContent(onlineFriends);
                            sendMessage(socket, message);
                            break;
                        case MessageType.NEW_ONLINE_FRIEND:
                            message.setMessageType(MessageType.NEW_ONLINE_TO_ALL_FRIENDS);
                            Set<String> onlineFriendSet2 = YYchatServer.getUserSocketMap().keySet();
                            Iterator<String> it2 = onlineFriendSet2.iterator();
                            while (it2.hasNext()) {
                                String friendName = it2.next();
                                message.setReceiver(friendName);
                                Socket friendSocket = YYchatServer.getUserSocket(friendName);
                                sendMessage(friendSocket, message);
                            }
                        break;
                        case MessageType.REQUEST_FRIEND_LIST:
                            message.setContent(DBUtil.getAllFriends(message.getSender(),1));
                            message.setMessageType(MessageType.RESPONSE_FRIEND_LIST);
                            message.setReceiver(message.getSender());
                            message.setSender("Server");
                            sendMessage(socket, message);
                            break;
                        case MessageType.USER_ADD_NEW_FRIEND:
                            String sender = message.getSender();
                            System.out.println("<UNK>" + sender + "<UNK>");
                            //是否拥有这名用户
                            if(DBUtil.hasUser(message.getContent())){
                                //这名用户是否已经是请求用户的朋友
                                System.out.println("<UNK>");
                                if(DBUtil.isUsersFriend(sender,message.getContent(),1)){
                                    message.setMessageType(MessageType.USER_ADD_NEW_FRIEND_FAILURE_ALREADY_FRIEND);
                                }
                                else{
                                    //添加好友关系
                                    DBUtil.insertIntoFriend(sender,message.getContent(),1);
                                    message.setMessageType(MessageType.USER_ADD_NEW_FRIEND_SUCCESS);
                                    System.out.println("success");
                                }
                            }
                            else{

                                message.setMessageType(MessageType.USER_ADD_NEW_FRIEND_FAILURE_NO_USER);
                            }
                            message.setReceiver(sender);
                            message.setSender("Server");
                            System.out.println(message.getMessageType());
                            sendMessage(socket, message);
                            break;
                        case MessageType.IS_FRIEND_ONLINE:
                            Set<String> onlineFriendSet3 = YYchatServer.getUserSocketMap().keySet();
                            if(onlineFriendSet3.contains(message.getContent())){
                                message.setMessageType(MessageType.IS_FRIEND_ONLINE_SUCCESS);
                            }
                            else{
                                message.setMessageType(MessageType.IS_FRIEND_ONLINE_FAILURE);
                            }
                            message.setReceiver(message.getSender());
                            message.setSender("Server");
                            sendMessage(socket, message);
                        break;
                    }
                }
                else{
                    Thread.currentThread().interrupt();
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void sendMessage(Socket socket,Message message){
        ObjectOutputStream out = null;
        try{
            out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(message);
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
