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
    // JOIN FETCH 로 pointCredit 을 한 번에 조회해 N+1 방지
    @Query("""
            SELECT pdd FROM PointDebitDetail pdd
            JOIN FETCH pdd.pointCredit
            WHERE pdd.pointDebit.id = :pointDebitId
            """)
    List<PointDebitDetail> findWithCreditByPointDebitId(@Param("pointDebitId") Long pointDebitId);
}