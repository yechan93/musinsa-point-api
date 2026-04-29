package com.musinsa.point.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 포인트 도메인 전용 에러 코드
 * 각 에러는 메시지와 HTTP 상태코드를 함께 보유
 */
@Getter
public enum ErrorCode {

    // 적립 관련
    INVALID_CREDIT_AMOUNT("적립 금액은 1원 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    EXCEED_MAX_CREDIT_PER_ONCE("1회 최대 적립 한도를 초과했습니다.", HttpStatus.BAD_REQUEST),
    EXCEED_MAX_BALANCE("개인 최대 보유 한도를 초과합니다.", HttpStatus.BAD_REQUEST),
    INVALID_EXPIRE_DATE("만료일은 최소 1일 이상, 최대 5년 미만이어야 합니다.", HttpStatus.BAD_REQUEST),
    CREDIT_NOT_FOUND("적립 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ALREADY_CANCELLED_CREDIT("이미 취소된 적립 내역입니다.", HttpStatus.BAD_REQUEST),
    PARTIAL_USED_CREDIT("일부라도 사용된 포인트는 적립 취소할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 사용 관련
    DUPLICATE_ORDER_NO("이미 포인트가 사용된 주문번호입니다.", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE("포인트 잔액이 부족합니다.", HttpStatus.BAD_REQUEST),
    INVALID_DEBIT_AMOUNT("사용 금액은 1원 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    DEBIT_NOT_FOUND("포인트 사용 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_CANCEL_AMOUNT("취소 금액은 1원 이상이어야 합니다.", HttpStatus.BAD_REQUEST),
    EXCEED_CANCELLABLE_AMOUNT("취소 가능 금액을 초과했습니다.", HttpStatus.BAD_REQUEST);

    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}