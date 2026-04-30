package com.musinsa.point.repository;

import com.musinsa.point.domain.PointDebitDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 포인트 사용 상세 레포지토리
 */
public interface PointDebitDetailRepository extends JpaRepository<PointDebitDetail, Long> {

    // 사용취소 시 어느 적립건에서 얼마나 썼는지 역추적
    // 수기지급 우선 → 만료일 오름차순 → id 오름차순
    // 사용 시 차감 순서(findDebitTargets)와 동일하게 맞춰 복구 순서를 보장
    @Query("""
        SELECT pdd FROM PointDebitDetail pdd
        JOIN FETCH pdd.pointCredit
        WHERE pdd.pointDebit.id = :pointDebitId
        ORDER BY pdd.pointCredit.manual DESC,
                 pdd.pointCredit.expiredAt ASC,
                 pdd.id ASC
        """)
    List<PointDebitDetail> findWithCreditByPointDebitId(@Param("pointDebitId") Long pointDebitId);
}