package com.musinsa.point.service;

import com.musinsa.point.domain.CreditStatus;
import com.musinsa.point.domain.PointCredit;
import com.musinsa.point.exception.ErrorCode;
import com.musinsa.point.exception.PointException;
import com.musinsa.point.repository.PointCreditRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class PointCreditServiceTest {

    @Autowired PointCreditService pointCreditService;
    @Autowired PointCreditRepository pointCreditRepository;

    private static final String USER_ID = "testUser";
    private static final LocalDate VALID_EXPIRE = LocalDate.now().plusDays(30);

    // ────────── 적립 성공 케이스 ──────────
    @Test
    void 기본_적립_성공() {
        // given : 준비 없음 (아무 데이터도 없는 깨끗한 상태)

        // when : 1000원 적립
        String creditKey = pointCreditService.credit(USER_ID, 1000L, false, VALID_EXPIRE);

        // then : DB에 저장된 데이터를 꺼내서 검증
        PointCredit credit = pointCreditRepository.findByCreditKey(creditKey).orElseThrow();
        assertThat(credit.getRemainAmount()).isEqualTo(1000L);       // 잔액 1000원
        assertThat(credit.getOriginalAmount()).isEqualTo(1000L);     // 원래 금액도 1000원
        assertThat(credit.isManual()).isFalse();                     // 수기지급 아님
        assertThat(credit.getStatus()).isEqualTo(CreditStatus.ACTIVE); // 상태 ACTIVE
    }

    @Test
    void 만료일_생략하면_기본값_365일_적용() {
        // when : 만료일 없이 적립 (null 전달)
        String creditKey = pointCreditService.credit(USER_ID, 1000L, false, null);

        // then : 오늘 기준 365일 뒤 만료일로 저장됐는지 확인
        PointCredit credit = pointCreditRepository.findByCreditKey(creditKey).orElseThrow();
        assertThat(credit.getExpiredAt()).isEqualTo(LocalDate.now().plusDays(365));
    }

    // ────────── 적립 제약으로 인한 실패 케이스 ──────────
    @Test
    void 적립금액_1회_최대한도_초과시_예외() {
        // 10만원 초과 → 100,001원 시도
        assertThatThrownBy(() -> pointCreditService.credit(USER_ID, 100_001L, false, VALID_EXPIRE))
                .isInstanceOf(PointException.class)
                .satisfies(e -> assertThat(((PointException) e).getErrorCode())
                        .isEqualTo(ErrorCode.EXCEED_MAX_CREDIT_PER_ONCE));
    }

    @Test
    void 개인_최대보유한도_초과시_예외() {
        // max-balance-per-user: 500000 → 10만원 * 5회 = 50만원 꽉 채움
        for (int i = 0; i < 5; i++) {
            pointCreditService.credit(USER_ID, 100_000L, false, VALID_EXPIRE);
        }

        // 1원이라도 더 적립하면 예외
        assertThatThrownBy(() -> pointCreditService.credit(USER_ID, 1L, false, VALID_EXPIRE))
                .isInstanceOf(PointException.class)
                .satisfies(e -> assertThat(((PointException) e).getErrorCode())
                        .isEqualTo(ErrorCode.EXCEED_MAX_BALANCE));
    }

    @Test
    void 만료일_오늘이면_예외() {
        // 최소 내일 이상이어야 함 → 오늘 날짜는 예외
        assertThatThrownBy(() -> pointCreditService.credit(USER_ID, 1000L, false, LocalDate.now()))
                .isInstanceOf(PointException.class)
                .satisfies(e -> assertThat(((PointException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_EXPIRE_DATE));
    }

    @Test
    void 만료일_5년이상이면_예외() {
        // 최대 5년 미만 → 정확히 5년 후는 예외
        assertThatThrownBy(() -> pointCreditService.credit(USER_ID, 1000L, false, LocalDate.now().plusYears(5)))
                .isInstanceOf(PointException.class)
                .satisfies(e -> assertThat(((PointException) e).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_EXPIRE_DATE));
    }

    // ────────── 적립 취소 ──────────

    @Test
    void 적립취소_성공() {
        // given
        String creditKey = pointCreditService.credit(USER_ID, 1000L, false, VALID_EXPIRE);

        // when
        pointCreditService.cancelCredit(creditKey);

        // then : 상태가 CANCELLED 로 변경됐는지 확인
        PointCredit credit = pointCreditRepository.findByCreditKey(creditKey).orElseThrow();
        assertThat(credit.getStatus()).isEqualTo(CreditStatus.CANCELLED);
    }

    @Test
    void 적립취소_일부라도_사용됐으면_예외() {
        String creditKey = pointCreditService.credit(USER_ID, 1000L, false, VALID_EXPIRE);

        // 도메인 메서드로 직접 일부 차감
        PointCredit credit = pointCreditRepository.findByCreditKey(creditKey).orElseThrow();
        credit.deduct(500L);

        // 일부라도 사용됐으면 취소 불가
        assertThatThrownBy(() -> pointCreditService.cancelCredit(creditKey))
                .isInstanceOf(PointException.class)
                .satisfies(e -> assertThat(((PointException) e).getErrorCode())
                        .isEqualTo(ErrorCode.PARTIAL_USED_CREDIT));
    }
}