## Wordiga Backend API 명세

이 문서는 현재 `main` 브랜치에 구현된 소비자 API 계약을 기준으로 합니다. 구현 예정 기능은 포함하지 않습니다.

### 전체 API Path

| Method | Path | Auth | 설명 |
|---|---|---|---|
| POST | `/api/v1/auth/refresh` | Refresh Cookie | Access Token 재발급 |
| DELETE | `/api/v1/auth/session` | X | Refresh Cookie 삭제 |
| GET | `/api/v1/tourism/contents` | X | 관광 콘텐츠 목록 조회 |
| GET | `/api/v1/tourism/contents/sigungu` | X | 충청남도 시군구 목록 조회 |
| GET | `/api/v1/tourism/contents/{contentId}` | X | 관광 콘텐츠 통합 상세 조회 |
| POST | `/api/v1/wishes` | O | 위시 등록 및 지역 자동 분류 |
| DELETE | `/api/v1/wishes` | O | 위시 삭제 |
| GET | `/api/v1/wishes` | O | 위시 지역 폴더 조회 |
| GET | `/api/v1/wishes/{folderName}` | O | 폴더별 위시 목록 조회 |
| POST | `/api/v1/plans/generate` | O | AI 일정 생성 및 저장 |
| GET | `/api/v1/plans` | O | 내 일정 목록 조회 |
| GET | `/api/v1/plans/{planId}` | O | 내 일정 상세 조회 |
| PATCH | `/api/v1/plans/{planId}` | O | 내 일정 기본정보 수정 |
| PUT | `/api/v1/plans/{planId}/contents` | O | 내 일정 콘텐츠 수정 |
| DELETE | `/api/v1/plans/{planId}` | O | 내 일정 삭제 |
| POST | `/api/v1/plans/{planId}/proposals` | O | AI 제안서 DOCX 생성 |
| GET | `/api/v1/plans/{planId}/proposals` | O | 일정별 제안서 목록 조회 |
| DELETE | `/api/v1/plans/{planId}/proposals/{proposalId}` | O | 제안서 영구 삭제 |
| GET | `/api/v1/me` | O | 내 소셜 프로필 조회 |
| DELETE | `/api/v1/me` | O | 소셜 연결 해제 및 회원탈퇴 |

### 공통 인증

- 인증이 필요한 API는 HTTP `Authorization` 헤더에 `Bearer {accessToken}`을 전달합니다.
- 비회원 위시리스트 저장은 제공하지 않습니다.
- Access Token 유효기간은 1시간입니다.
- OAuth 로그인 성공 시 30일 유효한 Wordiga Refresh Token을 `Secure`, `HttpOnly`, `SameSite=Lax` 쿠키로 함께 발급합니다.
- Refresh Token은 Google/Kakao 토큰이 아니라 Wordiga가 자체 발급하며 JavaScript 응답에 노출하지 않습니다.

### 공통 오류 응답

```json
{
  "code": "INVALID_REQUEST",
  "message": "요청값을 확인해 주세요.",
  "fieldErrors": [
    {
      "field": "startDate",
      "reason": "오늘 이후의 날짜를 입력해 주세요."
    }
  ]
}
```

## 1. 관광 콘텐츠 목록 조회

```http
GET /api/v1/tourism/contents
```

관련 기능:

* 인기·계절·연관 관광지·위시 기반 개인화 콘텐츠를 조회합니다.
* 검색어와 지역·관광타입 필터를 적용합니다.

인증: 불필요

### Query Parameter

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `type` | Enum | N | `POPULAR`, `SEASONAL`, `RELATED`, `PERSONALIZED`; 기본값은 `POPULAR` | `POPULAR` |
| `visitDate` | LocalDate | N | 계절 정렬 기준일, 미입력 시 오늘 | `2026-08-20` |
| `keyword` | String | N | 콘텐츠명 검색어 | `공주` |
| `contentTypeId` | String | N | 관광타입 ID | `12` |
| `lDongSignguCd` | String | N | 충청남도 법정동 시군구 코드 | `200` |
| `referenceContentId` | String | C | `type=RELATED`일 때 필수인 기준 관광 콘텐츠 ID | `2717354` |
| `capacitySatisfied` | Boolean | N | `true`이면 참가 인원을 수용할 수 있는 숙박만 반환 | `true` |
| `participantCount` | Integer | N | 숙박 수용 가능 여부 계산 인원, 기본값은 `10` | `25` |
| `ageGroups` | Enum[] | N | `20S`, `30S`, `40S`, `50S_PLUS`만 허용 | `30S,40S` |
| `page` | Integer | N | 0부터 시작하는 페이지 기본값은 `0` | `0` |
| `size` | Integer | N | 페이지 크기 기본값은 `20`, 최댓값은 `50` | `20` |

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| - | - | - | 요청 본문 없음 |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `items` | ContentSummary[] | Y | 관광 콘텐츠 목록 |
| `items[].contentId` | String | Y | 관광 콘텐츠 ID |
| `items[].contentTypeId` | String | Y | 관광타입 ID |
| `items[].title` | String | Y | 콘텐츠명 |
| `items[].addr1` | String | N | 주소 |
| `items[].lDongSignguCd` | String | N | 법정동 시군구 코드 |
| `items[].mapx` | BigDecimal | N | 경도 |
| `items[].mapy` | BigDecimal | N | 위도 |
| `items[].firstImage` | String | N | 대표 이미지 URL |
| `items[].theme` | CodeName | N | 화면 대분류 테마. 정책에 없는 분류는 `null` |
| `items[].theme.code` | String | Y | `ATTRACTION_EXPERIENCE`, `LODGING`, `FOOD_CAFE` 중 하나 |
| `items[].theme.name` | String | Y | `관광지/체험`, `숙소`, `맛집/카페` 중 하나 |
| `items[].category` | CodeName | N | 신분류체계 기반 세부 카테고리. 정책에 없는 분류는 `null` |
| `items[].category.code` | String | Y | 필터와 선택 상태에 사용하는 분류 코드 |
| `items[].category.name` | String | Y | 화면 표시용 분류명 |
| `items[].recommendationScore` | BigDecimal | Y | 정렬에 사용한 개인화 점수 |
| `items[].satisfaction` | Satisfaction | N | 로그인 회원 기준 만족도. 원천 데이터가 모두 없으면 `null` |
| `items[].isWished` | Boolean | Y | 현재 로그인 회원의 위시 등록 여부이며 비회원은 `false` |
| `page` | Integer | Y | 현재 페이지 |
| `size` | Integer | Y | 페이지 크기 |
| `hasNext` | Boolean | Y | 다음 페이지 존재 여부 |

### Response Example

