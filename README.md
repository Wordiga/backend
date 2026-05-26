# 워디가 백엔드

---

## 🛠️ 기술 스택

| 분류   | 스택                                              |
|------|-------------------------------------------------|
| Core | Java 25 / Spring Boot 4.0.6 / Gradle            |
| DB   | PostgreSQL 17 / Spring Data JPA / Spring Cache  |
| Auth | Spring Security / OAuth2 Client (Kakao, Google) |
| Docs | Springdoc OpenAPI 2.8.8 / Jakarta Validation    |

---

## 🏗️ 패키지 구조

```
com.wordiga
├── config/        # 전역 설정 (Security, CORS, WebClient 등)
├── controller/    # REST API 엔드포인트 (/api/v1/...)
├── service/       # 비즈니스 로직, 트랜잭션
├── repository/    # Spring Data JPA
└── domain/        # JPA Entity
```

---

## 🚀 환경별 구동 가이드

> ⚠️ `.env` 파일이 프로젝트 루트에 있어야 합니다.

### Local (IDE 개발용)

```bash
docker compose -f docker-compose.local.yml up -d
```

IntelliJ 설정:

- **Active profiles**: `local`

### Dev (컨테이너 통합 실행)

```bash
# 기동
docker compose -f docker-compose.dev.yml up -d

# 소스 변경 시 재빌드
docker compose -f docker-compose.dev.yml up -d --build
```

---

## 📄 API 문서 (Swagger)

구동 후 브라우저에서 접속:

```
http://localhost:8080/swagger-ui.html
```

---

## 🗃️️ DB 초기화

볼륨 삭제 후 `init.sql`부터 재생성:

```bash
# Local
docker compose -f docker-compose.local.yml down -v && docker compose -f docker-compose.local.yml up -d

# Dev
docker compose -f docker-compose.dev.yml down -v && docker compose -f docker-compose.dev.yml up -d
```
