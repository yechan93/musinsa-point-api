package com.musinsa.point.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 사용취소 요청 DTO
 * POST /api/v1/points/debit/{debitKey}/cancel
 *
 * 전체 또는 일부 취소가 가능하므로 취소 금액을 명시적으로 받는다
 */
@Getter
@NoArgsConstructor
public class CancelDebitRequest {

    @NotNull(message = "취소 금액은 필수입니다.")
    private Long cancelAmount;
}