```json
{
  "items": [
    {
      "contentId": "126508",
      "contentTypeId": "12",
      "title": "현충사",
      "addr1": "충청남도 아산시 염치읍 현충사길 126",
      "lDongSignguCd": "200",
      "mapx": 126.9891281,
      "mapy": 36.8051452,
      "firstImage": "https://example.com/main.jpg",
      "theme": {"code": "ATTRACTION_EXPERIENCE", "name": "관광지/체험"},
      "category": {"code": "HS", "name": "역사관광"},
      "recommendationScore": 88.3,
      "satisfaction": null,
      "isWished": true
    }
  ],
  "page": 0,
  "size": 20,
  "hasNext": false
}
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `INVALID_REQUEST` | 요청값 오류 | 400 | 검색 조건을 확인해 주세요. |
| `UNAUTHORIZED` | 인증 실패 | 401 | `PERSONALIZED` 조회는 로그인이 필요합니다. |
| `TOURISM_API_UNAVAILABLE` | 관광 API 장애 | 503 | 잠시 후 다시 조회해 주세요. |

### Validation

- `size`는 1~50이어야 합니다.
- `participantCount`는 1~50이어야 합니다.
- `PERSONALIZED`는 로그인이 필요합니다.
- `RELATED`는 `referenceContentId`가 필요합니다.

### 동작 및 정렬 기준

- 검색어가 있으면 한국관광공사 `searchKeyword2`, 없으면 `areaBasedList2`로 후보를 조회합니다.
- `POPULAR` 기본 점수는 소비 강도 60%, 체류 강도 40%로 계산합니다.
- `SEASONAL` 기본 점수는 방문 예정 월의 관광 서비스 수요를 사용합니다.
- `PERSONALIZED`는 위시에 저장된 상위 3개 시군구와 관광 분류 테마를 후보 조건으로 사용하고, 활동로그는 수집하지 않습니다. 만족도는 조회 시 계산하며 저장하지 않습니다.
- `RELATED`는 기준 콘텐츠명과 지역으로 한국관광공사 연관 관광지를 조회한 뒤 충청남도 콘텐츠 ID로 매칭하며, 기준 콘텐츠와 중복 결과를 제외하고 원천 순위를 유지합니다.
- 수용 가능 숙소는 객실별 최대 인원·객실 수 또는 전체 수용 인원을 참가 인원과 비교합니다. 원천 수용 정보가 없으면 만족으로 간주하지 않습니다.
- `POPULAR`, `SEASONAL`은 원천 API 순위를 유지하고 `PERSONALIZED`는 `recommendationScore DESC`로 정렬합니다.
- 별도의 추천 API와 검색 API는 만들지 않습니다.
- 로그인 회원은 각 콘텐츠의 위시 등록 여부를 `isWished`로 반환하며 비회원은 `false`로 반환합니다.

### 관광 테마·카테고리 목록 조회

```http
GET /api/v1/tourism/categories
```

관련 기능: 일정 설계 화면의 테마 탭과 세부 카테고리 필터 옵션 조회

인증: 불필요

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `[].theme` | CodeName | Y | 화면 대분류 테마 |
| `[].theme.code` | String | Y | API 전용 고정 테마 코드 |
| `[].theme.name` | String | Y | 화면 표시용 테마명 |
| `[].categories` | CodeName[] | Y | 해당 테마의 세부 카테고리 |

```json
[
  {
    "theme": {"code": "ATTRACTION_EXPERIENCE", "name": "관광지/체험"},
    "categories": [
      {"code": "EV", "name": "축제/공연/행사"},
      {"code": "EX", "name": "체험관광"},
      {"code": "HS", "name": "역사관광"},
      {"code": "LS", "name": "레저스포츠"},
      {"code": "NA", "name": "자연관광"},
      {"code": "SH", "name": "쇼핑"},
      {"code": "VE", "name": "문화관광"}
    ]
  },
  {
    "theme": {"code": "LODGING", "name": "숙소"},
    "categories": [
      {"code": "AC01", "name": "호텔"},
      {"code": "AC02", "name": "콘도미니엄"},
      {"code": "AC03", "name": "펜션/민박"},
      {"code": "AC04", "name": "모텔"},
      {"code": "AC05", "name": "캠핑"},
      {"code": "AC06", "name": "호스텔"}
    ]
  },
  {
    "theme": {"code": "FOOD_CAFE", "name": "맛집/카페"},
    "categories": [
      {"code": "FD01", "name": "한식"},
      {"code": "FD020100", "name": "중식"},
      {"code": "FD020200", "name": "일식"},
      {"code": "FD020300", "name": "양식"},
      {"code": "FD020400", "name": "기타외국식"},
      {"code": "FD020500", "name": "퓨전음식"},
      {"code": "FD03", "name": "간이음식"},
      {"code": "FD04", "name": "주점"},
      {"code": "FD05", "name": "카페"}
    ]
  }
]
```

### 충청남도 시군구 목록 조회

```http
GET /api/v1/tourism/contents/sigungu
```

관련 기능: 관광 콘텐츠 지역 필터용 충청남도 법정동 시군구 코드 조회

인증: 불필요

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| - | - | - | 요청 본문 없음 |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `[].code` | String | Y | 법정동 시군구 코드 |
| `[].name` | String | Y | 시군구명 |

천안시 동남구·서북구를 구분한 16개 항목을 코드 오름차순으로 반환합니다.

## 2. 관광 콘텐츠 통합 상세 조회

```http
GET /api/v1/tourism/contents/{contentId}
```

관련 기능:

* 관광 콘텐츠 하나의 공통정보, 관광타입별 소개정보, 반복정보, 이미지, 만족도, 계절 사진을 조회합니다.

인증: 불필요

### Path Parameter

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `contentId` | String | Y | 한국관광공사 관광 콘텐츠 ID | `126508` |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `visitDate` | LocalDate | N | 쾌적도와 월 기후 판단 기준일, 미입력 시 오늘 | `2026-08-20` |
| `participantCount` | Integer | N | 숙박 수용 가능 여부 계산 인원, 기본값은 `10` | `8` |
| `ageGroups` | Enum[] | N | `20S`, `30S`, `40S`, `50S_PLUS`만 허용 | `20S,30S` |
| `stayDays` | Integer | N | 체류 일수 `1`~`3`; 체류 적합도 계산 시 숙박 일수 `stayDays-1`로 변환 | `2` |

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| - | - | - | 요청 본문 없음 |

### Response Body - 최상위

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `common` | CommonDetail | Y | 모든 관광타입의 공통정보 |
| `intro` | IntroDetail | Y | `contentTypeId`별 소개정보 전체 필드 |
| `details` | DetailInfo[] | Y | 타입별 반복정보, 숙박 객실·여행코스 항목 |
| `images` | DetailImage[] | Y | 상세 이미지와 저작권 정보 |
| `seasonalImages` | SeasonalImage[] | Y | 촬영일을 기준으로 계절을 의미화한 사진 |
| `satisfaction` | Satisfaction | N | 관광 수요 기반 예상 만족도 |
| `capacitySatisfied` | Boolean | N | 숙박 콘텐츠가 참가 인원을 수용할 수 있는지 여부이며 원천 수용인원이 없으면 `null` |
| `monthlyWeather` | MonthlyWeather | N | 방문 월의 최근 5개년 ASOS 동일 월 관측값으로 계산한 기후 예상치 |
| `monthlyWeather.targetMonth` | Integer | Y | 대상 월 1~12 |
| `monthlyWeather.avgTemp` | BigDecimal | N | ASOS 일평균기온의 5개년 동일 월 평균 |
| `monthlyWeather.monthlyPrecipitation` | BigDecimal | N | 연도별 동일 월 누적강수량의 5개년 평균 |
| `monthlyWeather.historicalYears` | Integer | Y | 산출에 사용한 연도 수, 기본 5 |
| `monthlyWeather.stationName` | String | N | 시군구에 매핑한 인근 ASOS 관측소명 |
| `monthlyWeather.basis` | String | Y | `ASOS_HISTORICAL_MONTHLY_AVERAGE` |

### Response Body - CommonDetail

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `contentId` | String | Y | 콘텐츠 ID |
| `contentTypeId` | String | Y | 관광타입 ID |
| `title` | String | Y | 콘텐츠명 |
| `createdTime` | String | N | 한국관광공사 등록 시각 |
| `modifiedTime` | String | N | 한국관광공사 수정 시각 |
| `tel` | String | N | 전화번호 |
| `telName` | String | N | 전화번호 명칭 |
| `homepage` | String | N | 홈페이지 |
| `firstImage` | String | N | 대표 이미지 |
| `firstImage2` | String | N | 대표 썸네일 |
| `copyrightTypeCode` | String | N | 이미지 저작권 유형 |
| `addr1` | String | N | 주소 |
| `addr2` | String | N | 상세 주소 |
| `zipcode` | String | N | 우편번호 |
| `mapx` | BigDecimal | N | 경도 |
| `mapy` | BigDecimal | N | 위도 |
| `mapLevel` | String | N | 지도 레벨 |
| `overview` | String | N | 콘텐츠 개요 |
| `lDongRegnCd` | String | N | 법정동 시도 코드 |
| `lDongSignguCd` | String | N | 법정동 시군구 코드 |
| `lclsSystm1` | String | N | 분류체계 대분류 |
| `lclsSystm2` | String | N | 분류체계 중분류 |
| `lclsSystm3` | String | N | 분류체계 소분류 |

### Response Body - IntroDetail 관광지 `contentTypeId=12`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `accomCount` | String | N | 수용 인원 |
| `checkBabyCarriage` | String | N | 유모차 대여 가능 여부 |
| `checkCreditCard` | String | N | 신용카드 사용 가능 여부 |
| `checkPet` | String | N | 반려동물 동반 가능 여부 |
| `experienceAgeRange` | String | N | 체험 가능 연령 |
| `experienceGuide` | String | N | 체험 안내 |
| `heritage1` | String | N | 세계문화유산 여부 |
| `heritage2` | String | N | 세계자연유산 여부 |
| `heritage3` | String | N | 세계기록유산 여부 |
| `infoCenter` | String | N | 문의 및 안내 정보 |
| `openDate` | String | N | 개장일 |
| `parking` | String | N | 주차시설 정보 |
| `restDate` | String | N | 쉬는 날 |
| `useSeason` | String | N | 이용 가능한 시기 |
| `useTime` | String | N | 이용시간 |

### Response Body - IntroDetail 문화시설 `contentTypeId=14`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `accomCount` | String | N | 수용 인원 |
| `checkBabyCarriage` | String | N | 유모차 대여 가능 여부 |
| `checkCreditCard` | String | N | 신용카드 사용 가능 여부 |
| `checkPet` | String | N | 반려동물 동반 가능 여부 |
| `discountInfo` | String | N | 할인 정보 |
| `infoCenter` | String | N | 문의 및 안내 정보 |
| `parking` | String | N | 주차시설 정보 |
| `parkingFee` | String | N | 주차요금 |
| `restDate` | String | N | 쉬는 날 |
| `useFee` | String | N | 이용요금 |
| `useTime` | String | N | 이용시간 |
| `scale` | String | N | 시설 규모 |
| `spendTime` | String | N | 관람 소요시간 |

### Response Body - IntroDetail 행사·공연·축제 `contentTypeId=15`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `ageLimit` | String | N | 관람 가능 연령 |
| `bookingPlace` | String | N | 예매처 |
| `discountInfo` | String | N | 할인 정보 |
| `eventEndDate` | LocalDate | N | 행사 종료일 |
| `eventHomepage` | String | N | 행사 홈페이지 |
| `eventPlace` | String | N | 행사 장소 |
| `eventStartDate` | LocalDate | N | 행사 시작일 |
| `festivalGrade` | String | N | 축제 등급 |
| `placeInfo` | String | N | 행사장 위치 안내 |
| `playTime` | String | N | 공연시간 |
| `program` | String | N | 행사 프로그램 |
| `spendTime` | String | N | 관람 소요시간 |
| `sponsor1` | String | N | 주최자 정보 |
| `sponsor1Tel` | String | N | 주최자 연락처 |
| `sponsor2` | String | N | 주관사 정보 |
| `sponsor2Tel` | String | N | 주관사 연락처 |
| `subEvent` | String | N | 부대행사 정보 |
| `useTime` | String | N | 이용요금 |

### Response Body - IntroDetail 여행코스 `contentTypeId=25`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `distance` | String | N | 코스 총거리 |
| `infoCenter` | String | N | 문의 및 안내 정보 |
| `schedule` | String | N | 코스 일정 |
| `takeTime` | String | N | 코스 총 소요시간 |
| `theme` | String | N | 코스 테마 |

### Response Body - IntroDetail 레포츠 `contentTypeId=28`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `accomCount` | String | N | 수용 인원 |
| `checkBabyCarriage` | String | N | 유모차 대여 가능 여부 |
| `checkCreditCard` | String | N | 신용카드 사용 가능 여부 |
| `checkPet` | String | N | 반려동물 동반 가능 여부 |
| `experienceAgeRange` | String | N | 체험 가능 연령 |
| `infoCenter` | String | N | 문의 및 안내 정보 |
| `openPeriod` | String | N | 개장 기간 |
| `parkingFee` | String | N | 주차요금 |
| `parking` | String | N | 주차시설 정보 |
| `reservation` | String | N | 예약 안내 |
| `restDate` | String | N | 쉬는 날 |
| `scale` | String | N | 시설 규모 |
| `useFee` | String | N | 입장료 |
| `useTime` | String | N | 이용시간 |

### Response Body - IntroDetail 숙박 `contentTypeId=32`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `accomCount` | String | N | 수용 가능 인원 |
| `checkInTime` | String | N | 입실시간 |
| `checkOutTime` | String | N | 퇴실시간 |
| `checkCooking` | String | N | 객실 내 취사 가능 여부 |
| `foodPlace` | String | N | 식음료장 정보 |
| `infoCenter` | String | N | 문의 및 안내 정보 |
| `parking` | String | N | 주차시설 정보 |
| `pickup` | String | N | 픽업 서비스 정보 |
| `roomCount` | String | N | 객실 수 |
| `reservation` | String | N | 예약 안내 |
| `reservationUrl` | String | N | 예약 홈페이지 |
| `roomType` | String | N | 객실 유형 |
| `scale` | String | N | 숙박시설 규모 |
| `subFacility` | String | N | 기타 부대시설 |
| `barbecue` | String | N | 바비큐장 여부 |
| `beauty` | String | N | 뷰티시설 여부 |
| `beverage` | String | N | 식음료장 여부 |
| `bicycle` | String | N | 자전거 대여 여부 |
| `campfire` | String | N | 캠프파이어 가능 여부 |
| `fitness` | String | N | 피트니스센터 여부 |
| `karaoke` | String | N | 노래방 여부 |
| `publicBath` | String | N | 공용 샤워실 여부 |
| `publicPc` | String | N | 공용 PC실 여부 |
| `sauna` | String | N | 사우나실 여부 |
| `seminar` | String | N | 세미나실 여부 |
| `sports` | String | N | 스포츠시설 여부 |
| `refundRegulation` | String | N | 환불 규정 |

### Response Body - IntroDetail 쇼핑 `contentTypeId=38`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `checkBabyCarriage` | String | N | 유모차 대여 가능 여부 |
| `checkCreditCard` | String | N | 신용카드 사용 가능 여부 |
| `checkPet` | String | N | 반려동물 동반 가능 여부 |
| `cultureCenter` | String | N | 문화센터 정보 |
| `fairDay` | String | N | 장이 서는 날 |
| `infoCenter` | String | N | 문의 및 안내 정보 |
| `openDate` | String | N | 개장일 |
| `openTime` | String | N | 영업시간 |
| `parking` | String | N | 주차시설 정보 |
| `restDate` | String | N | 쉬는 날 |
| `restroom` | String | N | 화장실 정보 |
| `saleItem` | String | N | 판매 품목 |
| `saleItemCost` | String | N | 판매 품목별 가격 |
| `scale` | String | N | 매장 규모 |
| `shopGuide` | String | N | 매장 안내 |

### Response Body - IntroDetail 음식점 `contentTypeId=39`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `checkCreditCard` | String | N | 신용카드 사용 가능 여부 |
| `discountInfo` | String | N | 할인 정보 |
| `firstMenu` | String | N | 대표 메뉴 |
| `infoCenter` | String | N | 문의 및 안내 정보 |
| `kidsFacility` | String | N | 어린이 놀이방 여부 |
| `openDate` | String | N | 개업일 |
| `openTime` | String | N | 영업시간 |
| `packing` | String | N | 포장 가능 여부 |
| `parking` | String | N | 주차시설 정보 |
| `reservation` | String | N | 예약 안내 |
| `restDate` | String | N | 쉬는 날 |
| `scale` | String | N | 음식점 규모 |
| `seat` | String | N | 좌석 수 |
| `smoking` | String | N | 금연·흡연 여부 |
| `treatMenu` | String | N | 취급 메뉴 |
| `licenseNumber` | String | N | 인허가 번호 |

### Response Body - DetailInfo

일반 관광타입:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `serialNumber` | Integer | N | 반복정보 순서 |
| `infoName` | String | N | 반복정보 제목 |
| `infoText` | String | N | 반복정보 내용 |
| `fieldType` | String | N | 반복정보 유형 구분값 |

여행코스:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `subContentId` | String | N | 하위 콘텐츠 ID |
| `subDetailAlt` | String | N | 코스 이미지 설명 |
| `subDetailImage` | String | N | 코스 이미지 URL |
| `subDetailOverview` | String | N | 코스 개요 |
| `subName` | String | N | 코스명 |
| `subNumber` | Integer | N | 코스 순서 |

숙박 객실:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `roomCode` | String | N | 객실 코드 |
| `roomTitle` | String | N | 객실명 |
| `roomSizePyeong` | BigDecimal | N | 객실 크기(평) |
| `roomCount` | Integer | N | 객실 수 |
| `roomBaseCount` | Integer | N | 기준 인원 |
| `roomMaxCount` | Integer | N | 최대 인원 |
| `roomOffSeasonWeekdayMinFee` | Long | N | 비수기 주중 최소 요금 |
| `roomOffSeasonWeekendMinFee` | Long | N | 비수기 주말 최소 요금 |
| `roomPeakSeasonWeekdayMinFee` | Long | N | 성수기 주중 최소 요금 |
| `roomPeakSeasonWeekendMinFee` | Long | N | 성수기 주말 최소 요금 |
| `roomIntro` | String | N | 객실 소개 |
| `roomBathFacility` | String | N | 목욕시설 여부 |
| `roomBath` | String | N | 욕조 여부 |
| `roomHomeTheater` | String | N | 홈시어터 여부 |
| `roomAirCondition` | String | N | 에어컨 여부 |
| `roomTv` | String | N | TV 여부 |
| `roomPc` | String | N | PC 여부 |
| `roomCable` | String | N | 케이블 설치 여부 |
| `roomInternet` | String | N | 인터넷 가능 여부 |
| `roomRefrigerator` | String | N | 냉장고 여부 |
| `roomToiletries` | String | N | 세면도구 여부 |
| `roomSofa` | String | N | 소파 여부 |
| `roomCook` | String | N | 취사용품 여부 |
| `roomTable` | String | N | 테이블 여부 |
| `roomHairDryer` | String | N | 헤어드라이어 여부 |
| `roomSizeSquareMeters` | BigDecimal | N | 객실 크기(제곱미터) |
| `roomImages` | RoomImage[] | Y | 객실 이미지 목록 이미지가 없으면 빈 배열 |

객실 이미지 `RoomImage`:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `imageUrl` | String | Y | 객실 이미지 URL |
| `alt` | String | N | 객실 이미지 설명 |
| `copyrightTypeCode` | String | N | 이미지 저작권 유형 |

### Response Body - 계절 이미지 및 만족도

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `seasonalImages[].imageUrl` | String | Y | 사진 URL |
| `seasonalImages[].shootingDate` | LocalDate | N | 실제 촬영일 |
| `seasonalImages[].season` | Enum | Y | `SPRING`, `SUMMER`, `AUTUMN`, `WINTER`, `UNKNOWN` |
| `seasonalImages[].matchConfidence` | BigDecimal | Y | 장소 일치 신뢰도 |
| `satisfaction.totalScore` | BigDecimal | Y | 최종 만족도 |
| `satisfaction.popularityScore` | ScoreComponent | Y | 인기도 점수 |
| `satisfaction.ageFitScore` | ScoreComponent | Y | 연령 적합도 |
| `satisfaction.stayFitScore` | ScoreComponent | Y | 체류 적합도 |
| `satisfaction.comfortScore` | ScoreComponent | Y | 쾌적도 점수 |
| `satisfaction.calculatedAt` | OffsetDateTime | Y | 만족도 계산 시각 |
| `satisfaction.*Score.score` | BigDecimal | Y | 구성요소 점수 |
| `satisfaction.*Score.weight` | BigDecimal | Y | 구성요소 가중치 |
| `satisfaction.*Score.imputed` | Boolean | Y | 원천 데이터 누락으로 중립값을 적용했는지 여부 |
| `satisfaction.*Score.reason` | String | N | 중립값 적용 사유 |
| `satisfaction.*Score.source` | String | N | 점수 산출에 사용한 관광공사 operation |

### Response Example

```json
{
  "common": {
    "contentId": "126508",
    "contentTypeId": "32",
    "title": "아산 온천 호텔",
    "createdTime": "20250101090000",
    "modifiedTime": "20260701090000",
    "tel": "041-000-0000",
    "telName": "예약실",
    "homepage": "https://example.com",
    "firstImage": "https://example.com/main.jpg",
    "firstImage2": "https://example.com/thumb.jpg",
    "copyrightTypeCode": "Type1",
    "addr1": "충청남도 아산시 온천동",
    "addr2": "1",
    "zipcode": "31500",
    "mapx": 126.9891281,
    "mapy": 36.8051452,
    "mapLevel": "6",
    "overview": "숙박시설 소개입니다.",
    "lDongRegnCd": "44",
    "lDongSignguCd": "200",
    "lclsSystm1": "AC",
    "lclsSystm2": "AC01",
    "lclsSystm3": null
  },
  "intro": {
    "accomCount": "100명",
    "checkInTime": "15:00",
    "checkOutTime": "11:00",
    "checkCooking": "불가",
    "foodPlace": "레스토랑",
    "infoCenter": "041-000-0000",
    "parking": "가능",
    "pickup": "불가",
    "roomCount": "30",
    "reservation": "전화 예약",
    "reservationUrl": "https://example.com/reservation",
    "roomType": "더블, 트윈",
    "scale": "지상 5층",
    "subFacility": "세미나실",
    "barbecue": "없음",
    "beauty": "없음",
    "beverage": "있음",
    "bicycle": "없음",
    "campfire": "없음",
    "fitness": "있음",
    "karaoke": "없음",
    "publicBath": "있음",
    "publicPc": "없음",
    "sauna": "있음",
    "seminar": "있음",
    "sports": "있음",
    "refundRegulation": "예약일 기준 환불 규정이 적용됩니다."
  },
  "details": [
    {
      "roomCode": "A01",
      "roomTitle": "스탠다드 더블",
      "roomSizePyeong": "8",
      "roomCount": "10",
      "roomBaseCount": "2",
      "roomMaxCount": "2",
      "roomOffSeasonWeekdayMinFee": "80000",
      "roomOffSeasonWeekendMinFee": "100000",
      "roomPeakSeasonWeekdayMinFee": "120000",
      "roomPeakSeasonWeekendMinFee": "150000",
      "roomIntro": "더블베드 객실입니다.",
      "roomBathFacility": "Y",
      "roomBath": "Y",
      "roomHomeTheater": "N",
      "roomAirCondition": "Y",
      "roomTv": "Y",
      "roomPc": "N",
      "roomCable": "Y",
      "roomInternet": "Y",
      "roomRefrigerator": "Y",
      "roomToiletries": "Y",
      "roomSofa": "N",
      "roomCook": "N",
      "roomTable": "Y",
      "roomHairDryer": "Y",
      "roomSizeSquareMeters": "26.4",
      "roomImages": []
    }
  ],
  "images": [],
  "seasonalImages": [],
  "satisfaction": {
    "totalScore": 84.7,
    "popularityScore": {
      "score": 91,
      "weight": 0.3,
      "imputed": false
    },
    "ageFitScore": {
      "score": 82,
      "weight": 0.35,
      "imputed": false
    },
    "stayFitScore": {
      "score": 50,
      "weight": 0.2,
      "imputed": true
    },
    "comfortScore": {
      "score": 88,
      "weight": 0.15,
      "imputed": false
    }
  }
}
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `INVALID_REQUEST` | 요청값 오류 | 400 | 상세 조회 조건을 확인해 주세요. |
| `CONTENT_NOT_FOUND` | 콘텐츠 없음 | 404 | 관광 콘텐츠를 찾을 수 없습니다. |
| `TOURISM_API_UNAVAILABLE` | 관광 API 장애 | 503 | 잠시 후 다시 조회해 주세요. |

