package com.yychat.model;

import cn.hutool.json.JSONObject;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

public class Message implements Serializable, MessageType {
    private static final long serialVersionUID = 1L;

    String MessageType;
    private String sender;
    private String receiver;
    private String content;
    private LocalDateTime time;
    private boolean isJsonMessage = false;
    private JSONObject json;
    private byte[] avatarData;       // 头像二进制数据
    private String avatarFileName;   // 头像文件名
    public String getMessageType() {
        return MessageType;
    }
    public void setMessageType(String messageType) {
        this.MessageType = messageType;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setJsonMessage(JSONObject json) {
        isJsonMessage = true;
        this.json = json;
    }

    public JSONObject getJson() {
        return json;
    }

    public boolean isJsonMessage() {
        return isJsonMessage;
    }

    // 头像相关getter和setter方法
    public byte[] getAvatarData() {
        return avatarData;
    }

    public void setAvatarData(byte[] avatarData) {
        this.avatarData = avatarData;
    }

    public String getAvatarFileName() {
        return avatarFileName;
    }

    public void setAvatarFileName(String avatarFileName) {
        this.avatarFileName = avatarFileName;
    }

    // ==================== TCP文件传输相关字段 ====================

    private String transferId;        // 传输唯一标识
    private String fileName;          // 文件名
    private long fileSize;           // 文件大小（字节）
    private String fileType;         // 文件类型（MIME类型）
    private String transferMethod;   // 传输方式（TCP/UDP）
    private int progressPercent;     // 传输进度（百分比）
    private String fileHash;         // 文件校验和（MD5/SHA256）
    private boolean isFileMessage;   // 是否为文件传输消息

    // ==================== TCP文件传输Getter和Setter方法 ====================

    /**
     * 获取传输唯一标识
     */
    public String getTransferId() {
        return transferId;
    }

    /**
     * 设置传输唯一标识
     */
    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    /**
     * 获取文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 设置文件名
     */
    public void setFileName(String fileName) {
        this.fileName = fileName;
        this.isFileMessage = true;
    }

    /**
     * 获取文件大小
     */
    public long getFileSize() {
        return fileSize;
    }

    /**
     * 设置文件大小
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * 获取文件类型
     */
    public String getFileType() {
        return fileType;
    }

    /**
     * 设置文件类型
     */
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    /**
     * 获取传输方式
     */
    public String getTransferMethod() {
        return transferMethod;
    }

    /**
     * 设置传输方式
     */
    public void setTransferMethod(String transferMethod) {
        this.transferMethod = transferMethod;
    }

    /**
     * 获取传输进度
     */
    public int getProgressPercent() {
        return progressPercent;
    }

    /**
     * 设置传输进度
     */
    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    /**
     * 获取文件校验和
     */
    public String getFileHash() {
        return fileHash;
    }

    /**
     * 设置文件校验和
     */
    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    /**
     * 判断是否为文件传输消息
     */
    public boolean isFileMessage() {
        return isFileMessage;
    }

    /**
     * 设置是否为文件传输消息
     */
    public void setFileMessage(boolean fileMessage) {
        isFileMessage = fileMessage;
    }

    // ==================== 便捷方法 ====================

    /**
     * 快速创建文件传输消息
     */
    public static Message createFileMessage(String sender, String receiver, String fileName,
                                          long fileSize, String fileType, String transferMethod) {
        Message message = new Message();
        message.setMessageType("FILE_TRANSFER");
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setFileName(fileName);
        message.setFileSize(fileSize);
        message.setFileType(fileType);
        message.setTransferMethod(transferMethod);
        message.setProgressPercent(0);
        message.setTime(java.time.LocalDateTime.now());
        return message;
    }

    /**
     * 创建TCP文件传输消息
     */
    public static Message createTCPFileMessage(String sender, String receiver, String fileName,
                                             long fileSize, String fileType) {
        return createFileMessage(sender, receiver, fileName, fileSize, fileType, "TCP");
    }

    /**
     * 创建UDP文件传输消息
     */
    public static Message createUDPFileMessage(String sender, String receiver, String fileName,
                                             long fileSize, String fileType) {
        return createFileMessage(sender, receiver, fileName, fileSize, fileType, "UDP");
    }

    /**
     * 获取文件大小格式化字符串
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", fileSize / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 验证是否为有效的文件传输消息
     */
    public boolean isValidFileMessage() {
        return isFileMessage &&
               fileName != null && !fileName.trim().isEmpty() &&
               fileSize > 0 &&
               transferMethod != null;
    }

    @Override
    public String toString() {
        if (isFileMessage) {
            return "FileMessage{" +
                    "transferId='" + transferId + '\'' +
                    ", fileName='" + fileName + '\'' +
                    ", fileSize=" + fileSize +
                    ", fileType='" + fileType + '\'' +
                    ", transferMethod='" + transferMethod + '\'' +
                    ", progressPercent=" + progressPercent +
                    ", sender='" + sender + '\'' +
                    ", receiver='" + receiver + '\'' +
                    '}';
        } else {
            return "Message{" +
                    "messageType='" + MessageType + '\'' +
                    ", sender='" + sender + '\'' +
                    ", receiver='" + receiver + '\'' +
                    ", content='" + content + '\'' +
                    ", time=" + time +
                    '}';
        }
    }
}
