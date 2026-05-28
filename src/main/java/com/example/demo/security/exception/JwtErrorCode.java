package com.example.demo.security.exception;

import com.example.demo.exception.ErrorCode;

public enum JwtErrorCode implements ErrorCode {
    JWT_EXPIRED_TOKEN(401, "JWT_EXPIRED_TOKEN", "만료된 토큰입니다."),
    JWT_UNSUPPORTED_TOKEN(401, "JWT_UNSUPPORTED_TOKEN", "지원하지 않는 토큰입니다."),
    JWT_MALFORMED_TOKEN(401, "JWT_MALFORMED_TOKEN", "형식이 잘못된 토큰입니다."),
    JWT_INVALID_TOKEN(401, "JWT_INVALID_TOKEN", "유효하지 않은 토큰입니다.");

    private final int status;
    private final String code;
    private final String message;

    JwtErrorCode(int status, String code, String message) {
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
