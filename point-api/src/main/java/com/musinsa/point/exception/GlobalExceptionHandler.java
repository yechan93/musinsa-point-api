package com.musinsa.point.exception;

import com.musinsa.point.dto.response.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 포인트 비즈니스 예외 — ErrorCode 에 정의된 상태코드와 메시지 그대로 응답
    @ExceptionHandler(PointException.class)
    public ResponseEntity<ErrorResponse> handlePointException(PointException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(new ErrorResponse(errorCode.name(), errorCode.getMessage()));
    }

    // @Valid 검증 실패 예외 — 필수값 누락, 형식 오류 등 클라이언트 요청 문제
    // MethodArgumentNotValidException : @Valid 가 붙은 @RequestBody 검증 실패 시 Spring 이 던짐
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {

        // 검증 실패한 필드가 여러 개일 수 있음
        // getFieldErrors() 로 전체 목록을 가져온 뒤 첫 번째 에러만 응답에 담음
        String message = e.getBindingResult()
                .getFieldErrors()           // 실패한 필드 에러 목록
                .getFirst()                 // 첫 번째 에러
                .getDefaultMessage();       // @NotNull(message = "...") 에 작성한 메시지

        return ResponseEntity
                .badRequest()   // 400 Bad Request : 클라이언트가 잘못된 요청을 보낸 것
                .body(new ErrorResponse("INVALID_REQUEST", message));
    }

    // 그 외 예상치 못한 예외 — 500으로 통일
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다."));
    }
}