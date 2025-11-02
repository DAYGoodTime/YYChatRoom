package com.yychat.common.model;

public enum AttachmentType {
    IMAGE_AVATAR(0,"image_avatar"),
    MESSAGE_FILE(1,"message_file"),
    GROUP_AVATAR(2,"group_avatar"),;
    private final int code;
    private final String type;
    AttachmentType(int code, String type) {
        this.code = code;
        this.type = type;
    }
    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return type;
    }
}