### Validation

- `participantCount`는 1~50이어야 합니다.
- 숙박 콘텐츠는 객실별 최대 인원과 객실 수, 또는 전체 수용 인원으로 `capacitySatisfied`를 계산합니다.
- 충청남도 콘텐츠가 아니면 404를 반환합니다.

### 산출식 및 데이터 기준

```text
totalScore = popularityScore × 0.30
           + ageFitScore × 0.35
           + stayFitScore × 0.20
           + comfortScore × 0.15

comfortScore = 100 - concentrationRate
```

- `detailCommon2`, `detailIntro2`, `detailInfo2`, `detailImage2`를 호출하여 하나의 응답으로 조합합니다.
- 소개정보는 `contentTypeId`에 해당하는 모든 필드를 전달합니다.
- `contentTypeId`에 따라 해당 관광타입의 `IntroDetail`과 `DetailInfo` 구조를 반환합니다.
- 숙박 수용 가능 여부는 객실별 `roomMaxCount × roomCount`의 합을 우선 사용하고, 계산할 수 없으면 전체 수용 인원을 사용합니다.
- 숙박 객실과 여행코스 하위 콘텐츠는 `detailInfo2`의 전체 반복 결과를 전달합니다.
- 계절 사진은 대한민국 관광사진 API의 촬영일을 사용합니다.
- 촬영일이 없으면 `season=UNKNOWN`으로 제공합니다.
- 만족도 구성요소 하나가 누락되면 중립값 50과 `imputed=true`를 제공합니다. 모든 구성요소가 누락되면 `satisfaction=null`로 제공합니다.
- `ageGroups`를 보내지 않으면 연령 적합도는 원천값 없음으로 처리되어 중립값 50을 적용합니다.
- 상세 API는 숙박일 수를 입력받지 않으므로 체류 적합도는 전체 체류 유형 코드로 계산합니다.
- `participantCount`는 만족도 점수에 반영되지 않고 숙박 콘텐츠의 `capacitySatisfied`에만 반영됩니다.
- `visitDate`가 관광 집중도 30일 자료의 날짜와 정확히 일치하면 해당 날짜의 집중률을 사용합니다. 일치하지 않으면 조회된 30일 집중률 평균을 사용하며, 자료가 없으면 중립값 50을 적용합니다.

