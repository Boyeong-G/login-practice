package com.example.demo.auth.exception;

import com.example.demo.exception.ErrorCode;

public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND(404, "USER_NOT_FOUND", "유저를 찾을 수 없습니다"),
    USER_EMAIL_ALREADY_EXISTS(409, "USER_EMAIL_ALREADY_EXISTS", "이미 존재하는 이메일입니다");

    private final int status;
    private final String code;
    private final String message;

    UserErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public int getStatus() { return status; }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
