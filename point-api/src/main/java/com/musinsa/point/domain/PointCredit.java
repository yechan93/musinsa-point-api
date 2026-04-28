package com.musinsa.point.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;
// domain/PointCredit.java

/**
 * 포인트 적립 1건
 * remainAmount 를 기준으로 사용 가능 잔액을 관리하며,
 * isManual 과 expiredAt 으로 사용 우선순위를 결정한다
 */
@Getter
@Entity
@Table(name = "point_credit")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointCredit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // API로 제공 될 KEY
    @Column(nullable = false, unique = true)
    private String creditKey;

    @Column(nullable = false)
    private String userId;

    // 최초 적립액 — 사용/취소와 무관하게 절대 변경되지 않음, 이력 추적용
    @Column(nullable = false)
    private Long originalAmount;

    // 실시간 사용 가능 잔액 — 사용 시 차감, 사용취소 시 복구
    @Column(nullable = false)
    private Long remainAmount;

    // true 면 사용 시 일반 적립보다 우선 차감
    @Column(nullable = false)
    private boolean isManual;

    @Column(nullable = false)
    private LocalDate expiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditStatus status;

    // Service 에서 새 적립건 생성 시 사용
    // new PointCredit() 대신 이 메서드로만 생성하도록 강제
    public static PointCredit create(String userId, Long amount,
                                     boolean isManual, LocalDate expiredAt) {
        PointCredit credit = new PointCredit();
        credit.creditKey      = UUID.randomUUID().toString();
        credit.userId         = userId;
        credit.originalAmount = amount;
        credit.remainAmount   = amount;     // 처음엔 originalAmount 와 동일
        credit.isManual       = isManual;
        credit.expiredAt      = expiredAt;
        credit.status         = CreditStatus.ACTIVE;
        return credit;
    }

    // 사용 시 호출 — 차감과 status 변경을 항상 세트로 보장
    public void deduct(Long amount) {
        this.remainAmount -= amount;
        if (this.remainAmount == 0) {
            this.status = CreditStatus.USED;
        }
    }

    // 사용취소 시 호출 — 복구와 status 변경을 항상 세트로 보장
    public void restore(Long amount) {
        this.remainAmount += amount;
        if (this.status == CreditStatus.USED) {
            this.status = CreditStatus.ACTIVE;
        }
    }

    // 적립취소 시 호출
    public void cancel() {
        this.status = CreditStatus.CANCELLED;
    }

    // 만료 여부 확인 — Service 에서 만료 판단 시 사용
    public boolean isExpired() {
        return LocalDate.now().isAfter(this.expiredAt);
    }
}