## 3. 위시 등록

```http
POST /api/v1/wishes
```

관련 기능:

* 회원이 콘텐츠 카드 또는 상세정보의 찜하기 버튼으로 위시를 등록합니다.
* 서버가 관광공사 상세정보를 조회하여 충청남도 시군구별로 자동 분류합니다.

인증: 필수

### Query Parameter

없습니다.

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `contentId` | String | Y | 저장할 관광 콘텐츠 ID |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `id` | Long | Y | 위시 ID |
| `contentId` | String | Y | 콘텐츠 ID |
| `contentTypeId` | String | N | 관광타입 ID |
| `title` | String | Y | 콘텐츠명 |
| `firstImage` | String | N | 대표 이미지 |
| `addr1` | String | N | 주소 |
| `lDongSignguCd` | String | N | 법정동 시군구 코드 |
| `sigunguName` | String | N | 시군구명 |
| `folderName` | String | Y | 자동 분류된 폴더명 |
| `createdAt` | LocalDateTime | Y | 등록 시각 |

### Response Example

```json
{
  "id": 41,
  "contentId": "126508",
  "contentTypeId": "12",
  "title": "현충사",
  "firstImage": "https://example.com/main.jpg",
  "addr1": "충청남도 아산시 염치읍 현충사길 126",
  "lDongSignguCd": "200",
  "sigunguName": "아산시",
  "folderName": "아산시",
  "createdAt": "2026-07-28T15:20:00"
}
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |
| `CONTENT_NOT_FOUND` | 콘텐츠 없음 | 404 | 관광 콘텐츠를 찾을 수 없습니다. |
| `TOURISM_API_UNAVAILABLE` | 관광 API 장애 | 503 | 잠시 후 다시 시도해 주세요. |

### Validation

- 동일 회원·콘텐츠 조합은 한 번만 저장합니다.
- 중복 요청이면 새 행을 만들지 않고 기존 위시를 반환합니다.
- 콘텐츠 정보는 요청에서 받지 않고 서버가 관광공사에서 조회합니다.

### 처리 기준

- 서버가 통합 관광 상세 서비스를 호출해 콘텐츠를 검증하고 저장용 스냅샷을 생성합니다.
- 시군구 코드는 천안시 동남구·서북구를 포함한 16개 코드로 폴더명을 결정합니다.
- 별도 폴더 엔티티가 없어 마지막 위시 삭제 시 빈 폴더도 조회 결과에서 사라집니다.

## 4. 위시 삭제

```http
DELETE /api/v1/wishes
```

관련 기능:

* 회원이 찜하기 버튼을 해제하여 위시를 삭제합니다.

인증: 필수

### Query Parameter

없습니다.

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `contentId` | String | Y | 삭제할 관광 콘텐츠 ID |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| - | - | - | 본문 없음 (`200 OK`) |

### Response Example

```http
HTTP/1.1 200 OK
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |

