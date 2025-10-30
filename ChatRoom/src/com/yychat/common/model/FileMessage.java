package com.yychat.common.model;

public class FileMessage {

    private String transferId;        // 传输唯一标识
    private String fileName;          // 文件名
    private long fileSize;           // 文件大小
    private String fileType;         // 文件类型
    private byte[] fileContent;

    public FileMessage(String fileName, String fileType, byte[] fileContent) {
        this.transferId = String.valueOf(System.currentTimeMillis());
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileContent = fileContent;
        this.fileSize = fileContent.length;
    }

    public String getTransferId() {
        return transferId;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public byte[] getFileContent() {
        return fileContent;
    }
}
