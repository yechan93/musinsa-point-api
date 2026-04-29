package com.musinsa.point.repository;

import com.musinsa.point.domain.PointDebitDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 포인트 사용 상세 레포지토리
 */
public interface PointDebitDetailRepository extends JpaRepository<PointDebitDetail, Long> {

    // 사용취소 시 어느 적립건에서 얼마나 썼는지 역추적하기 위해 사용
    List<PointDebitDetail> findByPointDebitId(Long pointDebitId);
}