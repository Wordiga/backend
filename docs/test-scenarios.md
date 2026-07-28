## 테스트 환경

| 구분 | 구성 |
|---|---|
| 단위 테스트 | JUnit 5, Mockito, AssertJ |
| Controller 테스트 | `@WebMvcTest`, MockMvc |
| 통합 테스트 | `@SpringBootTest`, Testcontainers `postgres:17-alpine` |
| 커버리지 | JaCoCo 0.8.15 |
| 품질 기준 | 핵심 서비스·Controller 라인·브랜치 커버리지 각각 80% 이상 |
| 현재 결과 | 598/610 lines(98.0%), 163/203 branches(80.3%) |

## 테스트 클래스 분리 기준

| 접미사 | 책임 |
|---|---|
| `UnitTest` | 서비스의 정상 흐름, 기본 분기와 변환을 검증합니다. |
| `UnitExceptionTest` | 누락값, 잘못된 원천 데이터와 도메인 예외를 검증합니다. |
| `ControllerTest` | HTTP 요청 바인딩과 정상 응답을 검증합니다. |
| `ControllerExceptionTest` | HTTP 입력 검증과 400 응답을 검증합니다. |
| `IntegrationTest` | PostgreSQL 저장·조회·삭제와 정상 동시 처리를 검증합니다. |
| `IntegrationExceptionTest` | PostgreSQL 유니크 제약과 동시 경합 시 예외·무교착을 검증합니다. |

## 테스트 시나리오

| 테스트 클래스 | 구분 | 시나리오 | 검증 내용 |
|---|---|---|---|
| `TourismContentServiceUnitTest` | 성공 | 키워드 검색 | 충남·타입·시군구·페이지 조건과 `hasNext`를 검증합니다. |
| `TourismContentServiceUnitTest` | 성공 | 계절 추천 | 방문일 전년도 동월을 서비스 수요 기준월로 사용하는지 검증합니다. |
| `TourismContentServiceUnitTest` | 성공 | 인기 추천 | 소비 60%·체류 40% 지역 정렬과 관광타입 매핑을 검증합니다. |
| `TourismContentServiceUnitExceptionTest` | 예외 | 공공 API 응답 누락 | 목록·검색 응답이 없을 때 빈 페이지로 안전하게 처리하는지 검증합니다. |
| `TourismContentDetailServiceUnitTest` | 성공 | 통합 상세 조회 | 공통·소개·반복·이미지·소비지수·계절 이미지를 조립하는지 검증합니다. |
| `TourismContentDetailServiceUnitExceptionTest` | 예외 | 콘텐츠 누락·지역 불일치 | 존재하지 않거나 충남 외 콘텐츠를 404로 거부하는지 검증합니다. |
| `TourismDetailMapperUnitTest` | 성공 | 타입별 DTO 변환 | 8개 관광타입 소개정보와 숙박 객실 이미지 변환을 검증합니다. |
| `TourismDetailMapperUnitTest` | 예외 | null·잘못된 숫자 | 누락 목록과 잘못된 일련번호를 안전하게 변환하는지 검증합니다. |
| `TourismSatisfactionServiceUnitTest` | 성공 | 만족도 산출 | 인기도 30%, 연령 35%, 체류 20%, 쾌적도 15% 가중합을 검증합니다. |
| `TourismSatisfactionServiceUnitTest` | 성공 | 일부 데이터 누락 | 연령 입력 누락 시 중립값 50과 `imputed=true`를 검증합니다. |
| `TourismSatisfactionServiceUnitExceptionTest` | 예외 | 전체 원천 데이터 누락 | 모든 산출 데이터가 없으면 만족도를 `null`로 제공하는지 검증합니다. |
| `WishServiceUnitTest` | 성공 | 위시 등록·중복·폴더 조회 | 시군구 자동 폴더링, 좌표, 중복 반환, 조회와 삭제 위임을 검증합니다. |
| `WishServiceUnitExceptionTest` | 예외 | 콘텐츠 누락·충남 외 콘텐츠 | 각각 404와 422로 저장을 거부하는지 검증합니다. |
| `TourismContentControllerTest` | 성공 | 목록·상세 HTTP 조회 | Query Parameter 바인딩과 JSON 응답 구조를 검증합니다. |
| `TourismContentControllerExceptionTest` | 예외 | 잘못된 페이지·성별·날짜 | Validation과 도메인 입력 오류가 400인지 검증합니다. |
| `WishControllerTest` | 성공 | 위시 HTTP CRUD | 등록·삭제·폴더·폴더별 목록 요청을 검증합니다. |
| `WishControllerExceptionTest` | 예외 | 빈 콘텐츠 ID·Body 누락 | 잘못된 Request Body가 400인지 검증합니다. |
| `WordigaApplicationIntegrationTest` | 성공 | 애플리케이션 기동 | 임시 PostgreSQL에서 전체 Spring Context가 기동되는지 검증합니다. |
| `WishServiceIntegrationTest` | 성공 | PostgreSQL CRUD | 위시와 생성시각이 저장되고 폴더 집계·조회·삭제되는지 검증합니다. |
| `WishServiceIntegrationTest` | 성공 | 서로 다른 위시 동시 저장 | 두 트랜잭션이 10초 안에 완료되고 두 행이 저장되어 교착이 없는지 검증합니다. |
| `WishServiceIntegrationExceptionTest` | 예외 | 동일 위시 동시 저장 | DB 유니크 제약으로 한 행만 남고 한 요청만 실패하며 10초 안에 종료되는지 검증합니다. |

## JaCoCo 관리 범위

다음 핵심 실행 로직을 80% 게이트 대상으로 관리합니다.

- 관광 콘텐츠 목록·통합 상세·만족도·타입 변환 서비스
- 위시 서비스
- 관광 콘텐츠·위시 Controller
- 공통 요청값 예외 처리

DTO, JPA Entity, Spring 설정, 외부 API 전송 객체처럼 실행 분기보다 선언이 중심인 클래스는
라인·브랜치 커버리지 게이트에서 제외합니다.
