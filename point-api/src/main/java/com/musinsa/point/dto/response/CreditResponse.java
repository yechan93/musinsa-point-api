package com.musinsa.point.dto.response;

/**
 * 포인트 적립 응답 DTO
 * 생성된 creditKey 를 클라이언트에 반환
 */
public record CreditResponse(
        // 적립 식별 키 — 적립취소 요청 시 이 값을 사용
        String creditKey
) {
    // 서비스에서 반환받은 creditKey 를 감싸서 응답 객체로 만드는 팩토리 메서드
    public static CreditResponse of(String creditKey) {
        return new CreditResponse(creditKey);
    }
}