package com.musinsa.point.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
// domain/PointDebitDetail.java

/**
 * 포인트 사용 상세
 * 사용 1건이 어떤 적립건에서 얼마를 차감했는지 1원 단위로 기록
 * 사용취소 시 이 테이블을 기준으로 각 적립건의 복구 금액을 결정
 */
@Getter
@Entity
@Table(name = "point_debit_detail")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointDebitDetail extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_debit_id", nullable = false)
    private PointDebit pointDebit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_credit_id", nullable = false)
    private PointCredit pointCredit;

    // 이 적립건에서 사용한 금액
    @Column(nullable = false)
    private Long usedAmount;

    // 사용취소 시 누적되며 usedAmount 범위 내에서만 취소 가능
    @Column(nullable = false)
    private Long cancelledAmount;

    // Service 에서 사용 상세 생성 시 사용
    public static PointDebitDetail create(PointDebit debit,
                                          PointCredit credit, Long usedAmount) {
        PointDebitDetail detail = new PointDebitDetail();
        detail.pointDebit      = debit;
        detail.pointCredit     = credit;
        detail.usedAmount      = usedAmount;
        detail.cancelledAmount = 0L;
        return detail;
    }

    // 사용취소 시 호출 — 이 적립건에서 취소할 금액 누적
    public void cancel(Long amount) {
        this.cancelledAmount += amount;
    }

    // Service 에서 추가 취소 가능 금액 확인 시 사용
    public Long cancellableAmount() {
        return this.usedAmount - this.cancelledAmount;
    }
}