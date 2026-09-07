# 소셜 로그인 (OIDC) API 문서

## 프론트엔드 연동 가이드

소셜 로그인은 브라우저 리다이렉트 기반으로 동작합니다. 프론트엔드에서 직접 호출하는 REST API는 존재하지 않으며, 브라우저 URL 이동을 통해 로그인 플로우가 진행됩니다.

---

### 프론트엔드 처리 항목

| 순서 | 작업 | 설명 |
|------|------|------|
| 1 | 로그인 버튼 클릭 | 백엔드 OAuth2 엔드포인트로 브라우저 이동 |
| 2 | 콜백 페이지 토큰 추출 | URL 쿼리의 1시간 Access Token 저장. BE는 30일 Refresh Token을 HttpOnly 쿠키로 별도 발급 |
| 3 | API 요청 시 토큰 전달 | Authorization 헤더에 Bearer Token 첨부 |
| 4 | Access Token 만료 시 | Refresh Cookie로 `/api/v1/auth/refresh`를 호출해 Access Token 재발급 |

---

### 1단계: 로그인 요청

| 항목 | 내용 |
|------|------|
| **Method** | `GET` (브라우저 이동) |
| **URL** | `{백엔드}/oauth2/authorization/{provider}` |
| **provider** | `google` 또는 `kakao` |

```javascript
window.location.href = 'http://localhost:8080/oauth2/authorization/kakao';
```

해당 URL 접근 시 백엔드가 302 응답 → 브라우저가 Provider 로그인 페이지로 자동 이동. **프론트엔드 추가 처리 불필요.**

---

### 2단계: 콜백 처리

로그인 완료 후 백엔드가 아래 URL로 리다이렉트:

```
{FRONTEND_URL}/login/callback?token={JWT}
```

```javascript
const params = new URLSearchParams(window.location.search);
const token = params.get('token');

if (token) {
  localStorage.setItem('token', token);
  window.location.href = '/';
}
```

브라우저가 자동으로 해당 페이지를 로드하므로 URL에서 Access Token을 추출해 저장합니다. 같은 로그인 응답에서 BE는 JavaScript로 읽을 수 없는 Refresh Token 쿠키를 함께 설정합니다. Access Token의 URL/localStorage 전달은 FE 전환 완료 후 제거할 호환 경로입니다.

---

### 3단계: API 호출

| 항목 | 내용 |
|------|------|
| **Header** | `Authorization: Bearer {token}` |
| **토큰 만료** | Access Token 1시간, Refresh Token 30일 |
| **인증 실패** | `401 Unauthorized` |

```javascript
const token = localStorage.getItem('token');

const response = await fetch('/api/v1/wishes', {
  headers: { 'Authorization': `Bearer ${token}` }
});

if (response.status === 401) {
  localStorage.removeItem('token');
  window.location.href = '/login';
}
```

---

### 4단계: Access Token 재발급

```javascript
const response = await fetch('https://api.wordiga.site/api/v1/auth/refresh', {
  method: 'POST',
  credentials: 'include'
});
const { accessToken } = await response.json();
```

Google/Kakao Refresh Token을 사용하는 것이 아니라 Wordiga가 자체 발급한 Refresh Token 쿠키로 Wordiga Access Token을 재발급합니다.

### 5단계: 로그아웃

```javascript
localStorage.removeItem('token');
await fetch('https://api.wordiga.site/api/v1/auth/session', {
  method: 'DELETE',
  credentials: 'include'
});
```

클라이언트 Access Token을 삭제하고 서버 응답으로 Refresh Token 쿠키를 만료시킵니다.

---

## 전체 인증 Flow

![oicd-login.png](resource/oicd-login.png)

---

## 백엔드 내부 처리

### OIDC (OpenID Connect) 개요

OIDC는 OAuth 2.0 위에 구축된 인증 레이어입니다. OAuth 2.0이 "무엇에 접근할 수 있는가"(인가)를 다루는 반면, OIDC는 "사용자가 누구인가"(인증)를 표준화된 방식으로 확인합니다.

| 구분 | OAuth 2.0 | OIDC |
|------|-----------|------|
| 목적 | 리소스 접근 권한 위임 (인가) | 사용자 신원 확인 (인증) |
| 발급 토큰 | Access Token | Access Token + **ID Token (JWT)** |
| 사용자 정보 | 별도 API 호출 필요 | ID Token에 포함 (추가 호출 불필요) |
| 표준화 수준 | 프레임워크 (구현 자유도 높음) | 엄격한 프로토콜 (Provider 간 동일 인터페이스) |

