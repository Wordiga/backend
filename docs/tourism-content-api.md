# 관광 콘텐츠 추천 API 문서

## 기본 정보

| 항목         | 내용                              |
|------------|---------------------------------|
| **Method** | `GET`                           |
| **Path**   | `/api/v1/tourism/contents`      |
| **인증**     | 불필요 (permitAll)                 |
| **설명**     | 충남 지역 관광 콘텐츠를 인기순 또는 시즌 추천으로 제공 |

---

## Request Parameters

| 파라미터        | 타입              | 필수 | 기본값          | 설명                                                 |
|-------------|-----------------|----|--------------|----------------------------------------------------|
| `type`      | `String` (enum) | X  | `POPULAR`    | 추천 유형. `POPULAR` (인기순) / `SEASONAL` (시즌 추천)        |
| `baseYm`    | `String`        | X  | null (자동 계산) | 기준 연월 (형식: `YYYYMM`). SEASONAL일 때만 사용. 미입력 시 작년 당월 |
| `numOfRows` | `int`           | X  | `5`          | 응답 콘텐츠 수 (1~10 권장)                                 |

---

## Response Body

```json
[
  {
    "contentId": "126128",
    "contentTypeId": "12",
    "title": "안면도 꽃지 해수욕장",
    "location": "충청남도 태안군",
    "firstimage": "http://tong.visitkorea.or.kr/cms/resource/..._image2_1.jpg",
    "estimatedDurationMin": null,
    "categoryName": "관광지"
  },
  ...
]
```

| 필드                     | 타입        | 설명                                                             |
|------------------------|-----------|----------------------------------------------------------------|
| `contentId`            | `String`  | 관광공사 콘텐츠 ID                                                    |
| `contentTypeId`        | `String`  | 관광타입 코드 (12:관광지, 14:문화시설, 15:축제, 28:레포츠, 32:숙박, 38:쇼핑, 39:음식점) |
| `title`                | `String`  | 콘텐츠 제목                                                         |
| `location`             | `String`  | 주소 (시도 + 시군구)                                                  |
| `firstimage`           | `String`  | 대표 이미지 URL                                                     |
| `estimatedDurationMin` | `Integer` | 예상 소요 시간 (분). 현재 미제공                                           |
| `categoryName`         | `String`  | 카테고리명 (관광지, 문화시설 등)                                            |

---

## 추천 로직 상세

### type=POPULAR (인기순)

#### 기준

**관광 소비 강도 (60%) + 관광 체류 강도 (40%)** 종합 점수가 높은 충남 시군구의 콘텐츠 제공

#### 데이터 기준월 자동 계산

- 현재 날짜가 **16일 이후** → 전월 데이터 사용
- 현재 날짜가 **16일 이전** → 전전월 데이터 사용
- (공공데이터 갱신이 매월 16일이므로)

#### 내부 호출 API

| 순서 | 외부 API              | 오퍼레이션               | 파라미터                                                        | 목적                             |
|----|---------------------|---------------------|-------------------------------------------------------------|--------------------------------|
| 1  | AreaTarDemDsService | `areaTarExpDsList`  | baseYm=자동계산, areaCd=44, tarExpDsIxCd=2201                   | 충남 시군구별 **외지인 소비액** 조회         |
| 2  | AreaTarDemDsService | `areaTarSjrnDsList` | baseYm=자동계산, areaCd=44, tarSjrnDsIxCd=2101                  | 충남 시군구별 **타권역 방문자 비중** 조회      |
| 3  | KorService2         | `areaBasedList2`    | lDongRegnCd=44, lDongSignguCd=상위시군구, arrange=Q, numOfRows=1 | 상위 시군구별 **대표이미지 있는 콘텐츠** 1건 조회 |

#### 점수 계산

```
종합점수 = (외지인 소비액 지표값 × 0.6) + (타권역 방문자 비중 지표값 × 0.4)
```

상위 N개 시군구를 추출하여 각 시군구에서 콘텐츠 1건씩 조회.

---

### type=SEASONAL (시즌 추천)

#### 기준

**작년 동월 관광 서비스 수요**가 높았던 충남 시군구의 콘텐츠 제공

#### 데이터 기준월

- `baseYm` 파라미터 제공 시 → 해당 연월 사용
- 미제공 시 → **작년 동월** 자동 계산 (예: 현재 2026-07 → `202507`)

#### 내부 호출 API

| 순서 | 외부 API               | 오퍼레이션               | 파라미터                                                        | 목적                             |
|----|----------------------|---------------------|-------------------------------------------------------------|--------------------------------|
| 1  | AreaTarResDemService | `areaTarSvcDemList` | baseYm=작년동월, areaCd=44, tarSvcDemIxCd=11                    | 충남 시군구별 **관광 서비스 수요 전체 지표** 조회 |
| 2  | KorService2          | `areaBasedList2`    | lDongRegnCd=44, lDongSignguCd=상위시군구, arrange=Q, numOfRows=1 | 상위 시군구별 콘텐츠 조회                 |

#### 정렬 기준

`tarSvcDemIxVal` (관광 서비스 수요 지표값) 내림차순 → 상위 N개 시군구 추출

---

## 내부 호출 API 전체 목록

| 서비스          | URL                                                                      | 설명        |
|--------------|--------------------------------------------------------------------------|-----------|
| 지역별 관광 수요 강도 | `https://apis.data.go.kr/B551011/AreaTarDemDsService/areaTarExpDsList`   | 소비 강도     |
| 지역별 관광 수요 강도 | `https://apis.data.go.kr/B551011/AreaTarDemDsService/areaTarSjrnDsList`  | 체류 강도     |
| 지역별 관광 자원 수요 | `https://apis.data.go.kr/B551011/AreaTarResDemService/areaTarSvcDemList` | 서비스 수요    |
| 국문 관광정보 서비스  | `https://apis.data.go.kr/B551011/KorService2/areaBasedList2`             | 관광 콘텐츠 목록 |

---

## 시군구코드 변환

수요강도 API의 `signguCd` (5자리: `44131`) → KorService2의 `lDongSignguCd` (3자리: `131`)

```
"44131" → substring(2) → "131"
"44230" → substring(2) → "230"
```

---

## 에러 응답

| 상황           | HTTP Status | 설명                        |
|--------------|-------------|---------------------------|
| 외부 API 호출 실패 | 200 (빈 배열)  | 외부 API 실패 시 빈 리스트 `[]` 반환 |
| 데이터 없음       | 200 (빈 배열)  | 해당 기간 데이터 미존재 시 `[]`      |

---

## 요청 예시

```http
### 인기순 (기본)
GET /api/v1/tourism/contents

### 인기순 10개
GET /api/v1/tourism/contents?type=POPULAR&numOfRows=10

### 시즌 추천 (작년 7월 기준)
GET /api/v1/tourism/contents?type=SEASONAL&baseYm=202507

### 시즌 추천 (자동 계산)
GET /api/v1/tourism/contents?type=SEASONAL&numOfRows=3
```