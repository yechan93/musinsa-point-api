package com.musinsa.point.domain;

public enum DebitStatus {
    USED,           // 사용 완료
    PARTIAL_CANCEL, // 일부 취소됨
    CANCELLED       // 전액 취소됨
}
