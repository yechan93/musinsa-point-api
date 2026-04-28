package com.musinsa.point;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing // BaseEntity 의 createdAt, updatedAt 자동 관리를 활성화
@SpringBootApplication
public class PointApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PointApiApplication.class, args);
	}

}