### Validation

- 회원 ID와 콘텐츠 ID가 모두 일치하는 위시만 삭제합니다.

### 처리 기준

- 회원·콘텐츠 ID 조건으로 삭제하며 대상이 없어도 `200 OK`를 반환합니다.

## 5. 위시 폴더 목록 조회

```http
GET /api/v1/wishes
```

관련 기능:

* 마이페이지 위시리스트 화면에 시군구별 폴더와 위시 수를 표시합니다.

인증: 필수

### Query Parameter

없습니다.

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| - | - | - | 요청 본문 없음 |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `folderName` | String | Y | 시군구명 또는 기본 위시리스트 |
| `lDongSignguCd` | String | N | 법정동 시군구 코드 |
| `count` | Long | Y | 폴더의 위시 수 |
| `thumbnailUrl` | String | N | 폴더에서 가장 최근에 저장한 위시의 이미지 |

### Response Example

```json
[
  {
    "folderName": "아산시",
    "lDongSignguCd": "200",
    "count": 3,
    "thumbnailUrl": "https://example.com/main.jpg"
  }
]
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |

### Validation

- 본인의 위시만 집계합니다.

### 정렬 기준

- `folderName ASC`로 정렬합니다.

## 6. 폴더별 위시 목록 조회

```http
GET /api/v1/wishes/{folderName}
```

관련 기능:

* 선택한 지역 폴더의 콘텐츠 카드와 지도 좌표를 표시합니다.

인증: 필수

### Path Parameter

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `folderName` | String | Y | 시군구명 또는 기본 위시리스트 | `아산시` |

### Query Parameter

없습니다.

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| - | - | - | 요청 본문 없음 |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `items[]` | Wish[] | Y | 위시 목록 |
| `items[].id` | Long | Y | 위시 ID |
| `items[].contentId` | String | Y | 콘텐츠 ID |
| `items[].contentTypeId` | String | N | 관광타입 ID |
| `items[].title` | String | Y | 콘텐츠명 |
| `items[].firstImage` | String | N | 대표 이미지 |
| `items[].addr1` | String | N | 주소 |
| `items[].lDongSignguCd` | String | N | 법정동 시군구 코드 |
| `items[].sigunguName` | String | N | 시군구명 |
| `items[].folderName` | String | Y | 자동 분류 폴더명 |
| `items[].mapx` | BigDecimal | N | 경도 |
| `items[].mapy` | BigDecimal | N | 위도 |
| `items[].createdAt` | LocalDateTime | Y | 등록 시각 |

### Response Example

```json
[
  {
    "id": 41,
    "contentId": "126508",
    "contentTypeId": "12",
    "title": "현충사",
    "firstImage": "https://example.com/main.jpg",
    "addr1": "충청남도 아산시 염치읍 현충사길 126",
    "lDongSignguCd": "200",
    "sigunguName": "아산시",
    "folderName": "아산시",
    "mapx": 126.9891281,
    "mapy": 36.8051452,
    "createdAt": "2026-07-28T15:20:00"
  }
]
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |

### Validation

- 본인의 위시만 조회합니다.

### 정렬 기준

- `createdAt DESC, id DESC`로 정렬합니다.
- 목록 항목은 위시 등록 시 저장한 관광정보 스냅샷이며 조회 시 관광공사 API를 다시 호출하지 않습니다.

## 7. AI 일정 생성 및 저장

```http
POST /api/v1/plans/generate
```

관련 기능:

* 상세정보의 단일 콘텐츠 또는 일정설계에서 선택한 최대 10개 콘텐츠로 AI 일정을 생성합니다.
* AI 서버 응답을 저장한 뒤 화면에 일자별 일정과 이동시간을 제공합니다.
* 동일 조건으로 다시 호출해도 기존 일정을 덮어쓰지 않고 새 `planId`와 AI가 발급한 새 `scheduleId`로 저장합니다.
* 제목을 생략하면 첫 번째 선택 콘텐츠의 시군구와 시작일로 `{시군구} {MMdd}`를 사용하며, 같은 회원에게 같은 제목이 있으면 ` 1`, ` 2` 순번을 붙입니다.

인증: 필수

### Query Parameter

없습니다.

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `title` | String | N | 일정 제목 |
| `startDate` | LocalDate | Y | 시작일 |
| `endDate` | LocalDate | Y | 종료일 |
| `participantCount` | Integer | Y | 참가 인원 |
| `visitMonth` | Integer | N | 방문 월(1~12), 미입력 시 `startDate`의 월 사용 |
| `stayDays` | Integer | Y | 체류 일수 `1`~`PLAN_MAX_STAY_DAYS`(기본 3); 시작일과 종료일을 포함한 기간과 일치해야 함 |
| `ageGroups` | Enum[] | N | `20S`, `30S`, `40S`, `50S_PLUS`만 허용 |
| `selectedContentIds` | String[] | Y | 선택 콘텐츠 ID, 배열 순서가 사용자 우선순위 |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `planId` | Long | Y | 저장된 일정 ID |
| `scheduleId` | Long | N | AI 서버 일정 식별자 |
| `title` | String | Y | 일정 제목 |
| `startDate` | LocalDate | Y | 시작일 |
| `endDate` | LocalDate | Y | 종료일 |
| `participantCount` | Integer | Y | 참가 인원 |
| `estimatedBudget` | EstimatedBudget | N | 예상 예산 |
| `estimatedBudget.totalAmount` | Long | N | 전체 예상 금액 |
| `estimatedBudget.perPersonAmount` | Long | N | 1인당 예상 금액 |
| `estimatedBudget.currency` | String | N | 통화 코드 |
| `estimatedBudget.breakdown` | Map<String, Long> | N | AI 서버가 제공한 항목별 예상 금액. 항목명과 금액을 구조화해 저장 |
| `days` | PlanDay[] | Y | 일자별 일정 |
| `days[].dayNumber` | Integer | Y | 일차 |
| `days[].date` | LocalDate | Y | 일정 날짜 |
| `days[].contents` | PlanContent[] | Y | 일정 콘텐츠 목록 |
| `days[].contents[].sequence` | Integer | Y | 일자 내 순서 |
| `days[].contents[].contentId` | String | Y | 콘텐츠 ID |
| `days[].contents[].title` | String | Y | 콘텐츠명 |
| `days[].contents[].contentTypeId` | String | N | 관광타입 ID |
| `days[].contents[].addr1` | String | N | 주소 |
| `days[].contents[].mapx` | BigDecimal | N | 경도 |
| `days[].contents[].mapy` | BigDecimal | N | 위도 |
| `days[].contents[].startTime` | LocalTime | N | 시작 시각 |
| `days[].contents[].endTime` | LocalTime | N | 종료 시각 |
| `days[].contents[].durationMinutes` | Integer | N | 체류시간(분) |
| `days[].contents[].travelTimeMinutes` | Integer | N | 이전 콘텐츠부터 이동시간(분) |
| `days[].contents[].travelDistanceMeters` | Integer | N | 이전 콘텐츠부터 이동거리(m) |
| `days[].contents[].estimatedCost` | Long | N | 예상 비용 |
| `days[].contents[].memo` | String | N | 일정 메모 |
| `createdAt` | LocalDateTime | Y | 저장 시각 |
| `updatedAt` | LocalDateTime | Y | 수정 시각 |

### Response Example

