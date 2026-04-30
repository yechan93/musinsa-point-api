package com.musinsa.point.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 포인트 적립 요청 DTO
 * POST /api/v1/points/{userId}/credit
 */
@Getter
@NoArgsConstructor
public class CreditRequest {

    // @NotNull : JSON 바디에 amount 키가 없거나 null 이면 검증 실패 → 400
    @NotNull(message = "적립 금액은 필수입니다.")
    private Long amount;

    // boolean 기본형은 null 이 될 수 없어서 @NotNull 불필요
    private boolean manual;

    // null 이면 서비스에서 기본값(365일) 적용
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiredAt;
}