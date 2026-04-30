package com.musinsa.point.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 사용 요청 DTO
 * POST /api/v1/points/{userId}/debit
 */
@Getter
@NoArgsConstructor
public class DebitRequest {

    // @NotBlank : null, 빈 문자열(""), 공백만 있는 문자열(" ") 모두 검증 실패 → 400
    @NotBlank(message = "주문번호는 필수입니다.")
    private String orderNo;

    @NotNull(message = "사용 금액은 필수입니다.")
    private Long amount;
}