```json
{
  "planId": 77,
  "scheduleId": 1001,
  "title": "아산 역사 워크숍",
  "startDate": "2026-08-20",
  "endDate": "2026-08-21",
  "participantCount": 10,
  "estimatedBudget": {
    "totalAmount": 640000,
    "perPersonAmount": 80000,
    "currency": "KRW",
    "breakdown": {
      "accommodation": 300000,
      "food": 240000,
      "activity": 100000
    }
  },
  "days": [
    {
      "dayNumber": 1,
      "date": "2026-08-20",
      "contents": [
        {
          "sequence": 1,
          "contentId": "126508",
          "title": "현충사",
          "contentTypeId": "12",
          "addr1": "충청남도 아산시 염치읍 현충사길 126",
          "mapx": 126.9891281,
          "mapy": 36.8051452,
          "startTime": "10:00",
          "endTime": "11:30",
          "durationMinutes": 90,
          "travelTimeMinutes": null,
          "travelDistanceMeters": null,
          "estimatedCost": 0,
          "memo": null
        }
      ]
    }
  ],
  "createdAt": "2026-07-28T15:40:00",
  "updatedAt": "2026-07-28T15:40:00"
}
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `INVALID_REQUEST` | 요청값 오류 | 400 | 일정 생성 입력을 확인해 주세요. |
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |
| `CONTENT_NOT_FOUND` | 콘텐츠 없음 | 404 | 선택한 콘텐츠를 찾을 수 없습니다. |
| `AI_RESPONSE_INVALID` | AI 응답 오류 | 502 | AI 일정 응답 형식을 확인해 주세요. |
| `AI_UPSTREAM_ERROR` | AI 처리 오류 | 502 | AI 서버가 요청을 받았지만 일정 생성에 실패했습니다. |
| `AI_SERVER_UNAVAILABLE` | AI 서버 장애 | 503 | 잠시 후 다시 생성해 주세요. |
| `AI_SERVER_TIMEOUT` | AI 처리 시간 초과 | 504 | AI 서버 처리 제한 시간을 초과했습니다. |

### Validation

- 시작일은 종료일보다 늦을 수 없습니다.
- 일정 기간은 `PLAN_MAX_STAY_DAYS`(기본 3)까지입니다.
- 참가 인원은 10~50명입니다.
- `visitMonth`는 `startDate`의 월과 같아야 합니다.
- `stayDays`는 1~`PLAN_MAX_STAY_DAYS`(기본 3)이며 `endDate - startDate + 1`과 같아야 합니다.
- AI 요청 JSON만 snake_case를 사용합니다. 애플리케이션 요청·응답 DTO는 lowerCamelCase를 유지합니다.
- AI 요청에는 월 기후 예상치 `monthly_weather`, 콘텐츠별 `is_outdoor`, 보충 후보 `regional_contents`를 전달합니다. 체류 기간은 `num_days` 하나로 전달합니다.
- 분류 코드는 관광공사 `lclsSystmCode2` 변환 API의 한글 분류명으로 바꿔 `tags`에 전달합니다. 변환 실패 시 로그를 기록하고 AI 계약에 맞게 `tags: []`로 전달합니다.
- `selectedContentIds`는 중복 없이 1~10개입니다.
- AI 응답의 일차는 1부터 중복 없이 이어져야 합니다.
- AI 응답의 일수는 요청한 `stayDays`와 같아야 합니다.
- 일자별 `sequence`는 1부터 중복 없이 이어져야 합니다.
- 같은 날짜 안에서는 같은 콘텐츠를 중복 배치할 수 없고, 서로 다른 날짜에는 동일 숙소 등 같은 콘텐츠를 다시 배치할 수 있습니다.
- AI 응답에는 사용자가 선택한 `saved_contents`가 모두 포함되어야 하며, 전달된 `regional_contents`도 일정에 포함할 수 있습니다.

### AI 연동 계약

- AI 서버에는 `visit_month`, `num_people`, `num_days`, `saved_content_ids`, `saved_contents`,
  `regional_contents`, `age_groups`, `gender_ratio`, `preferences` snake_case 필드로 전달합니다.
- `saved_contents`는 콘텐츠 ID·제목·카테고리·주소·좌표·시군구·운영정보·분류 태그·연락처·개요·이미지·평균 체류시간·행사기간을 포함합니다.
- `regional_contents`는 선택 콘텐츠의 시군구에서 관광타입별 가까운 후보를 조회하고 상세정보로 보강해 최대 15개 전달합니다.
- 사용자가 선택한 `saved_contents`는 모두 유지하며, 동일 콘텐츠가 연관 후보에도 있으면 `regional_contents`에서만 제외합니다.
- 백엔드는 선택 콘텐츠의 타입별 상세정보, 좌표와 운영정보를 AI 서버에 HTTP POST로 전달합니다.
- 사용자 입력 예산은 받지 않습니다.
- AI 서버 호출은 DB 트랜잭션 밖에서 수행하고 연결 2초·응답 45초 타임아웃을 적용합니다.
- AI 응답의 날짜·순서·콘텐츠 ID를 검증한 뒤 일정과 예산 항목을 구조화된 테이블에 5초 제한의 단일 트랜잭션으로 저장합니다. AI 원문 JSON은 중복 저장하지 않습니다.
- 현재 AI 응답의 `memo`는 일정 콘텐츠에 저장하지 않으므로 응답에서 `null`입니다.

## 8. 내 일정 목록 조회

```http
GET /api/v1/plans
```

관련 기능:

* 마이페이지 내 일정 목록에 AI 생성과 저장이 완료된 일정만 표시합니다.

인증: 필수

### Query Parameter

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `page` | Integer | N | 0부터 시작하는 페이지 | `0` |
| `size` | Integer | N | 페이지 크기 기본값은 `20`, 최댓값은 `50` | `20` |
| `sort` | Enum | N | `LATEST`, `START_DATE_ASC`를 사용하며 기본값은 `LATEST` | `LATEST` |

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| - | - | - | 요청 본문 없음 |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `items` | PlanSummary[] | Y | 일정 목록 |
| `items[].planId` | Long | Y | 일정 ID |
| `items[].scheduleId` | Long | N | AI 서버 일정 식별자 |
| `items[].title` | String | Y | 일정 제목 |
| `items[].startDate` | LocalDate | Y | 시작일 |
| `items[].endDate` | LocalDate | Y | 종료일 |
| `items[].participantCount` | Integer | Y | 참가 인원 |
| `items[].thumbnailUrl` | String | N | 첫 일정 콘텐츠 대표 이미지 |
| `items[].contentCount` | Integer | Y | 일정 콘텐츠 수 |
| `items[].createdAt` | LocalDateTime | Y | 생성 시각 |
| `items[].updatedAt` | LocalDateTime | Y | 수정 시각 |
| `page` | Integer | Y | 현재 페이지 번호 |
| `size` | Integer | Y | 페이지 크기 |
| `hasNext` | Boolean | Y | 다음 페이지 존재 여부 |

### Response Example

```json
{
  "items": [
    {
      "planId": 77,
      "scheduleId": 1001,
      "title": "아산 역사 워크숍",
      "startDate": "2026-08-20",
      "endDate": "2026-08-21",
      "participantCount": 10,
      "thumbnailUrl": "https://example.com/main.jpg",
      "contentCount": 5,
      "createdAt": "2026-07-28T15:40:00",
      "updatedAt": "2026-07-28T15:40:00"
    }
  ],
  "page": 0,
  "size": 20,
  "hasNext": false
}
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `INVALID_REQUEST` | 요청값 오류 | 400 | 페이지 또는 정렬 조건을 확인해 주세요. |
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |

### Validation

- `size`는 1~50이어야 합니다.
- 본인 일정만 조회합니다.

### 정렬 기준

- `LATEST`: `createdAt DESC, planId DESC`입니다.
- `START_DATE_ASC`: `startDate ASC, planId ASC`입니다.

## 9. 내 일정 상세 조회

```http
GET /api/v1/plans/{planId}
```

관련 기능:

* 저장된 일정의 기본정보와 일자별 콘텐츠 전체를 조회합니다.

인증: 필수

### Path Parameter

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `planId` | Long | Y | 일정 ID | `77` |

### Query Parameter

없습니다.

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| - | - | - | 요청 본문 없음 |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `planId` | Long | Y | 일정 ID |
| `scheduleId` | Long | N | AI 서버 일정 식별자 |
| `title` | String | Y | 일정 제목 |
| `startDate` | LocalDate | Y | 시작일 |
| `endDate` | LocalDate | Y | 종료일 |
| `participantCount` | Integer | Y | 참가 인원 |
| `estimatedBudget` | EstimatedBudget | N | 예상 예산 |
| `days` | PlanDay[] | Y | 일자별 일정 |
| `days[].dayNumber` | Integer | Y | 일차 |
| `days[].date` | LocalDate | Y | 일정 날짜 |
| `days[].contents` | PlanContent[] | Y | 일정 콘텐츠 목록 |
| `days[].contents[].sequence` | Integer | Y | 일자 내 순서 |
| `days[].contents[].contentId` | String | Y | 콘텐츠 ID |
| `days[].contents[].title` | String | Y | 콘텐츠명 |
| `days[].contents[].contentTypeId` | String | N | 관광타입 ID |
| `days[].contents[].addr1` | String | N | 주소 |
| `days[].contents[].mapx` | BigDecimal | N | 경도 |
| `days[].contents[].mapy` | BigDecimal | N | 위도 |
| `days[].contents[].startTime` | LocalTime | N | 시작 시각 |
| `days[].contents[].endTime` | LocalTime | N | 종료 시각 |
| `days[].contents[].durationMinutes` | Integer | N | 체류시간(분) |
| `days[].contents[].travelTimeMinutes` | Integer | N | 이전 콘텐츠부터 이동시간(분) |
| `days[].contents[].travelDistanceMeters` | Integer | N | 이전 콘텐츠부터 이동거리(m) |
| `days[].contents[].estimatedCost` | Long | N | 예상 비용 |
| `days[].contents[].memo` | String | N | 일정 메모 |
| `createdAt` | LocalDateTime | Y | 생성 시각 |
| `updatedAt` | LocalDateTime | Y | 수정 시각 |

