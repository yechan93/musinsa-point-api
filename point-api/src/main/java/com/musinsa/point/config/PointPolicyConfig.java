package com.musinsa.point.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * application.yml 의 point 설정값을 Java 코드에서 사용할 수 있도록 바인딩하는 설정 클래스
 * 한도값 변경이 필요할 때 yml 파일만 수정하면 되므로 하드코딩을 방지할 수 있다
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "point")  // yml의 'point:' 하위 값들을 이 클래스에 바인딩
public class PointPolicyConfig {

    //1회 최대 적립 한도
    private long maxCreditPerOnce;

    // 개인별 최대 보유 한도
    private long maxBalancePerUser;

    // 기본 만료일
    private int defaultExpireDays;
}