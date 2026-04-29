package com.musinsa.point.service;

import com.musinsa.point.config.PointPolicyConfig;
import com.musinsa.point.domain.CreditStatus;
import com.musinsa.point.domain.PointCredit;
import com.musinsa.point.exception.ErrorCode;
import com.musinsa.point.exception.PointException;
import com.musinsa.point.repository.PointCreditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 포인트 적립 / 적립취소 / 잔액조회 서비스
 */
@Service
@RequiredArgsConstructor
public class PointCreditService {

    private final PointCreditRepository pointCreditRepository;
    private final PointPolicyConfig policyConfig;

    /**
     * 포인트 적립
     * @param userId    적립 대상 유저
     * @param amount    적립 금액
     * @param isManual  수기 지급 여부 (true 면 사용 시 우선 차감)
     * @param expiredAt 만료일 (null 이면 기본 정책값 적용)
     * @return 생성된 creditKey
     */
    @Transactional
    public String credit(String userId, Long amount, boolean isManual, LocalDate expiredAt) {

        // 만료일이 없으면 정책 기본값 적용
        LocalDate resolvedExpiredAt = (expiredAt != null) ? expiredAt
                : LocalDate.now().plusDays(policyConfig.getDefaultExpireDays());

        // 적립금액 유효성체크
        validateCreditAmount(amount);
        
        // 만료일 유효성 체크
        validateExpiredAt(resolvedExpiredAt);
        
        // 잔액 유효성 체크
        validateMaxBalance(userId, amount);

        PointCredit credit = PointCredit.create(userId, amount, isManual, resolvedExpiredAt);
        pointCreditRepository.save(credit);

        return credit.getCreditKey();
    }

    /**
     * 적립 취소
     * 사용된 금액이 1원이라도 있으면 취소 불가
     */
    @Transactional
    public void cancelCredit(String creditKey) {

        PointCredit credit = pointCreditRepository.findByCreditKey(creditKey)
                .orElseThrow(() -> new PointException(ErrorCode.CREDIT_NOT_FOUND));

        // 이미 취소된 적립인지 확인
        if (credit.getStatus() == CreditStatus.CANCELLED) {
            throw new PointException(ErrorCode.ALREADY_CANCELLED_CREDIT);
        }

        // 1원이라도 사용됐으면 취소 불가
        if (credit.getRemainAmount() < credit.getOriginalAmount()) {
            throw new PointException(ErrorCode.PARTIAL_USED_CREDIT);
        }

        // 통과하면 적립 취소
        credit.cancel();
    }

    /**
     * 잔액 조회
     * ACTIVE 상태이며 만료되지 않은 적립건의 잔액 합산
     */
    @Transactional(readOnly = true)
    public Long getBalance(String userId) {
        return pointCreditRepository.sumRemainAmountByUserIdAndStatus(userId, CreditStatus.ACTIVE);
    }

    // ────────────────────────────────────────────
    // private 검증 메서드
    // ────────────────────────────────────────────

    // 적립 금액 유효성 : 1원 이상, 1회 최대 한도 이하
    private void validateCreditAmount(Long amount) {
        if (amount < 1) {
            throw new PointException(ErrorCode.INVALID_CREDIT_AMOUNT);
        }
        if (amount > policyConfig.getMaxCreditPerOnce()) {
            throw new PointException(ErrorCode.EXCEED_MAX_CREDIT_PER_ONCE);
        }
    }

    // 만료일 유효성 : 최소 내일, 최대 5년 미만
    private void validateExpiredAt(LocalDate expiredAt) {
        LocalDate minDate = LocalDate.now().plusDays(1);
        LocalDate maxDate = LocalDate.now().plusYears(5);

        // expiredAt 이 내일보다 이전이거나, 5년 이후(포함)이면 예외
        if (expiredAt.isBefore(minDate) || !expiredAt.isBefore(maxDate)) {
            throw new PointException(ErrorCode.INVALID_EXPIRE_DATE);
        }
    }

    // 개인 최대 보유 잔액 초과 여부 확인
    private void validateMaxBalance(String userId, Long amount) {
        Long currentBalance = getBalance(userId);
        if (currentBalance + amount > policyConfig.getMaxBalancePerUser()) {
            throw new PointException(ErrorCode.EXCEED_MAX_BALANCE);
        }
    }
}