### Response Example

```json
{
  "planId": 77,
  "scheduleId": 1001,
  "title": "아산 역사 워크숍",
  "startDate": "2026-08-20",
  "endDate": "2026-08-21",
  "participantCount": 10,
  "estimatedBudget": null,
  "days": [
    {
      "dayNumber": 1,
      "date": "2026-08-20",
      "contents": [
        {
          "sequence": 1,
          "contentId": "126508",
          "title": "현충사",
          "contentTypeId": "12",
          "addr1": "충청남도 아산시 염치읍 현충사길 126",
          "mapx": 126.9891281,
          "mapy": 36.8051452,
          "startTime": "10:00",
          "endTime": "11:30",
          "durationMinutes": 90,
          "travelTimeMinutes": null,
          "travelDistanceMeters": null,
          "estimatedCost": 0,
          "memo": null
        }
      ]
    }
  ],
  "createdAt": "2026-07-28T15:40:00",
  "updatedAt": "2026-07-28T15:40:00"
}
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |
| `PLAN_NOT_FOUND` | 일정 없음 | 404 | 일정을 찾을 수 없습니다. |

### Validation

- 본인 소유 일정만 조회합니다.

### 정렬 기준

- 일자는 `date ASC`, 콘텐츠는 `sequence ASC`로 정렬합니다.

## 10. 내 일정 기본정보 수정

```http
PATCH /api/v1/plans/{planId}
```

관련 기능:

* 일정 제목과 참가 인원을 수정합니다.

인증: 필수

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `title` | String | N | 변경할 일정 제목 |
| `participantCount` | Integer | N | 변경할 참가 인원 |

### Response Body

응답 형식은 [9. 내 일정 상세 조회](#9-내-일정-상세-조회)의 `PlanDetailResponse`와 동일하며, 아래는 수정과 직접 관련된 주요 필드입니다.

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `planId` | Long | Y | 일정 ID |
| `title` | String | Y | 일정 제목 |
| `participantCount` | Integer | Y | 참가 인원 |
| `estimatedBudget` | EstimatedBudget | N | 예상 예산 |
| `updatedAt` | LocalDateTime | Y | 수정 시각 |

### Response Example

```json
{
  "planId": 77,
  "title": "아산 역사 워크숍 수정",
  "participantCount": 10,
  "estimatedBudget": {
    "totalAmount": 800000,
    "perPersonAmount": 80000,
    "currency": "KRW",
    "breakdown": {
      "accommodation": 300000,
      "food": 240000,
      "activity": 100000
    }
  },
  "updatedAt": "2026-07-28T16:00:00"
}
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `INVALID_REQUEST` | 요청값 오류 | 400 | 수정할 값을 확인해 주세요. |
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |
| `PLAN_NOT_FOUND` | 일정 없음 | 404 | 일정을 찾을 수 없습니다. |

### Validation

- 최소 한 필드를 전달해야 합니다.
- 제목은 공백 제거 후 1~100자여야 합니다.
- 참가 인원은 1~50명이어야 합니다.

## 11. 내 일정 콘텐츠 수정

```http
PUT /api/v1/plans/{planId}/contents
```

관련 기능:

* 일정에 포함할 콘텐츠를 교체하고 순서를 변경합니다.
* 요청 리스트 순서를 일정 콘텐츠 순서로 저장합니다.

인증: 필수

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `days` | PlanDayUpdate[] | Y | 일자별 콘텐츠 목록 |
| `days[].dayNumber` | Integer | Y | 1부터 시작하는 일차 |
| `days[].contentIds` | String[] | Y | 해당 일차의 콘텐츠 ID 목록 리스트 순서가 표시 순서 |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `planId` | Long | Y | 일정 ID |
| `scheduleId` | Long | N | AI 서버 일정 식별자 |
| `title` | String | Y | 일정 제목 |
| `startDate` | LocalDate | Y | 시작일 |
| `endDate` | LocalDate | Y | 종료일 |
| `participantCount` | Integer | Y | 참가 인원 |
| `estimatedBudget` | EstimatedBudget | N | 마지막 AI 생성 시 저장된 예상 예산이며 재생성 전까지 유지 |
| `days` | PlanDay[] | Y | 일자별 일정 |
| `days[].dayNumber` | Integer | Y | 일차 |
| `days[].date` | LocalDate | Y | 일정 날짜 |
| `days[].contents` | PlanContent[] | Y | 변경된 콘텐츠 목록 |
| `days[].contents[].sequence` | Integer | Y | 요청 배열 기준 순서 |
| `days[].contents[].contentId` | String | Y | 콘텐츠 ID |
| `days[].contents[].title` | String | Y | 콘텐츠명 |
| `days[].contents[].travelTimeMinutes` | Integer | N | AI 재계산 전 `null` |
| `createdAt` | LocalDateTime | Y | 생성 시각 |
| `updatedAt` | LocalDateTime | Y | 수정 시각 |

### Request Example

```json
{
  "days": [
    {
      "dayNumber": 1,
      "contentIds": [
        "126508",
        "2754012"
      ]
    },
    {
      "dayNumber": 2,
      "contentIds": [
        "3011445"
      ]
    }
  ]
}
```

### Response Example

```json
{
  "planId": 77,
  "title": "아산 역사 워크숍",
  "startDate": "2026-08-20",
  "endDate": "2026-08-21",
  "days": [
    {
      "dayNumber": 1,
      "date": "2026-08-20",
      "contents": [
        {
          "sequence": 1,
          "contentId": "126508",
          "title": "현충사",
          "travelTimeMinutes": null
        },
        {
          "sequence": 2,
          "contentId": "2754012",
          "title": "아산 외암마을",
          "travelTimeMinutes": null
        }
      ]
    },
    {
      "dayNumber": 2,
      "date": "2026-08-21",
      "contents": [
        {
          "sequence": 1,
          "contentId": "3011445",
          "title": "온양민속박물관",
          "travelTimeMinutes": null
        }
      ]
    }
  ],
  "updatedAt": "2026-07-28T16:10:00"
}
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `INVALID_REQUEST` | 요청값 오류 | 400 | 콘텐츠 목록을 확인해 주세요. |
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |
| `PLAN_NOT_FOUND` | 일정 없음 | 404 | 일정을 찾을 수 없습니다. |
| `CONTENT_NOT_FOUND` | 콘텐츠 없음 | 404 | 관광 콘텐츠를 찾을 수 없습니다. |

### Validation

- `dayNumber`는 1부터 중복 없이 이어져야 합니다.
- 모든 `contentIds`를 합친 결과는 중복 없이 1~10개여야 합니다.
- 모든 콘텐츠는 충청남도 범위여야 합니다.

### 처리 기준

- 서버가 `startDate + dayNumber - 1`로 날짜를 계산하고 `contentIds` 배열 순서를 `sequence`로 저장합니다.
- 콘텐츠가 변경되면 기존 일자·시각·이동시간 배치는 더 이상 유효하지 않습니다.
- 일자 또는 순서가 바뀐 이동 구간은 AI 재계산 전까지 이동시간을 `null`로 제공합니다.

## 12. 내 일정 삭제

```http
DELETE /api/v1/plans/{planId}
```

관련 기능:

* 저장된 내 일정을 삭제합니다.

인증: 필수

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| - | - | - | 요청 본문 없음 |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| - | - | - | 본문 없음 (`204 No Content`) |

### Response Example

```http
HTTP/1.1 204 No Content
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |
| `PLAN_NOT_FOUND` | 일정 없음 | 404 | 일정을 찾을 수 없습니다. |

### Validation

- 본인 소유 일정만 삭제합니다.

## 13. AI 제안서 DOCX 생성

```http
POST /api/v1/plans/{planId}/proposals
```

관련 기능:

* 저장된 일정으로 외부 AI 서버에 제안서 생성을 요청합니다.
* 생성된 DOCX를 비공개 S3에 30일 보관하고 미리보기·다운로드 URL을 응답합니다.

