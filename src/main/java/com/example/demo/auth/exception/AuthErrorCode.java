package com.example.demo.auth.exception;

import com.example.demo.exception.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
    AUTH_PASSWORD_MISMATCH(400, "AUTH_PASSWORD_MISMATCH", "두 비밀번호가 일치하지 않습니다"),
    AUTH_INVALID_PASSWORD(401, "AUTH_INVALID_PASSWORD", "비밀번호가 일치하지 않습니다"),
    AUTH_INVALID_REFRESH_TOKEN(401, "AUTH_INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다."),
    AUTH_EXPIRED_REFRESH_TOKEN(401, "AUTH_EXPIRED_REFRESH_TOKEN", "만료된 Refresh Token"),
    AUTH_REFRESH_TOKEN_MISMATCH(401, "AUTH_REFRESH_TOKEN_MISMATCH", "Refresh Token이 일치하지 않습니다"),
    AUTH_LOGIN_REQUIRED(401, "AUTH_LOGIN_REQUIRED", "다시 로그인해주세요.");

    private final int status;
    private final String code;
    private final String message;

    AuthErrorCode(int status, String code, String message) {
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