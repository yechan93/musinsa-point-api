package com.musinsa.point.service;

import com.musinsa.point.config.PointPolicyConfig;
import com.musinsa.point.domain.CreditStatus;
import com.musinsa.point.domain.PointCredit;
import com.musinsa.point.domain.PointDebit;
import com.musinsa.point.domain.PointDebitDetail;
import com.musinsa.point.exception.ErrorCode;
import com.musinsa.point.exception.PointException;
import com.musinsa.point.repository.PointCreditRepository;
import com.musinsa.point.repository.PointDebitDetailRepository;
import com.musinsa.point.repository.PointDebitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 포인트 사용 / 사용취소 서비스
 */
@Service
@RequiredArgsConstructor
public class PointDebitService {

    private final PointDebitRepository pointDebitRepository;
    private final PointDebitDetailRepository pointDebitDetailRepository;
    private final PointCreditRepository pointCreditRepository;
    private final PointPolicyConfig policyConfig;

    /**
     * 포인트 사용
     * 수기지급 우선 → 만료일 빠른 순 → ID 오름차순으로 차감
     *
     * @param userId  사용 유저
     * @param orderNo 주문번호 (중복 불가)
     * @param amount  사용 금액
     * @return 생성된 debitKey
     */
    @Transactional
    public String debit(String userId, String orderNo, Long amount) {

        // 사용 금액 검증
        validatePositiveAmount(amount, ErrorCode.INVALID_DEBIT_AMOUNT);

        // 동일 주문번호 중복 사용 방지
        if (pointDebitRepository.existsByOrderNo(orderNo)) {
            throw new PointException(ErrorCode.DUPLICATE_ORDER_NO);
        }

        // 잔액 부족 확인
        Long balance = pointCreditRepository
                .sumRemainAmountByUserIdAndStatus(userId, CreditStatus.ACTIVE);
        if (balance < amount) {
            throw new PointException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 사용건 생성
        PointDebit debit = PointDebit.create(userId, orderNo, amount);
        pointDebitRepository.save(debit);

        // 차감 대상 적립건 목록 조회 (수기지급 우선 → 만료 빠른 순)
        List<PointCredit> targets = pointCreditRepository.findDebitTargets(userId, CreditStatus.ACTIVE);

        // 각 적립건에서 순서대로 차감
        long remaining = amount;
        for (PointCredit credit : targets) {
            if (remaining <= 0) break;

            // 이 적립건에서 차감할 금액 : 적립 잔액과 남은 차감액 중 작은 값
            long deductAmount = Math.min(credit.getRemainAmount(), remaining);

            credit.deduct(deductAmount);

            // 이 적립건에서 얼마 사용했는지 상세 기록
            PointDebitDetail detail = PointDebitDetail.create(debit, credit, deductAmount);
            pointDebitDetailRepository.save(detail);

            remaining -= deductAmount;
        }

        return debit.getDebitKey();
    }

    /**
     * 사용 취소 (전체 또는 부분)
     * 만료된 적립건에서 복구할 금액은 신규 적립으로 처리
     *
     * @param debitKey     취소 대상 사용건 key
     * @param cancelAmount 취소 금액
     */
    @Transactional
    public void cancelDebit(String debitKey, Long cancelAmount) {

        validatePositiveAmount(cancelAmount, ErrorCode.INVALID_CANCEL_AMOUNT);

        PointDebit debit = pointDebitRepository.findByDebitKey(debitKey)
                .orElseThrow(() -> new PointException(ErrorCode.DEBIT_NOT_FOUND));

        // 취소 가능 금액 초과 여부 확인 (이미 취소된 금액 제외)
        if (cancelAmount > debit.cancellableAmount()) {
            throw new PointException(ErrorCode.EXCEED_CANCELLABLE_AMOUNT);
        }

        // 사용 상세 조회 — JOIN FETCH 로 pointCredit 함께 로딩 (N+1 방지)
        List<PointDebitDetail> details =
                pointDebitDetailRepository.findWithCreditByPointDebitId(debit.getId());

        // 각 상세에서 순서대로 취소 금액 분배
        long remaining = cancelAmount;
        for (PointDebitDetail detail : details) {
            if (remaining <= 0) break;

            // 이 상세에서 추가로 취소 가능한 금액
            long restoreAmount = Math.min(detail.cancellableAmount(), remaining);
            if (restoreAmount <= 0) continue;

            PointCredit credit = detail.getPointCredit();

            if (credit.isExpired()) {
                // 만료된 적립건 → 복구 대신 신규 적립 처리
                // 복구 개념의 신규 적립은 최대 포인트 보유 한도 체크 예외로 가정함
                LocalDate newExpiredAt = LocalDate.now().plusDays(policyConfig.getDefaultExpireDays());
                PointCredit newCredit = PointCredit.create(
                        debit.getUserId(), restoreAmount, credit.isManual(), newExpiredAt);
                pointCreditRepository.save(newCredit);
            } else {
                // 미만료 적립건 → 잔액 복구
                credit.restore(restoreAmount);
            }

            // 이 상세에서 취소한 금액 누적 기록
            detail.cancel(restoreAmount);

            remaining -= restoreAmount;
        }

        // 사용건 전체 취소 금액 누적 및 status 갱신
        debit.addCancelledAmount(cancelAmount);
    }

    // ────────────────────────────────────────────
    // private 검증 메서드
    // ────────────────────────────────────────────

    // 금액이 1원 이상인지 확인 — 사용, 취소 둘 다 재활용
    private void validatePositiveAmount(Long amount, ErrorCode errorCode) {
        if (amount < 1) {
            throw new PointException(errorCode);
        }
    }
}