package com.musinsa.point.dto.response;

/**
 * 잔액 조회 응답 DTO
 * GET /api/v1/points/{userId}/balance
 *
 * 잔액은 ACTIVE 상태이며 만료되지 않은 적립건의 remainAmount 합산값
 */
public record BalanceResponse(
        String userId,

        // 현재 사용 가능 잔액 (만료 포인트 제외)
        Long balance
) {
    public static BalanceResponse of(String userId, Long balance) {
        return new BalanceResponse(userId, balance);
    }
}