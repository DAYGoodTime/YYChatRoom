package com.yychat.control;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.yychat.api.MessageThread;
import com.yychat.model.Message;
import com.yychat.model.MessageType;
import com.yychat.view.ClientMain;
import com.yychat.view.FriendChat;
import com.yychat.view.FriendList;

import javax.swing.*;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.List;

/**
 * UDP版本的客户端接收线程，处理来自服务器的UDP消息
 */
public class ClientReceiverThreadUDP extends MessageThread {
    private DatagramSocket datagramSocket;
    private volatile boolean isRunning = true;

    public ClientReceiverThreadUDP(DatagramSocket socket) {
        this.datagramSocket = socket;
    }

    @Override
    public void run() {
        while (!this.isInterrupted() && isRunning) {
            try {
                if (datagramSocket != null && !datagramSocket.isClosed()) {
                    // 创建接收缓冲区
                    byte[] buffer = new byte[1024 * 64]; // 64KB缓冲区
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    // 接收UDP数据包
                    datagramSocket.receive(packet);

                    // 处理接收到的消息
                    processReceivedMessage(packet);
                } else {
                    break;
                }
            } catch (SocketException se) {
                if (this.isInterrupted() && datagramSocket != null && datagramSocket.isClosed()) {
                    System.out.println("UDP客户端socket已关闭，停止接收线程");
                    break;
                } else {
                    System.out.println("UDP接收线程Socket异常: " + se.getMessage());
                    se.printStackTrace();
                }
            } catch (Exception e) {
                if (!this.isInterrupted()) {
                    e.printStackTrace();
                }
                break;
            }
        }

        System.out.println("UDP客户端接收线程已结束");
    }

