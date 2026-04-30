package com.musinsa.point.controller;

import com.musinsa.point.dto.request.CancelDebitRequest;
import com.musinsa.point.dto.request.CreditRequest;
import com.musinsa.point.dto.request.DebitRequest;
import com.musinsa.point.dto.response.BalanceResponse;
import com.musinsa.point.dto.response.CreditResponse;
import com.musinsa.point.dto.response.DebitResponse;
import com.musinsa.point.service.PointCreditService;
import com.musinsa.point.service.PointDebitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 포인트 API 컨트롤러
 * HTTP 요청을 받아 서비스에 위임하고, 결과를 HTTP 응답으로 반환하는 역할만 담당
 * 비즈니스 로직은 절대 여기서 작성하지 않는다
 */
@RestController
// @RestController = @Controller + @ResponseBody
// @Controller     : 이 클래스가 Spring MVC 컨트롤러임을 선언
// @ResponseBody   : 반환 객체를 JSON으로 자동 직렬화해서 응답 바디에 담음
// → 메서드마다 @ResponseBody 를 붙이지 않아도 됨

@RequestMapping("/api/v1/points")
// 이 컨트롤러의 모든 메서드 URL 앞에 /api/v1/points 가 공통으로 붙음
// 각 메서드에서는 그 뒤의 경로만 추가로 정의하면 됨

@RequiredArgsConstructor
public class PointController {

    // 적립 / 적립취소 / 잔액조회 서비스
    private final PointCreditService pointCreditService;

    // 사용 / 사용취소 서비스
    private final PointDebitService pointDebitService;

    /**
     * 포인트 적립
     * POST /api/v1/points/{userId}/credit
     */
    @PostMapping("/{userId}/credit")
    public ResponseEntity<CreditResponse> credit(
            @PathVariable String userId,
            // @PathVariable : URL 경로의 {userId} 값을 파라미터에 바인딩
            // 예) /api/v1/points/user123/credit → userId = "user123"

            @Valid @RequestBody CreditRequest request
            // @RequestBody : HTTP 요청 바디의 JSON 문자열을 CreditRequest 객체로 자동 변환
            // Jackson 라이브러리가 처리하며, @NoArgsConstructor + @Getter 가 필요한 이유
    ) {
        String creditKey = pointCreditService.credit(
                userId,
                request.getAmount(),
                request.isManual(),
                request.getExpiredAt()   // null 이면 서비스에서 기본값(365일) 적용
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)   // 201 Created : 새 리소스 생성 성공
                .body(CreditResponse.of(creditKey));
    }

    /**
     * 포인트 적립취소
     * DELETE /api/v1/points/credit/{creditKey}
     */
    @DeleteMapping("/credit/{creditKey}")
    public ResponseEntity<Void> cancelCredit(
            @PathVariable String creditKey
            // 취소할 적립건의 식별 키를 URL 경로에서 받음
            // 요청 바디 없음 — DELETE 요청은 일반적으로 바디를 사용하지 않음
    ) {
        pointCreditService.cancelCredit(creditKey);

        return ResponseEntity
                .noContent()   // 204 No Content : 처리 성공, 반환할 내용 없음
                .build();      // body 없이 ResponseEntity 를 완성
    }

    /**
     * 포인트 사용
     * POST /api/v1/points/{userId}/debit
     */
    @PostMapping("/{userId}/debit")
    public ResponseEntity<DebitResponse> debit(
            @PathVariable String userId,
            @Valid @RequestBody DebitRequest request
    ) {
        String debitKey = pointDebitService.debit(
                userId,
                request.getOrderNo(),
                request.getAmount()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)   // 201 Created : 새 사용 리소스 생성
                .body(DebitResponse.of(debitKey));
    }

    /**
     * 포인트 사용취소 (전체 또는 부분)
     * POST /api/v1/points/debit/{debitKey}/cancel
     */
    @PostMapping("/debit/{debitKey}/cancel")
    public ResponseEntity<Void> cancelDebit(
            @PathVariable String debitKey,
            @Valid @RequestBody CancelDebitRequest request
            // 취소 금액을 바디로 받음
            // DELETE 대신 POST 를 사용하는 이유 :
            //   취소 금액이라는 추가 정보가 필요하고,
            //   부분취소가 가능해서 같은 debitKey 로 여러 번 요청할 수 있음
            //   DELETE 는 "리소스 삭제"의 의미라서 부분 취소 개념과 맞지 않음
    ) {
        pointDebitService.cancelDebit(debitKey, request.getCancelAmount());

        return ResponseEntity
                .ok()     // 200 OK : 기존 리소스의 상태를 변경한 경우
                .build(); // 반환할 바디 없음
    }

    /**
     * 포인트 잔액조회
     * GET /api/v1/points/{userId}/balance
     */
    @GetMapping("/{userId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(
            @PathVariable String userId
            // 요청 바디 없음 — 조회 조건은 URL 경로 또는 쿼리 파라미터로 전달
    ) {
        Long balance = pointCreditService.getBalance(userId);

        return ResponseEntity
                .ok()   // 200 OK
                .body(BalanceResponse.of(userId, balance));
    }
}