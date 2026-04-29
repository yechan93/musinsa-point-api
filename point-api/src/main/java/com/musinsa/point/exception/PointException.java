package com.musinsa.point.exception;

import lombok.Getter;

/**
 * 포인트 도메인에서 발생하는 비즈니스 예외
 * ErrorCode 를 필수로 받아 메시지와 HTTP 상태코드를 함께 전달
 */
@Getter
public class PointException extends RuntimeException {

    private final ErrorCode errorCode;

    public PointException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}