package com.musinsa.point.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;
// domain/PointDebit.java

/**
 * 포인트 사용 1건
 * 어떤 적립건에서 얼마를 사용했는지는 PointDebitDetail 에서 관리
 */
@Getter
@Entity
@Table(name = "point_debit")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointDebit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // API로 제공 될 KEY
    @Column(nullable = false, unique = true)
    private String debitKey;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String orderNo;

    @Column(nullable = false)
    private Long totalAmount;

    // 부분취소 시 누적되며 totalAmount 와 같아지면 전액 취소 상태가 됨
    @Column(nullable = false)
    private Long cancelledAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DebitStatus status;

    // Service 에서 새 사용건 생성 시 사용
    public static PointDebit create(String userId, String orderNo, Long totalAmount) {
        PointDebit debit = new PointDebit();
        debit.debitKey         = UUID.randomUUID().toString();
        debit.userId           = userId;
        debit.orderNo          = orderNo;
        debit.totalAmount      = totalAmount;
        debit.cancelledAmount  = 0L;        // 처음엔 항상 0
        debit.status           = DebitStatus.USED;
        return debit;
    }

    // 사용취소 시 호출 — 취소금액 누적과 status 변경을 항상 세트로 보장
    public void addCancelledAmount(Long amount) {
        this.cancelledAmount += amount;
        if (this.cancelledAmount.equals(this.totalAmount)) {
            this.status = DebitStatus.CANCELLED;        // 전액 취소
        } else {
            this.status = DebitStatus.PARTIAL_CANCEL;   // 부분 취소
        }
    }

    // Service 에서 추가 취소 가능 금액 확인 시 사용
    public Long cancellableAmount() {
        return this.totalAmount - this.cancelledAmount;
    }
}