package com.yychat.tcp;

import com.yychat.model.Message;
import com.yychat.model.MessageType;
import com.yychat.model.User;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class YYchatClientConnection {
    private static Socket socket = null;
    static private ClientReceiverThread thread = null;
    public YYchatClientConnection(){

        try {
            socket = new Socket("127.0.0.1", 5678);
            System.out.println("客户端连接成功" + getSocket());
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean loginValidate(User user){
        boolean loginSuccess = false;
        try{
            OutputStream os = getSocket().getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            Message message = new Message();
            message.setSender(user.getUserName());
            message.setReceiver("Server");
            message.setMessageType(MessageType.USER_LOGIN_REQUEST);
            oos.writeObject(message);
            oos.writeObject(user);
            ObjectInputStream ois = new ObjectInputStream(getSocket().getInputStream());
            message = (Message) ois.readObject();
            if(message.getMessageType().equals(MessageType.LOGIN_VALIDATE_SUCCESS)){
                loginSuccess = true;
            }
            else{
                socket.close();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return loginSuccess;
    }

    public boolean userSignup(User user){
        boolean result = false;
        try{
            OutputStream os = getSocket().getOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(os);
            Message singUpMessage = new Message();
            singUpMessage.setSender(user.getUserName());
            singUpMessage.setReceiver("Server");
            singUpMessage.setMessageType(MessageType.USER_SIGNUP_REQUEST);
            out.writeObject(singUpMessage);
            out.writeObject(user);
            ObjectInputStream in = new ObjectInputStream(getSocket().getInputStream());
            singUpMessage = (Message) in.readObject();
            if(singUpMessage.getMessageType().equals(MessageType.USER_SIGNUP_SUCCESS)){
                result = true;
            }
            socket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public static Socket getSocket() {
        return socket;
    }


}