### OIDC 선택 이유

- ID Token(JWT) 디코딩만으로 사용자 정보를 즉시 확인할 수 있어 추가 API 호출 불필요
- Google, Kakao 모두 동일한 표준 인터페이스로 처리 가능하여 Provider별 분기 코드가 최소화
- JWT 서명 검증을 통해 토큰 위변조 여부를 확인 가능

---
### OIDC 개요

| 구분 | OAuth 2.0 | OIDC |
|------|-----------|------|
| 목적 | 리소스 접근 권한 위임 (인가) | 사용자 신원 확인 (인증) |
| 토큰 | Access Token | Access Token + **ID Token (JWT)** |
| 사용자 정보 | 별도 API 호출 필요 | ID Token에 포함 (추가 호출 불필요) |

OIDC 채택으로 Provider별 사용자 정보 API 호출 제거, 네트워크 비용 절감, Google/Kakao 동일 인터페이스로 처리.

---

### Provider별 ID Token Claim

| 정보 | Google | Kakao |
|------|--------|-------|
| 고유 식별자 | `sub` | `sub` |
| 이메일 | `email` | `email` |
| 닉네임 | `name` | `nickname` |
| 프로필 이미지 | `picture` | `picture` |

---

### 토큰 구분

| 토큰 | 발급 주체 | 용도 |
|------|-----------|------|
| ID Token | Google/Kakao | 사용자 정보 추출 (백엔드 내부 소비 후 폐기) |
| **자체 Access JWT** | **본 서버** | **API 인가, 유효기간 1시간** |
| **자체 Refresh JWT** | **본 서버** | **Access Token 재발급, HttpOnly 쿠키, 유효기간 30일** |

---

### 접근 제어

Role 분리 없이 **로그인 여부(JWT 유효성)**로만 제어.

| 엔드포인트 | 접근 권한 |
|------------|-----------|
| `/api/v1/tourism/contents/**` | 비회원 가능 |
| `/api/v1/wishes/**` | 회원만 가능 |
| `/api/v1/plans/**` | 회원만 가능 |

---

## 환경 변수

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `GOOGLE_CLIENT_ID` | Google OAuth Client ID | 필수 |
| `GOOGLE_CLIENT_SECRET` | Google OAuth Client Secret | 필수 |
| `KAKAO_CLIENT_ID` | Kakao OAuth Client ID | 필수 |
| `KAKAO_CLIENT_SECRET` | Kakao OAuth Client Secret | 필수 |
| `JWT_SECRET` | JWT 서명 키 (최소 32자) | 필수 |
| `JWT_COOKIE_SECURE` | Refresh Cookie의 Secure 적용 여부 | `true` |
| `PLAN_MAX_STAY_DAYS` | AI 일정의 최대 체류 일수 | `3` |
| `FRONTEND_URL` | 프론트엔드 URL | `http://localhost:3000` |

---

## 보안 현황 및 보완 계획

### 현재 상태

| 항목 | 상태 | 리스크 |
|------|------|--------|
| 토큰 저장 | localStorage | XSS 시 탈취 가능 |
| 전송 프로토콜 | HTTP (개발) | 네트워크 스니핑 취약 |
| Access Token 만료 | 1시간 | FE 전환 전까지 URL/localStorage 노출 가능 |
| Refresh Token | 30일 HttpOnly 쿠키 | 개별 폐기 저장소는 없음 |

### 보완 로드맵

| 우선순위 | 항목 | 효과 |
|---------|------|------|
| P0 | HTTPS 적용 | 네트워크 구간 토큰 탈취 방지 |
| P1 | httpOnly Cookie 전환 | XSS로부터 토큰 보호 |
| 적용 | Refresh Token 도입 | Access Token 만료 시 재로그인 방지 |
| P3 | CSP Header 설정 | XSS 원천 차단 |

---

## 요청 예시

```http
### 카카오 로그인 (브라우저 이동)
GET /oauth2/authorization/kakao

### 구글 로그인 (브라우저 이동)
GET /oauth2/authorization/google

### 인증 필요 API
GET /api/v1/wishes
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

### 인증 불필요 API
GET /api/v1/tourism/contents
```