인증: 필수

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `proposalTitle` | String | N | 제안서 제목 |
| `organizationName` | String | N | 조직명 |
| `purpose` | String | N | 워크숍 목적 |
| `additionalRequest` | String | N | AI 서버에 전달할 추가 요청 |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `proposalId` | Long | Y | 저장된 제안서 ID |
| `planId` | Long | Y | 제안서의 기준 일정 ID |
| `fileName` | String | Y | `.docx` 확장자를 포함한 파일명 |
| `fileSize` | Long | Y | DOCX 파일 크기이며 단위는 byte |
| `previewUrl` | String | Y | 프론트 DOCX 뷰어용 1일 유효 presigned URL |
| `downloadUrl` | String | Y | 다운로드용 1일 유효 presigned URL |
| `createdAt` | LocalDateTime | Y | 제안서 생성 시각 |
| `expiresAt` | LocalDateTime | Y | 제안서 보관 만료 시각 |

### Response Example

```json
{
  "proposalId": 15,
  "planId": 77,
  "fileName": "아산 워크숍 제안서.docx",
  "fileSize": 152340,
  "previewUrl": "https://presigned.example.com/proposal.docx",
  "downloadUrl": "https://presigned.example.com/proposal.docx",
  "createdAt": "2026-08-04T10:00:00",
  "expiresAt": "2026-09-03T10:00:00"
}
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `INVALID_REQUEST` | 요청값 오류 | 400 | 제안서 입력을 확인해 주세요. |
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |
| `PLAN_NOT_FOUND` | 일정 없음 | 404 | 일정을 찾을 수 없습니다. |
| `AI_RESPONSE_INVALID` | AI 응답 오류 | 502 | AI 제안서 응답을 확인해 주세요. |
| `AI_SERVER_UNAVAILABLE` | AI 서버 장애 | 503 | 잠시 후 다시 생성해 주세요. |

### Validation

- 일정에는 콘텐츠가 1개 이상 있어야 합니다.
- 제목과 조직명은 각각 최대 100자입니다. 제목이 비어 있으면 일정 제목 뒤에 `제안서`를 붙이고, 조직명이 비어 있으면 `Wordiga`를 사용합니다.
- 목적과 추가 요청은 각각 최대 1000자입니다.
- 응답 파일은 20 MiB 이하여야 하며 ZIP 내부에 `[Content_Types].xml`과 `word/document.xml`이 있어야 합니다.

### AI 및 저장소 연동 계약

- 백엔드는 저장된 일정 전체를 AI 제안서 서버에 전달합니다.
- AI 서버에서 받은 DOCX bytes를 검증한 뒤 비공개 S3에 저장합니다.
- S3 객체는 Lifecycle 정책으로 30일 뒤 삭제하며 접근 URL은 1일 동안 유효합니다.

## 14. 일정별 제안서 목록 조회

```http
GET /api/v1/plans/{planId}/proposals
```

인증: 필수

### Path Parameter

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `planId` | Long | Y | 제안서를 조회할 일정 ID | `77` |

### Query Parameter

없습니다.

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| - | - | - | 요청 본문 없음 |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `proposalId` | Long | Y | 저장된 제안서 ID |
| `planId` | Long | Y | 제안서의 기준 일정 ID |
| `fileName` | String | Y | `.docx` 확장자를 포함한 파일명 |
| `fileSize` | Long | Y | DOCX 파일 크기이며 단위는 byte |
| `previewUrl` | String | Y | 조회 시 새로 발급한 1일 유효 미리보기 URL |
| `downloadUrl` | String | Y | 조회 시 새로 발급한 1일 유효 다운로드 URL |
| `createdAt` | LocalDateTime | Y | 제안서 생성 시각 |
| `expiresAt` | LocalDateTime | Y | 제안서 보관 만료 시각 |

### Response Example

```json
[
  {
    "proposalId": 15,
    "planId": 77,
    "fileName": "아산 워크숍 제안서.docx",
    "fileSize": 152340,
    "previewUrl": "https://presigned.example.com/proposal.docx",
    "downloadUrl": "https://presigned.example.com/proposal.docx",
    "createdAt": "2026-08-04T10:00:00",
    "expiresAt": "2026-09-03T10:00:00"
  }
]
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |
| `PLAN_NOT_FOUND` | 일정 없음 | 404 | 일정을 찾을 수 없습니다. |
| `PROPOSAL_STORAGE_UNAVAILABLE` | 저장소 장애 | 503 | 제안서 접근 URL을 발급할 수 없습니다. |

### Validation

- 본인 소유 일정의 제안서만 조회합니다.
- 보관 만료 시각이 지나지 않은 제안서만 반환합니다.

### 정렬 기준

- `createdAt DESC, proposalId DESC`로 정렬합니다.

## 15. 제안서 영구 삭제

```http
DELETE /api/v1/plans/{planId}/proposals/{proposalId}
```

인증: 필수

본인 소유 일정의 제안서만 삭제할 수 있습니다. S3 객체 삭제에 성공한 뒤 DB 메타데이터를 삭제하며 정상 응답은 `204 No Content`입니다. S3 삭제 실패 시 DB 행을 유지하고 `503 Service Unavailable`을 반환합니다.

| code | name | http code | description |
|---|---|---:|---|
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |
| `CONTENT_NOT_FOUND` | 제안서 없음 | 404 | 제안서를 찾을 수 없습니다. |
| `PROPOSAL_STORAGE_UNAVAILABLE` | 저장소 장애 | 503 | 제안서를 삭제할 수 없습니다. |

## 16. 내 소셜 프로필 조회

```http
GET /api/v1/me
```

관련 기능: 계정 관리 화면에서 로그인한 소셜 프로필을 조회합니다.

인증: 필수

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `memberId` | Long | Y | Wordiga 내부 회원 식별자 |
| `email` | String | Y | 소셜 공급자가 제공한 이메일 |
| `nickname` | String | N | 소셜 공급자가 제공한 프로필명 |
| `profileImage` | String | N | 소셜 공급자가 제공한 프로필 이미지 URL |
| `provider` | Enum | Y | `GOOGLE` 또는 `KAKAO` |

### Response Example

```json
{
  "memberId": 1,
  "email": "user@example.com",
  "nickname": "사용자",
  "profileImage": "https://example.com/profile.png",
  "provider": "GOOGLE"
}
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |
| `CONTENT_NOT_FOUND` | 회원 없음 | 404 | 회원을 찾을 수 없습니다. |

## 17. 회원탈퇴

```http
DELETE /api/v1/me
```

관련 기능: 계정 관리 화면에서 소셜 연결과 Wordiga 저장 데이터를 삭제합니다.

인증: 필수

### 처리 순서

1. Kakao 회원은 Kakao Unlink API로 앱 연결을 해제합니다.
2. Google 회원은 프론트에서 Google ID 공유 동의를 해제한 뒤 이 API를 호출합니다. 백엔드는 Google access/refresh token을 저장하지 않습니다.
3. S3에 저장된 회원 제안서 파일을 영구 삭제합니다.
4. `members` 행을 삭제하고 FK cascade로 위시·일정·제안서 메타데이터를 삭제합니다.
5. 삭제된 회원 ID로 발급한 기존 Wordiga JWT는 인증되지 않습니다.

정상 응답은 `204 No Content`입니다. 외부 연결 해제 또는 S3 삭제가 실패하면 회원 DB 행을 유지하고 `503 Service Unavailable`을 반환합니다.

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |
| `CONTENT_NOT_FOUND` | 회원 없음 | 404 | 회원을 찾을 수 없습니다. |
| `AI_SERVER_UNAVAILABLE` | 외부 서비스 실패 | 503 | 소셜 연결을 해제하거나 외부 저장소를 처리할 수 없습니다. |
| `PROPOSAL_STORAGE_UNAVAILABLE` | 저장소 장애 | 503 | 제안서 파일을 삭제할 수 없습니다. |

### 외부 API

- Kakao `POST https://kapi.kakao.com/v1/user/unlink`를 사용합니다.
- 인증은 `KAKAO_ADMIN_KEY`와 회원의 Kakao `providerId`를 사용합니다.

## 18. Access Token 재발급

```http
POST /api/v1/auth/refresh
Cookie: refreshToken={wordigaRefreshToken}
```

인증: Wordiga Refresh Token 쿠키 필수

유효한 Refresh Token의 회원이 현재 존재할 때 1시간 유효한 새 Access Token을 반환합니다. 응답에는 `Cache-Control: no-store`를 적용합니다.

```json
{
  "accessToken": "eyJ..."
}
```

Refresh Token이 없거나, 만료·위조되었거나, 탈퇴한 회원의 토큰이면 `401 Unauthorized`를 반환합니다.

## 19. 로그아웃

```http
DELETE /api/v1/auth/session
```

브라우저의 `refreshToken` 쿠키를 즉시 만료시킵니다. FE는 함께 보관 중인 Access Token도 삭제해야 합니다.

현재 Refresh Token은 서버 저장소에 보관하지 않는 stateless JWT이므로, 이미 탈취된 Refresh Token의 개별 폐기는 지원하지 않습니다. 회원탈퇴 또는 `JWT_SECRET` 교체 시에는 더 이상 사용할 수 없습니다.
