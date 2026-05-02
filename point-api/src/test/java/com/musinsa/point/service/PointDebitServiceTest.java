package com.musinsa.point.service;

import com.musinsa.point.domain.CreditStatus;
import com.musinsa.point.domain.DebitStatus;
import com.musinsa.point.domain.PointCredit;
import com.musinsa.point.domain.PointDebit;
import com.musinsa.point.repository.PointCreditRepository;
import com.musinsa.point.repository.PointDebitRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PointDebitServiceTest {

    @Autowired PointDebitService pointDebitService;
    @Autowired PointCreditService pointCreditService;
    @Autowired PointCreditRepository pointCreditRepository;
    @Autowired PointDebitRepository pointDebitRepository;

    // DB 데이터를 강제로 바꿔야 할 때 사용 (만료일 과거로 변경 등)
    @PersistenceContext EntityManager em;

    private static final String USER_ID = "testUser";

    // ────────── 차감 순서 검증 ──────────

    @Test
    void 수기지급_포인트_우선차감() {
        // given : 일반 적립 먼저, 수기 적립 나중에 등록
        String normalKey = pointCreditService.credit(USER_ID, 1000L, false, LocalDate.now().plusDays(60));
        String manualKey = pointCreditService.credit(USER_ID, 1000L, true,  LocalDate.now().plusDays(60));

        // when : 500원 사용
        pointDebitService.debit(USER_ID, "ORDER-001", 500L);

        // then : 수기 적립에서만 차감돼야 함
        PointCredit manual = pointCreditRepository.findByCreditKey(manualKey).orElseThrow();
        PointCredit normal = pointCreditRepository.findByCreditKey(normalKey).orElseThrow();

        assertThat(manual.getRemainAmount()).isEqualTo(500L);  // 수기 : 차감됨
        assertThat(normal.getRemainAmount()).isEqualTo(1000L); // 일반 : 그대로
    }

    @Test
    void 만료일이_짧은건부터_차감() {
        // given : 만료일 늦은 것 먼저 등록 (등록 순서와 무관하게 만료일 기준으로 차감돼야 함)
        String laterKey  = pointCreditService.credit(USER_ID, 1000L, false, LocalDate.now().plusDays(60));
        String soonerKey = pointCreditService.credit(USER_ID, 1000L, false, LocalDate.now().plusDays(10));

        // when : 1500원 사용 (두 적립건 모두 소진)
        pointDebitService.debit(USER_ID, "ORDER-001", 1500L);

        // then : 만료 빠른 것(sooner)이 먼저 전액 소진돼야 함
        PointCredit sooner = pointCreditRepository.findByCreditKey(soonerKey).orElseThrow();
        PointCredit later  = pointCreditRepository.findByCreditKey(laterKey).orElseThrow();

        assertThat(sooner.getRemainAmount()).isEqualTo(0L);    // 먼저 전액 소진
        assertThat(sooner.getStatus()).isEqualTo(CreditStatus.USED);
        assertThat(later.getRemainAmount()).isEqualTo(500L);   // 나머지 500원만 차감
    }

    // ────────── 사용취소 검증 ──────────

    @Test
    void 부분취소_후_추가취소_시_금액_체크() {
        // given : 1000원 적립 후 1000원 사용
        pointCreditService.credit(USER_ID, 1000L, false, LocalDate.now().plusDays(30));
        String debitKey = pointDebitService.debit(USER_ID, "ORDER-001", 1000L);

        // when : 300원 부분 취소
        pointDebitService.cancelDebit(debitKey, 300L);

        // then
        PointDebit debit = pointDebitRepository.findByDebitKey(debitKey).orElseThrow();
        assertThat(debit.getStatus()).isEqualTo(DebitStatus.PARTIAL_CANCEL);
        assertThat(debit.cancellableAmount()).isEqualTo(700L); // 1000 - 300
        assertThat(pointCreditService.getBalance(USER_ID)).isEqualTo(300L); // 복구된 금액
    }

    @Test
    void 만료된_적립건_사용취소시_신규적립() {
        String creditKey = pointCreditService.credit(USER_ID, 1000L, false, LocalDate.now().plusDays(30));
        String debitKey  = pointDebitService.debit(USER_ID, "ORDER-001", 1000L);

        em.createQuery("UPDATE PointCredit pc SET pc.expiredAt = :past WHERE pc.creditKey = :key")
                .setParameter("past", LocalDate.now().minusDays(1))
                .setParameter("key", creditKey)
                .executeUpdate();
        em.flush();
        em.clear();

        // 취소 전 기존 creditKey 목록 저장
        List<String> existingKeys = pointCreditRepository.findAll().stream()
                .map(PointCredit::getCreditKey)
                .toList();

        // when
        pointDebitService.cancelDebit(debitKey, 1000L);

        // then : 기존 목록에 없는 것 = 방금 새로 생긴 적립건
        PointCredit newCredit = pointCreditRepository.findAll().stream()
                .filter(c -> !existingKeys.contains(c.getCreditKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("신규 적립건이 생성되지 않았습니다."));
        PointCredit originCredit = pointCreditRepository.findByCreditKey(creditKey).orElseThrow();;

        assertThat(newCredit.getRemainAmount()).isEqualTo(1000L);
        assertThat(newCredit.getStatus()).isEqualTo(CreditStatus.ACTIVE); // 만료후 신규적립 상태
        assertThat(newCredit.getExpiredAt()).isEqualTo(LocalDate.now().plusDays(365)); // 만료후 신규적립 유효일자

        assertThat(originCredit.getStatus()).isEqualTo(CreditStatus.USED); // 기존 만료 건 적립 상태
        assertThat(pointCreditService.getBalance(USER_ID)).isEqualTo(1000L);
    }

    // ────────── 예시 시나리오 ──────────

    @Test
    void 예시_시나리오_전체_검증() {
        // [1] A : 1000원 적립 (만료 빠름)
        String keyA = pointCreditService.credit(USER_ID, 1000L, false, LocalDate.now().plusDays(10));
        // [2] B : 500원 적립 (만료 느림)
        String keyB = pointCreditService.credit(USER_ID, 500L, false, LocalDate.now().plusDays(60));

        assertThat(pointCreditService.getBalance(USER_ID)).isEqualTo(1500L);

        // [3] 주문번호 A1234 에서 1200원 사용
        String keyC = pointDebitService.debit(USER_ID, "A1234", 1200L);

        // A에서 1000원, B에서 200원 차감
        assertThat(pointCreditService.getBalance(USER_ID)).isEqualTo(300L);
        assertThat(pointCreditRepository.findByCreditKey(keyA).orElseThrow().getRemainAmount()).isEqualTo(0L);
        assertThat(pointCreditRepository.findByCreditKey(keyB).orElseThrow().getRemainAmount()).isEqualTo(300L);

        // [4] A 만료 처리
        em.createQuery("UPDATE PointCredit pc SET pc.expiredAt = :past WHERE pc.creditKey = :key")
                .setParameter("past", LocalDate.now().minusDays(1))
                .setParameter("key", keyA)
                .executeUpdate();
        em.flush();
        em.clear();

        // 취소 전 기존 creditKey 목록 저장
        List<String> existingKeys = pointCreditRepository.findAll().stream()
                .map(PointCredit::getCreditKey)
                .toList();

        // [5] 1100원 부분 취소
        pointDebitService.cancelDebit(keyC, 1100L);

        PointCredit newCredit = pointCreditRepository.findAll().stream()
                .filter(c -> !existingKeys.contains(c.getCreditKey()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("신규 적립건이 생성되지 않았습니다."));

        // A(만료) → 1000원 신규 적립 생성
        assertThat(newCredit.getRemainAmount()).isEqualTo(1000L);
        assertThat(newCredit.getStatus()).isEqualTo(CreditStatus.ACTIVE); // 만료후 신규적립 상태
        assertThat(newCredit.getExpiredAt()).isEqualTo(LocalDate.now().plusDays(365)); // 만료후 신규적립 유효일자

        // B(미만료) → 잔액 300 → 400 복구
        assertThat(pointCreditRepository.findByCreditKey(keyB).orElseThrow().getRemainAmount()).isEqualTo(400L);

        // 전체 잔액 : 400(B) + 1000(신규적립) = 1400
        assertThat(pointCreditService.getBalance(USER_ID)).isEqualTo(1400L);

        // C : 부분취소 상태, 남은 취소 가능액 100원
        PointDebit debitC = pointDebitRepository.findByDebitKey(keyC).orElseThrow();
        assertThat(debitC.getStatus()).isEqualTo(DebitStatus.PARTIAL_CANCEL);
        assertThat(debitC.cancellableAmount()).isEqualTo(100L);
    }
}