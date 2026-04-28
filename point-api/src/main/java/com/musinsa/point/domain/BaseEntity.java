package com.musinsa.point.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 Entity 의 공통 필드를 관리하는 추상 클래스
 * @MappedSuperclass  : 이 클래스 자체는 테이블로 생성되지 않고, 상속받은 Entity 에 컬럼으로 포함됨
 * @EntityListeners   : JPA Auditing 기능을 활성화해서 createdAt, updatedAt 을 자동으로 관리
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}