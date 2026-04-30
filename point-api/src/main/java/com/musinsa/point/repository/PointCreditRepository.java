package com.musinsa.point.repository;

import com.musinsa.point.domain.CreditStatus;
import com.musinsa.point.domain.PointCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 포인트 적립 레포지토리
 */
public interface PointCreditRepository extends JpaRepository<PointCredit, Long> {

    Optional<PointCredit> findByCreditKey(String creditKey);

    // 잔액 합산 : 유저의 ACTIVE 적립건 중 만료되지 않은 것만
    @Query("""
            SELECT COALESCE(SUM(pc.remainAmount), 0)
            FROM PointCredit pc
            WHERE pc.userId = :userId
              AND pc.status = :status
              AND pc.expiredAt >= CURRENT_DATE
            """)
    Long sumRemainAmountByUserIdAndStatus(@Param("userId") String userId,
                                          @Param("status") CreditStatus status);

    // 사용 시 차감 대상 목록 : 만료일 오름차순(선입선출), 단 수동적립은 맨 앞으로
    @Query("""
        SELECT pc
        FROM PointCredit pc
        WHERE pc.userId = :userId
          AND pc.status = :status
          AND pc.expiredAt >= CURRENT_DATE
        ORDER BY pc.manual DESC, pc.expiredAt ASC, pc.id ASC
        """)
    List<PointCredit> findDebitTargets(@Param("userId") String userId,
                                       @Param("status") CreditStatus status);
}