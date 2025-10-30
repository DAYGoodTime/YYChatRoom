package com.yychat.model;

public enum AttachmentType {
    IMAGE_AVATAR(0,"image_avatar"),;
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
