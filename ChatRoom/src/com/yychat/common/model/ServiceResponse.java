package com.yychat.common.model;

public class ServiceResponse<T> {

    private boolean success = false;
    private String message;
    private T data;

    public ServiceResponse(String message) {
        this.message = message;
    }

    public ServiceResponse(T data) {
        this.success = true;
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public static ServiceResponse error(String message) {
        return new ServiceResponse(message);
    }

    public static <T> ServiceResponse<T> success(T data) {
        return new ServiceResponse<>(data);
    }


}
