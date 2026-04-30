package com.musinsa.point.dto.response;

/**
 * 포인트 사용 응답 DTO
 * 생성된 debitKey 를 클라이언트에 반환
 */
public record DebitResponse(
        // 사용 식별 키 — 사용취소 요청 시 이 값을 사용
        String debitKey
) {
    public static DebitResponse of(String debitKey) {
        return new DebitResponse(debitKey);
    }
}