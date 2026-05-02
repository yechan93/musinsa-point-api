package com.musinsa.point.repository;

import com.musinsa.point.domain.PointDebit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 포인트 사용 레포지토리
 */
public interface PointDebitRepository extends JpaRepository<PointDebit, Long> {

    Optional<PointDebit> findByDebitKey(String debitKey);

}