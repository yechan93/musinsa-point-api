# 무신사페이먼츠 포인트 시스템

> 무료 포인트의 적립 · 적립취소 · 사용 · 사용취소를 처리하는 REST API 서버입니다.

---

## 목차

- [기술 스택](#기술-스택)
- [빠른 시작](#빠른-시작)
- [정책 설정](#정책-설정)
- [API 명세](#api-명세)
- [ERD](#erd)
- [설계 결정 사항](#설계-결정-사항)
- [한계 및 개선 포인트](#한계-및-개선-포인트)

---

## 기술 스택

| 분류 | 기술                       |
|------|--------------------------|
| Language | Java 21                  |
| Framework | Spring Boot 3.5.14       |
| ORM | Spring Data JPA          |
| Database | H2 (In-Memory, MySQL 모드) |
| Build | Gradle                   |
| Test | JUnit 5, AssertJ         |

---

## 빠른 시작

### 실행

```bash
./gradlew bootRun

1. 포인트 적립
curl -X POST http://localhost:8080/api/v1/points/user1/credit \
-H "Content-Type: application/json" \
-d '{"amount": 10000, "manual": false}'

2. 잔액 조회
curl http://localhost:8080/api/v1/points/user1/balance

3. 포인트 사용
curl -X POST http://localhost:8080/api/v1/points/user1/debit \
-H "Content-Type: application/json" \
-d '{"orderNo": "ORDER-001", "amount": 5000}'

# 위 적립 응답의 creditKey, 사용 응답의 debitKey를 각각 대입
4. 사용취소
curl -X POST http://localhost:8080/api/v1/points/debit/{debitKey}/cancel \
-H "Content-Type: application/json" \
-d '{"cancelAmount": 3000}'

5. 적립취소
curl -X DELETE http://localhost:8080/api/v1/points/credit/{creditKey}
```
### H2 콘솔 (데이터 직접 확인)

```
URL  : http://localhost:8080/h2-console
JDBC : jdbc:h2:mem:pointdb
User : sa
PW   : (없음)
```

### 테스트 실행

```bash
./gradlew test
```

---

## 정책 설정

한도값은 하드코딩 없이 `application.yml` 에서 관리합니다.  
PointPolicyConfig 클래스에 바인딩되며, yml 값만 수정하면 코드 변경 없이 정책이 반영됩니다.


```yaml
point:
  max-credit-per-once: 100000   # 1회 최대 적립 한도
  max-balance-per-user: 500000  # 개인별 최대 보유 한도
  default-expire-days: 365      # 만료일 미지정 시 기본값 (일)
```

---

## API 명세

### 포인트 적립

```
POST /api/v1/points/{userId}/credit
```

**Request Body**

```json
{
  "amount": 10000,
  "manual": false,
  "expiredAt": "2026-12-31"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| amount | Long | ✅ | 적립 금액 (1 이상 ~ 100,000 이하) |
| manual | boolean | - | 수기지급 여부. 미입력 시 false |
| expiredAt | LocalDate (yyyy-MM-dd) | - | 만료일. 미입력 시 오늘 기준 +365일 |

**Response `201 Created`**

```json
{ "creditKey": "550e8400-e29b-41d4-a716-446655440000" }
```

---

### 포인트 적립취소

```
DELETE /api/v1/points/credit/{creditKey}
```

- 사용된 금액이 1원이라도 있으면 취소 불가
- 이미 취소된 적립건 재취소 불가

**Response `204 No Content`**

---

### 포인트 사용

```
POST /api/v1/points/{userId}/debit
```

**Request Body**

```json
{
  "orderNo": "ORDER-20250101-001",
  "amount": 5000
}
```

| 필드 | 타입 | 필수 | 설명           |
|------|------|------|--------------|
| orderNo | String | ✅ | 주문번호 |
| amount | Long | ✅ | 사용 금액 (1 이상) |

**사용 차감 우선순위**

1. 수기지급(`manual=true`) 포인트 우선
2. 만료일이 짧게 남은 순
3. 먼저 적립된 순 (id 오름차순)

**Response `201 Created`**

```json
{ "debitKey": "550e8400-e29b-41d4-a716-446655440001" }
```

---

### 포인트 사용취소

```
POST /api/v1/points/debit/{debitKey}/cancel
```

전체 또는 일부 취소가 가능합니다. 동일 `debitKey`로 여러 번 나눠서 취소할 수 있습니다.

**Request Body**

```json
{ "cancelAmount": 3000 }
```

**사용취소 처리 방식**

- 미만료 적립건 → 해당 적립건의 잔액을 직접 복구
- 이미 만료된 적립건 → 복구 대신 **동일 금액을 신규 적립으로 처리** (만료일 기본값 적용)

**Response `200 OK`**

---

### 잔액 조회

```
GET /api/v1/points/{userId}/balance
```

만료되지 않은 ACTIVE 상태 포인트의 잔액 합산값을 반환합니다.

**Response `200 OK`**

```json
{
  "userId": "user123",
  "balance": 35000
}
```

---

### 에러 응답 형식

```json
{
  "code": "INSUFFICIENT_BALANCE",
  "message": "포인트 잔액이 부족합니다."
}
```

| code | HTTP | 설명 |
|------|------|------|
| INVALID_CREDIT_AMOUNT      | 400 | 적립 금액이 1원 미만 |
| EXCEED_MAX_CREDIT_PER_ONCE | 400 | 1회 최대 적립 한도 초과 |
| EXCEED_MAX_BALANCE         | 400 | 개인 최대 보유 한도 초과 |
| INVALID_EXPIRE_DATE        | 400 | 만료일 범위 오류 (최소 1일 이상, 최대 5년 미만) |
| ALREADY_CANCELLED_CREDIT   | 400 | 이미 취소된 적립건 |
| PARTIAL_USED_CREDIT        | 400 | 사용 이력이 있는 포인트는 적립취소 불가 |
| INVALID_DEBIT_AMOUNT       | 400 | 사용 금액이 1원 미만 |
| INVALID_CANCEL_AMOUNT      | 400 | 취소 금액이 1원 미만 |
| EXCEED_CANCELLABLE_AMOUNT  | 400 | 취소 가능 금액 초과 |
| INSUFFICIENT_BALANCE       | 400 | 포인트 잔액 부족 |
| CREDIT_NOT_FOUND           | 404 | 적립 내역 없음 |
| DEBIT_NOT_FOUND            | 404 | 사용 내역 없음 |
---

## ERD

> 상세 ERD 이미지는 `/resources/erd.png` 를 참고해 주세요.

```
┌───────────────────────┐        ┌──────────────────────────┐
│      point_credit     │        │    point_debit_detail    │
│───────────────────────│        │──────────────────────────│
│ id (PK)               │◀───────│ id (PK)                  │
│ credit_key (UK)       │        │ point_credit_id (FK)     │
│ user_id               │        │ point_debit_id  (FK) ──┐ │
│ original_amount       │        │ used_amount            │ │
│ remain_amount         │        │ cancelled_amount       │ │
│ manual                │        │ created_at             │ │
│ expired_at            │        │ updated_at             │ │
│ status                │        └────────────────────────┼─┘
│ created_at            │                                 │
│ updated_at            │                                 │
└───────────────────────┘                                 │
                                 ┌────────────────────────▼─┐
                                 │       point_debit        │
                                 │──────────────────────────│
                                 │ id (PK)                  │
                                 │ debit_key (UK)           │
                                 │ user_id                  │
                                 │ order_no                 │
                                 │ total_amount             │
                                 │ cancelled_amount         │
                                 │ status                   │
                                 │ created_at               │
                                 │ updated_at               │
                                 └──────────────────────────┘
```

**테이블 역할 요약**

| 테이블 | 역할 |
|--------|------|
| `point_credit` | 적립 1건. 실시간 잔액(`remain_amount`) 관리 |
| `point_debit` | 사용 1건. 주문번호 · 총 사용금액 · 취소 누적금액 관리 |
| `point_debit_detail` | 사용 1건이 어떤 적립건에서 얼마를 차감했는지 기록. 사용취소 시 역추적 기준 |

---

## 설계 결정 사항

### `original_amount`와 `remain_amount` 분리

적립 원금을 보존하기 위해 `point_credit` 에 금액 컬럼을 두 개로 분리했습니다.

| 컬럼 | 역할 |
|------|------|
| `original_amount` | 최초 적립액. 사용·취소와 무관하게 변경되지 않음 |
| `remain_amount` | 실시간 잔액. 사용 시 차감, 사용취소 시 복구 |

단일 컬럼으로 관리하면 잔액이 변경될 때 원래 얼마가 적립됐는지 알 수 없습니다.
두 컬럼을 분리해 `original_amount` 는 항상 고정으로 남겨,
적립 취소 시 "사용 이력이 있는지" 판단하는 기준으로도 활용합니다.

---

### 포인트 차감 순서를 쿼리 정렬로 강제

수기지급 우선 · 만료 임박 순 차감을 서비스 코드가 아닌 쿼리 `ORDER BY` 로 보장합니다.

```sql
ORDER BY manual DESC, expired_at ASC, id ASC
```

사용취소 시 복구 순서도 동일하게 맞춰, 차감과 복구의 순서가 항상 일치하도록 했습니다.

---

### 사용취소 역추적을 `point_debit_detail` 로 처리

사용 1건이 여러 적립건에 걸쳐 차감될 수 있기 때문에, 어느 적립건에서 얼마를 사용했는지 `point_debit_detail` 에 1원 단위로 기록합니다.  
사용취소 시 이 테이블을 조회해 각 적립건에 정확한 금액을 복구합니다.

```
사용 1200원 = A 적립에서 1000원 + B 적립에서 200원
          → point_debit_detail 2건 생성

취소 시   → 각 detail 역추적 → A 1000원, B 200원 각각 복구
```

---

## 과제 예시 시나리오 테스트 검증

과제 명세의 예시 흐름 전체를 `PointDebitServiceTest.예시_시나리오_전체_검증()` 으로 검증했습니다.

```
[1] A 1000원 적립 (만료 빠름)     → 잔액 1,000
[2] B 500원  적립 (만료 느림)     → 잔액 1,500
[3] 주문 A1234 에서 1200원 사용   → 잔액 300
    A 에서 1000원, B 에서 200원 차감
[4] A 만료 처리
[5] 1100원 부분취소               → 잔액 1,400
    A (만료)  → 1000원 신규 적립 생성
    B (미만료) → 잔액 300 → 400 복구
    남은 취소 가능액 : 100원
```

---

## 한계 및 개선 포인트

### 만료 포인트 상태가 자동으로 변경되지 않음

현재 만료된 포인트는 잔액 조회 쿼리 조건(`expired_at >= TODAY`)으로
합산에서만 제외되며, `status`는 `ACTIVE` 그대로 유지됩니다.

실 운영이라면 스케줄러가 만료일이 지난 적립건의 `status`를
`EXPIRED`로 일괄 변경하는 배치 처리가 필요합니다.

---

### 잔액 조회 성능

잔액 조회는 매번 `SUM(remain_amount)` 쿼리를 실행합니다.
적립 건수가 많아지면 쿼리 성능이 저하될 수 있으며,
대규모 트래픽 환경에서는 별도 잔액 캐시 또는 Redis 도입을 고려해야 합니다.