    private void processReceivedMessage(DatagramPacket packet) {
        try {
            // 反序列化消息
            byte[] data = packet.getData();
            ByteArrayInputStream bis = new ByteArrayInputStream(data, 0, packet.getLength());
            ObjectInputStream ois = new ObjectInputStream(bis);

            Message message = (Message) ois.readObject();

            System.out.println("客户端接收消息: " + JSONUtil.toJsonStr(message));

            switch (message.getMessageType()) {
                case MessageType.COMMON_CHAT_MESSAGE:
                    handleChatMessage(message);
                    break;

                case MessageType.RESPONSE_ONLINE_FRIENDS:
                    handleResponseOnlineFriends(message);
                    break;

                case MessageType.NEW_ONLINE_TO_ALL_FRIENDS:
                    handleNewOnlineFriend(message);
                    break;

                case MessageType.USER_ADD_NEW_FRIEND_SUCCESS:
                    handleAddNewFriendSuccess(message);
                    break;

                case MessageType.USER_ADD_NEW_FRIEND_FAILURE_ALREADY_FRIEND:
                    handleAddNewFriendFailureAlreadyFriend(message);
                    break;

                case MessageType.USER_ADD_NEW_FRIEND_FAILURE_NO_USER:
                    handleAddNewFriendFailureNoUser(message);
                    break;

                case MessageType.RESPONSE_FRIEND_LIST:
                    handleResponseFriendList(message);
                    break;

                case MessageType.IS_FRIEND_ONLINE_SUCCESS:
                    handleIsFriendOnlineSuccess(message);
                    break;

                case MessageType.IS_FRIEND_ONLINE_FAILURE:
                    handleIsFriendOnlineFailure(message);
                    break;
                case MessageType.REQUEST_USER_LIST:
                    handelRequestUnknownFriends(message);
                    break;

                case MessageType.RESPONSE_AVATAR:
                    handleResponseAvatar(message);
                    break;

                case MessageType.RESPONSE_AVATAR_DOWNLOAD:
                    handleResponseAvatarDownload(message);
                    break;

                case MessageType.UPDATE_AVATAR:
                    handleUpdateAvatar(message);
                    break;

                case MessageType.AVATAR_UPLOAD_SUCCESS:
                    handleAvatarUploadSuccess(message);
                    break;

                default:
                    System.out.println("未处理的消息类型: " + message.getMessageType());
                    break;
            }

            ois.close();
            bis.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleChatMessage(Message message) {
        try {
            String receiver = message.getReceiver();
            String sender = message.getSender();
            String chatKey = receiver + "to" + sender;

            System.out.println("收到来自 " + sender + " 的消息: " + message.getContent());

            // 查找或创建聊天窗口
            FriendChat chat = FriendList.getFriendChat(chatKey);
            if (chat != null) {
                chat.append(message);
            } else {
                System.out.println("自动创建聊天窗口: " + chatKey);

                // 获取UDP连接对象
                YYchatClientConnectionUDP udpConnection =
                        (YYchatClientConnectionUDP) ClientMain.getClient().getConnection();

                // 在EDT线程中创建聊天窗口
                SwingUtilities.invokeLater(() -> {
                    try {
                        // 创建新的聊天窗口
                        FriendChat newChat = new FriendChat(receiver, sender, udpConnection);
                        // 将聊天窗口存储到FriendList的map中
                        FriendList.getFriendChatMap().put(chatKey, newChat);
                        // 显示消息
                        newChat.append(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleResponseOnlineFriends(Message message) {
        try {
            String receiver = message.getReceiver();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null && message.isJsonMessage()) {
                JSONArray list = message.getJson().getJSONArray("list");
                System.out.println("收到在线好友列表: " + list.toJSONString(0));
                if (!list.isEmpty()) {
                    friendList.activeOnlineFriendIcon(list.toList(String.class));
                }
            } else {
                System.out.println("未找到好友列表窗口");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleNewOnlineFriend(Message message) {
        try {
            String receiver = message.getReceiver();
            String sender = message.getSender();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null) {
                System.out.println("新好友上线通知: " + sender);
                friendList.activeNewOnlineFriendIcon(sender);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddNewFriendSuccess(Message message) {
        try {
            String receiver = message.getReceiver();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null) {
                System.out.println("添加好友成功");
                JOptionPane.showMessageDialog(null, "添加好友成功！");
                friendList.addNewFriend(message.getContent());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddNewFriendFailureAlreadyFriend(Message message) {
        try {
            String receiver = message.getReceiver();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null) {
                System.out.println("添加好友失败：已经是好友");
                JOptionPane.showMessageDialog(null, "添加失败！该用户已经是你的朋友！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleAddNewFriendFailureNoUser(Message message) {
        try {
            String receiver = message.getReceiver();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null) {
                System.out.println("添加好友失败：用户不存在");
                JOptionPane.showMessageDialog(null, "添加失败！没有该用户");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleResponseFriendList(Message message) {
        try {
            String receiver = message.getReceiver();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null && message.isJsonMessage()) {
                System.out.println("收到好友列表: " + message.getJson().getJSONArray("list").toJSONString(0));
                friendList.setFriendList(message.getJson().getJSONArray("list").toList(String.class));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleIsFriendOnlineSuccess(Message message) {
        try {
            String receiver = message.getReceiver();
            System.out.println("好友 " + message.getContent() + " 在线");
            // 可以在这里添加好友在线状态显示
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleIsFriendOnlineFailure(Message message) {
        try {
            String receiver = message.getReceiver();
            System.out.println("好友 " + message.getContent() + " 不在线");
            // 可以在这里添加好友离线状态显示
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handelRequestUnknownFriends(Message message) {
        try {
            String receiver = message.getReceiver();
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);

            if (friendList != null && message.isJsonMessage()) {
                JSONArray list = message.getJson().getJSONArray("list");
                System.out.println("收到陌生人列表: " + list.toJSONString(0));
                if (!list.isEmpty()) {
                    friendList.initStrangerPanel(list.toList(String.class),false);
                }
            } else {
                System.out.println("未找到陌生人窗口");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 处理头像响应
     */
    private void handleResponseAvatar(Message message) {
        try {
            String avatarPath = message.getJson().getStr("avatarPath","0.jpg");
            String requestUser = message.getJson().getStr("username");
            FriendList friendList = ClientMain.getClient().getFriendList().get(ClientMain.getCurrentUser().getUserName());
            System.out.println("收到头像响应: " + " -> " + avatarPath);

            // 在实际应用中，这里应该更新本地缓存或UI
            // 简化实现：打印响应信息
            if (friendList != null) {
                // 更新指定用户的头像
                friendList.updateFriendAvatar(requestUser, avatarPath);
            }

        } catch (Exception e) {
            System.err.println("处理头像响应失败: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 处理头像上传成功
     */
    private void handleAvatarUploadSuccess(Message message) {
        try {
            String avatarPath = message.getContent();
            String userName = message.getReceiver(); // 接收者是上传者本人
            FriendList friendList = ClientMain.getClient().getFriendList().get(ClientMain.getCurrentUser().getUserName());
            System.out.println("头像上传成功: " + userName + " -> " + avatarPath);

            // 显示成功提示
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        friendList,
                        "头像更新成功！",
                        "更新成功",
                        JOptionPane.INFORMATION_MESSAGE
                );

                // 清除头像缓存，强制重新加载
                if (friendList != null) {
                    friendList.clearAvatarCache();
                }
            });

        } catch (Exception e) {
            System.err.println("处理头像上传成功消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 处理头像下载响应
     */
    private void handleResponseAvatarDownload(Message message) {
        try {
            String receiver = message.getReceiver(); // 当前用户
            String sender = message.getSender(); // 好友用户名
            String content = message.getContent(); // 消息内容（头像路径或错误信息）
            byte[] avatarData = message.getFileData(); // 头像数据

            // 获取好友列表窗口
            FriendList friendList = ClientMain.getClient().getFriendList().get(receiver);
            if (friendList != null) {
                // 在EDT线程中处理文件保存和头像更新
                SwingUtilities.invokeLater(() -> {
                    try {
                        // 检查是否包含头像数据
                        if (avatarData != null && avatarData.length > 0) {
                            // 头像下载成功
                            String avatarPath = content.startsWith("头像文件不存在") ?
                                "avatars/" + sender + "_default.jpg" : content;

                            if (saveAvatarFile(avatarPath, avatarData)) {
                                // 通知FriendList头像下载完成
                                friendList.handleAvatarDownloaded(sender, avatarPath);
                                System.out.println("头像下载完成: " + sender + " -> " + avatarPath + " (" + avatarData.length + " bytes)");
                            } else {
                                System.err.println("头像文件保存失败: " + avatarPath);
                            }
                        } else {
                            // 头像下载失败
                            System.err.println("头像下载失败: " + content);
                            // 可以在这里显示错误提示给用户
                        }
                    } catch (Exception e) {
                        System.err.println("处理头像下载失败: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("处理头像下载响应失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 保存头像文件到本地
     */
    private boolean saveAvatarFile(String avatarPath, byte[] fileData) {
        try {
            if (fileData == null || fileData.length == 0) {
                System.err.println("头像文件数据为空");
                return false;
            }

            // 确保avatars目录存在
            File avatarsDir = new File("avatars");
            if (!avatarsDir.exists()) {
                avatarsDir.mkdirs();
            }

            // 创建文件对象
            File avatarFile = new File(avatarPath);
            File parentDir = avatarFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 保存文件
            try (FileOutputStream fos = new FileOutputStream(avatarFile)) {
                fos.write(fileData);
                fos.flush();
            }

            System.out.println("头像文件已保存: " + avatarPath + " (" + fileData.length + " bytes)");
            return true;

        } catch (IOException e) {
            System.err.println("保存头像文件失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 处理头像更新广播消息
     * 当有好友更新头像时，服务器会向所有在线好友发送此消息
     */
    private void handleUpdateAvatar(Message message) {
        try {
            String sender = message.getSender(); // 更新头像的用户
            String avatarPath = message.getContent(); // 头像路径

            // 获取当前登录的用户名
            String currentUserName = ClientMain.getCurrentUser().getUserName();
            if (currentUserName != null) {
                // 获取当前用户的好友列表窗口
                FriendList friendList = ClientMain.getClient().getFriendList().get(currentUserName);
                if (friendList != null) {
                    // 更新好友头像
                    friendList.updateFriendAvatar(sender, avatarPath);
                    System.out.println("好友 " + sender + " 更新头像为: " + avatarPath);
                } else {
                    System.err.println("未找到好友列表窗口，用户名: " + currentUserName);
                }
            } else {
                System.err.println("当前用户为空，无法处理头像更新");
            }
        } catch (Exception e) {
            System.err.println("处理头像更新消息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stopThread() {
        this.isRunning = false;
        this.interrupt();
    }
}