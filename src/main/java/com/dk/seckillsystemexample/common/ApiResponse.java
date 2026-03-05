package com.dk.seckillsystemexample.common;

public class ApiResponse<T> {
    public int code;
    public String message;
    public T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 0;
        r.message = "OK";
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> fail(int code, String msg) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.message = msg;
        r.data = null;
        return r;
    }
}