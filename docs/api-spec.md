## Wordiga Backend API 명세

### 전체 API Path

| Method | Path | Auth | 설명 |
|---|---|---|---|
| GET | `/api/v1/tourism/contents` | X | 관광 콘텐츠 목록 조회 |
| GET | `/api/v1/tourism/contents/{contentId}` | X | 관광 콘텐츠 통합 상세 조회 |
| POST | `/api/v1/wishes` | O | 위시 등록 및 지역 자동 분류 |
| DELETE | `/api/v1/wishes` | O | 위시 삭제 |
| GET | `/api/v1/wishes/folders` | O | 위시 지역 폴더 조회 |
| GET | `/api/v1/wishes/folders/{folderName}` | O | 폴더별 위시 목록 조회 |
| POST | `/api/v1/plans/generate` | O | AI 일정 생성 및 저장 |
| GET | `/api/v1/plans` | O | 내 일정 목록 조회 |
| GET | `/api/v1/plans/{planId}` | O | 내 일정 상세 조회 |
| PATCH | `/api/v1/plans/{planId}` | O | 내 일정 기본정보 수정 |
| PUT | `/api/v1/plans/{planId}/contents` | O | 내 일정 콘텐츠 수정 |
| DELETE | `/api/v1/plans/{planId}` | O | 내 일정 삭제 |
| POST | `/api/v1/plans/{planId}/proposals` | O | AI 제안서 DOCX 생성 |

### 공통 인증

- 인증이 필요한 API는 HTTP `Authorization` 헤더에 `Bearer {accessToken}`을 전달합니다.
- 비회원 위시리스트 저장은 제공하지 않습니다.
- Stateless JWT를 사용하므로 로그아웃은 프론트엔드가 저장한 토큰을 삭제하는 방식으로 처리합니다.

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

* 메인 화면의 인기 콘텐츠와 계절 콘텐츠를 조회합니다.
* 검색어와 지역·관광타입 필터를 적용합니다.

인증: 불필요

### Query Parameter

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `type` | Enum | N | 메인 화면 구역입니다. `POPULAR`, `SEASONAL`을 사용하며 기본값은 `POPULAR`입니다. | `POPULAR` |
| `visitDate` | LocalDate | N | 계절 정렬의 기준 방문일입니다. 없으면 오늘을 사용합니다. | `2026-08-20` |
| `keyword` | String | N | 콘텐츠명 검색어입니다. | `공주` |
| `contentTypeId` | Integer | N | 관광타입 ID입니다. | `12` |
| `lDongSignguCd` | String | N | 충청남도 법정동 시군구 코드입니다. | `200` |
| `page` | Integer | N | 0부터 시작하는 페이지입니다. 기본값은 `0`입니다. | `0` |
| `size` | Integer | N | 페이지 크기입니다. 기본값은 `20`, 최댓값은 `50`입니다. | `20` |

### Request Body

없습니다.

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `items` | ContentSummary[] | Y | 관광 콘텐츠 목록입니다. |
| `items[].contentId` | String | Y | 관광 콘텐츠 ID입니다. |
| `items[].contentTypeId` | String | Y | 관광타입 ID입니다. |
| `items[].title` | String | Y | 콘텐츠명입니다. |
| `items[].addr1` | String | N | 주소입니다. |
| `items[].lDongSignguCd` | String | N | 법정동 시군구 코드입니다. |
| `items[].mapx` | BigDecimal | N | 경도입니다. |
| `items[].mapy` | BigDecimal | N | 위도입니다. |
| `items[].firstImage` | String | N | 대표 이미지 URL입니다. |
| `items[].recommendationScore` | BigDecimal | Y | 정렬에 사용한 개인화 점수입니다. |
| `items[].recommendationReasons` | String[] | Y | 추천 사유입니다. |
| `page` | Integer | Y | 현재 페이지입니다. |
| `size` | Integer | Y | 페이지 크기입니다. |
| `hasNext` | Boolean | Y | 다음 페이지 존재 여부입니다. |

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
      "recommendationScore": 88.3,
      "recommendationReasons": [
        "위시리스트의 역사 관광지와 유사합니다.",
        "방문 예정 월에 적합합니다."
      ]
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
| `TOURISM_API_UNAVAILABLE` | 관광 API 장애 | 503 | 잠시 후 다시 조회해 주세요. |

### Validation

- `keyword`는 공백 제거 후 1~100자여야 합니다.
- `size`는 1~50이어야 합니다.
- `lDongSignguCd`는 충청남도 시군구 코드여야 합니다.

### 정렬 및 구현 계획

- 검색 후보는 한국관광공사 `areaBasedList2`, `searchKeyword2`, `searchFestival2`, `searchStay2`를 조건에 맞게 사용합니다.
- `POPULAR` 기본 점수는 소비 강도 60%, 체류 강도 40%로 계산합니다.
- `SEASONAL` 기본 점수는 방문 예정 월의 관광 서비스 수요를 사용합니다.
- 최종 정렬은 `recommendationScore DESC, contentId ASC`입니다.
- 별도의 추천 API와 검색 API는 만들지 않습니다.

## 2. 관광 콘텐츠 통합 상세 조회

```http
GET /api/v1/tourism/contents/{contentId}
```

관련 기능:

* 상세정보 화면에 필요한 공통정보, 관광타입별 소개정보, 반복정보, 이미지, 만족도, 소비지수, 계절 사진을 한 번에 제공합니다.
* 상세정보의 하트 버튼과 일정 만들기 화면에서 사용합니다.

인증: 불필요

### Path Parameter

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `contentId` | String | Y | 한국관광공사 관광 콘텐츠 ID입니다. | `126508` |

### Query Parameter

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `visitDate` | LocalDate | N | 집중률과 계절 사진 판단 기준일입니다. 없으면 오늘을 사용합니다. | `2026-08-20` |
| `participantCount` | Integer | N | 만족도 산출에 참고할 인원입니다. | `8` |
| `ageGroups` | String[] | N | 만족도 산출에 참고할 연령대입니다. | `20S,30S` |
| `maleRatio` | Integer | N | 남성 비율입니다. | `50` |
| `femaleRatio` | Integer | N | 여성 비율입니다. | `50` |
| `expectedStayMinutes` | Integer | N | 희망 체류시간입니다. | `90` |

### Request Body

없습니다.

### Response Body - 최상위

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `common` | CommonDetail | Y | 모든 관광타입의 공통정보입니다. |
| `intro` | IntroDetail | Y | `contentTypeId`별 소개정보입니다. 해당 타입 필드를 모두 제공합니다. |
| `details` | DetailInfo[] | Y | 타입별 반복정보입니다. 숙박은 객실, 여행코스는 코스 항목을 제공합니다. |
| `images` | DetailImage[] | Y | 상세 이미지와 저작권 정보입니다. |
| `spendingIndex` | SpendingIndex | N | 지역·업종 소비지수입니다. 실제 평균 소비액이 아닙니다. |
| `seasonalImages` | SeasonalImage[] | Y | 촬영일을 기준으로 계절을 의미화한 사진입니다. |
| `satisfaction` | Satisfaction | N | 관광 수요 기반 예상 만족도입니다. |

### Response Body - CommonDetail

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `contentId` | String | Y | 콘텐츠 ID입니다. |
| `contentTypeId` | String | Y | 관광타입 ID입니다. |
| `title` | String | Y | 콘텐츠명입니다. |
| `createdTime` | String | N | 한국관광공사 등록 시각입니다. |
| `modifiedTime` | String | N | 한국관광공사 수정 시각입니다. 촬영일로 사용하지 않습니다. |
| `tel` | String | N | 전화번호입니다. |
| `telName` | String | N | 전화번호 명칭입니다. |
| `homepage` | String | N | 홈페이지입니다. |
| `firstImage` | String | N | 대표 이미지입니다. |
| `firstImage2` | String | N | 대표 썸네일입니다. |
| `copyrightTypeCode` | String | N | 이미지 저작권 유형입니다. |
| `addr1` | String | N | 주소입니다. |
| `addr2` | String | N | 상세 주소입니다. |
| `zipcode` | String | N | 우편번호입니다. |
| `mapx` | BigDecimal | N | 경도입니다. |
| `mapy` | BigDecimal | N | 위도입니다. |
| `mapLevel` | String | N | 지도 레벨입니다. |
| `overview` | String | N | 콘텐츠 개요입니다. |
| `lDongRegnCd` | String | N | 법정동 시도 코드입니다. |
| `lDongSignguCd` | String | N | 법정동 시군구 코드입니다. |
| `lclsSystm1` | String | N | 분류체계 대분류입니다. |
| `lclsSystm2` | String | N | 분류체계 중분류입니다. |
| `lclsSystm3` | String | N | 분류체계 소분류입니다. |

### Response Body - IntroDetail 관광지 `contentTypeId=12`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `accomCount` | String | N | 수용 인원입니다. |
| `checkBabyCarriage` | String | N | 유모차 대여 가능 여부입니다. |
| `checkCreditCard` | String | N | 신용카드 사용 가능 여부입니다. |
| `checkPet` | String | N | 반려동물 동반 가능 여부입니다. |
| `experienceAgeRange` | String | N | 체험 가능 연령입니다. |
| `experienceGuide` | String | N | 체험 안내입니다. |
| `heritage1` | String | N | 세계문화유산 여부입니다. |
| `heritage2` | String | N | 세계자연유산 여부입니다. |
| `heritage3` | String | N | 세계기록유산 여부입니다. |
| `infoCenter` | String | N | 문의 및 안내 정보입니다. |
| `openDate` | String | N | 개장일입니다. |
| `parking` | String | N | 주차시설 정보입니다. |
| `restDate` | String | N | 쉬는 날입니다. |
| `useSeason` | String | N | 이용 가능한 시기입니다. |
| `useTime` | String | N | 이용시간입니다. |

### Response Body - IntroDetail 문화시설 `contentTypeId=14`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `accomCount` | String | N | 수용 인원입니다. |
| `checkBabyCarriage` | String | N | 유모차 대여 가능 여부입니다. |
| `checkCreditCard` | String | N | 신용카드 사용 가능 여부입니다. |
| `checkPet` | String | N | 반려동물 동반 가능 여부입니다. |
| `discountInfo` | String | N | 할인 정보입니다. |
| `infoCenter` | String | N | 문의 및 안내 정보입니다. |
| `parking` | String | N | 주차시설 정보입니다. |
| `parkingFee` | String | N | 주차요금입니다. |
| `restDate` | String | N | 쉬는 날입니다. |
| `useFee` | String | N | 이용요금입니다. |
| `useTime` | String | N | 이용시간입니다. |
| `scale` | String | N | 시설 규모입니다. |
| `spendTime` | String | N | 관람 소요시간입니다. |

### Response Body - IntroDetail 행사·공연·축제 `contentTypeId=15`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `ageLimit` | String | N | 관람 가능 연령입니다. |
| `bookingPlace` | String | N | 예매처입니다. |
| `discountInfo` | String | N | 할인 정보입니다. |
| `eventEndDate` | LocalDate | N | 행사 종료일입니다. |
| `eventHomepage` | String | N | 행사 홈페이지입니다. |
| `eventPlace` | String | N | 행사 장소입니다. |
| `eventStartDate` | LocalDate | N | 행사 시작일입니다. |
| `festivalGrade` | String | N | 축제 등급입니다. |
| `placeInfo` | String | N | 행사장 위치 안내입니다. |
| `playTime` | String | N | 공연시간입니다. |
| `program` | String | N | 행사 프로그램입니다. |
| `spendTime` | String | N | 관람 소요시간입니다. |
| `sponsor1` | String | N | 주최자 정보입니다. |
| `sponsor1Tel` | String | N | 주최자 연락처입니다. |
| `sponsor2` | String | N | 주관사 정보입니다. |
| `sponsor2Tel` | String | N | 주관사 연락처입니다. |
| `subEvent` | String | N | 부대행사 정보입니다. |
| `useTime` | String | N | 이용요금입니다. |

### Response Body - IntroDetail 여행코스 `contentTypeId=25`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `distance` | String | N | 코스 총거리입니다. |
| `infoCenter` | String | N | 문의 및 안내 정보입니다. |
| `schedule` | String | N | 코스 일정입니다. |
| `takeTime` | String | N | 코스 총 소요시간입니다. |
| `theme` | String | N | 코스 테마입니다. |

### Response Body - IntroDetail 레포츠 `contentTypeId=28`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `accomCount` | String | N | 수용 인원입니다. |
| `checkBabyCarriage` | String | N | 유모차 대여 가능 여부입니다. |
| `checkCreditCard` | String | N | 신용카드 사용 가능 여부입니다. |
| `checkPet` | String | N | 반려동물 동반 가능 여부입니다. |
| `experienceAgeRange` | String | N | 체험 가능 연령입니다. |
| `infoCenter` | String | N | 문의 및 안내 정보입니다. |
| `openPeriod` | String | N | 개장 기간입니다. |
| `parkingFee` | String | N | 주차요금입니다. |
| `parking` | String | N | 주차시설 정보입니다. |
| `reservation` | String | N | 예약 안내입니다. |
| `restDate` | String | N | 쉬는 날입니다. |
| `scale` | String | N | 시설 규모입니다. |
| `useFee` | String | N | 입장료입니다. |
| `useTime` | String | N | 이용시간입니다. |

### Response Body - IntroDetail 숙박 `contentTypeId=32`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `accomCount` | String | N | 수용 가능 인원입니다. |
| `checkInTime` | String | N | 입실시간입니다. |
| `checkOutTime` | String | N | 퇴실시간입니다. |
| `checkCooking` | String | N | 객실 내 취사 가능 여부입니다. |
| `foodPlace` | String | N | 식음료장 정보입니다. |
| `infoCenter` | String | N | 문의 및 안내 정보입니다. |
| `parking` | String | N | 주차시설 정보입니다. |
| `pickup` | String | N | 픽업 서비스 정보입니다. |
| `roomCount` | String | N | 객실 수입니다. |
| `reservation` | String | N | 예약 안내입니다. |
| `reservationUrl` | String | N | 예약 홈페이지입니다. |
| `roomType` | String | N | 객실 유형입니다. |
| `scale` | String | N | 숙박시설 규모입니다. |
| `subFacility` | String | N | 기타 부대시설입니다. |
| `barbecue` | String | N | 바비큐장 여부입니다. |
| `beauty` | String | N | 뷰티시설 여부입니다. |
| `beverage` | String | N | 식음료장 여부입니다. |
| `bicycle` | String | N | 자전거 대여 여부입니다. |
| `campfire` | String | N | 캠프파이어 가능 여부입니다. |
| `fitness` | String | N | 피트니스센터 여부입니다. |
| `karaoke` | String | N | 노래방 여부입니다. |
| `publicBath` | String | N | 공용 샤워실 여부입니다. |
| `publicPc` | String | N | 공용 PC실 여부입니다. |
| `sauna` | String | N | 사우나실 여부입니다. |
| `seminar` | String | N | 세미나실 여부입니다. |
| `sports` | String | N | 스포츠시설 여부입니다. |
| `refundRegulation` | String | N | 환불 규정입니다. |

### Response Body - IntroDetail 쇼핑 `contentTypeId=38`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `checkBabyCarriage` | String | N | 유모차 대여 가능 여부입니다. |
| `checkCreditCard` | String | N | 신용카드 사용 가능 여부입니다. |
| `checkPet` | String | N | 반려동물 동반 가능 여부입니다. |
| `cultureCenter` | String | N | 문화센터 정보입니다. |
| `fairDay` | String | N | 장이 서는 날입니다. |
| `infoCenter` | String | N | 문의 및 안내 정보입니다. |
| `openDate` | String | N | 개장일입니다. |
| `openTime` | String | N | 영업시간입니다. |
| `parking` | String | N | 주차시설 정보입니다. |
| `restDate` | String | N | 쉬는 날입니다. |
| `restroom` | String | N | 화장실 정보입니다. |
| `saleItem` | String | N | 판매 품목입니다. |
| `saleItemCost` | String | N | 판매 품목별 가격입니다. |
| `scale` | String | N | 매장 규모입니다. |
| `shopGuide` | String | N | 매장 안내입니다. |

### Response Body - IntroDetail 음식점 `contentTypeId=39`

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `checkCreditCard` | String | N | 신용카드 사용 가능 여부입니다. |
| `discountInfo` | String | N | 할인 정보입니다. |
| `firstMenu` | String | N | 대표 메뉴입니다. |
| `infoCenter` | String | N | 문의 및 안내 정보입니다. |
| `kidsFacility` | String | N | 어린이 놀이방 여부입니다. |
| `openDate` | String | N | 개업일입니다. |
| `openTime` | String | N | 영업시간입니다. |
| `packing` | String | N | 포장 가능 여부입니다. |
| `parking` | String | N | 주차시설 정보입니다. |
| `reservation` | String | N | 예약 안내입니다. |
| `restDate` | String | N | 쉬는 날입니다. |
| `scale` | String | N | 음식점 규모입니다. |
| `seat` | String | N | 좌석 수입니다. |
| `smoking` | String | N | 금연·흡연 여부입니다. |
| `treatMenu` | String | N | 취급 메뉴입니다. |
| `licenseNumber` | String | N | 인허가 번호입니다. |

### Response Body - DetailInfo

일반 관광타입:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `serialNumber` | Integer | N | 반복정보 순서입니다. |
| `infoName` | String | N | 반복정보 제목입니다. |
| `infoText` | String | N | 반복정보 내용입니다. |
| `fieldType` | String | N | 반복정보 유형 구분값입니다. |

여행코스:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `subContentId` | String | N | 하위 콘텐츠 ID입니다. |
| `subDetailAlt` | String | N | 코스 이미지 설명입니다. |
| `subDetailImage` | String | N | 코스 이미지 URL입니다. |
| `subDetailOverview` | String | N | 코스 개요입니다. |
| `subName` | String | N | 코스명입니다. |
| `subNumber` | Integer | N | 코스 순서입니다. |

숙박 객실:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `roomCode` | String | N | 객실 코드입니다. |
| `roomTitle` | String | N | 객실명입니다. |
| `roomSizePyeong` | BigDecimal | N | 객실 크기(평)입니다. |
| `roomCount` | Integer | N | 객실 수입니다. |
| `roomBaseCount` | Integer | N | 기준 인원입니다. |
| `roomMaxCount` | Integer | N | 최대 인원입니다. |
| `roomOffSeasonWeekdayMinFee` | Long | N | 비수기 주중 최소 요금입니다. |
| `roomOffSeasonWeekendMinFee` | Long | N | 비수기 주말 최소 요금입니다. |
| `roomPeakSeasonWeekdayMinFee` | Long | N | 성수기 주중 최소 요금입니다. |
| `roomPeakSeasonWeekendMinFee` | Long | N | 성수기 주말 최소 요금입니다. |
| `roomIntro` | String | N | 객실 소개입니다. |
| `roomBathFacility` | String | N | 목욕시설 여부입니다. |
| `roomBath` | String | N | 욕조 여부입니다. |
| `roomHomeTheater` | String | N | 홈시어터 여부입니다. |
| `roomAirCondition` | String | N | 에어컨 여부입니다. |
| `roomTv` | String | N | TV 여부입니다. |
| `roomPc` | String | N | PC 여부입니다. |
| `roomCable` | String | N | 케이블 설치 여부입니다. |
| `roomInternet` | String | N | 인터넷 가능 여부입니다. |
| `roomRefrigerator` | String | N | 냉장고 여부입니다. |
| `roomToiletries` | String | N | 세면도구 여부입니다. |
| `roomSofa` | String | N | 소파 여부입니다. |
| `roomCook` | String | N | 취사용품 여부입니다. |
| `roomTable` | String | N | 테이블 여부입니다. |
| `roomHairDryer` | String | N | 헤어드라이어 여부입니다. |
| `roomSizeSquareMeters` | BigDecimal | N | 객실 크기(제곱미터)입니다. |
| `roomImages` | RoomImage[] | Y | 객실 이미지 목록입니다. 이미지가 없으면 빈 배열입니다. |

객실 이미지 `RoomImage`:

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `imageUrl` | String | Y | 객실 이미지 URL입니다. |
| `alt` | String | N | 객실 이미지 설명입니다. |
| `copyrightTypeCode` | String | N | 이미지 저작권 유형입니다. |

### Response Body - 부가 산출정보

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `spendingIndex.regionName` | String | Y | 소비지수 집계 지역입니다. |
| `spendingIndex.categoryName` | String | Y | 소비지수 업종입니다. |
| `spendingIndex.indexValue` | BigDecimal | Y | 기준값 100 대비 소비지수입니다. |
| `spendingIndex.referencePeriod` | String | Y | 집계 기준 기간입니다. |
| `seasonalImages[].imageUrl` | String | Y | 사진 URL입니다. |
| `seasonalImages[].shootingDate` | LocalDate | N | 실제 촬영일입니다. |
| `seasonalImages[].season` | Enum | Y | `SPRING`, `SUMMER`, `AUTUMN`, `WINTER`, `UNKNOWN`입니다. |
| `seasonalImages[].matchConfidence` | BigDecimal | Y | 장소 일치 신뢰도입니다. |
| `satisfaction.totalScore` | BigDecimal | Y | 최종 만족도입니다. |
| `satisfaction.popularityScore` | ScoreComponent | Y | 인기도 점수입니다. |
| `satisfaction.ageFitScore` | ScoreComponent | Y | 연령 적합도입니다. |
| `satisfaction.stayFitScore` | ScoreComponent | Y | 체류 적합도입니다. |
| `satisfaction.comfortScore` | ScoreComponent | Y | 쾌적도 점수입니다. |

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
  "spendingIndex": {
    "regionName": "아산시",
    "categoryName": "숙박",
    "indexValue": 112.4,
    "referencePeriod": "2026-06"
  },
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

- `participantCount`는 1~100이어야 합니다.
- 성별 비율을 모두 전달한 경우 합계가 100이어야 합니다.
- `expectedStayMinutes`는 1~1440이어야 합니다.
- 충청남도 콘텐츠가 아니면 404를 반환합니다.

### 산출식 및 구현 계획

```text
totalScore = popularityScore × 0.30
           + ageFitScore × 0.35
           + stayFitScore × 0.20
           + comfortScore × 0.15

comfortScore = 100 - concentrationRate
```

- `detailCommon2`, `detailIntro2`, `detailInfo2`, `detailImage2`를 호출하여 하나의 응답으로 조합합니다.
- 소개정보는 `contentTypeId`에 해당하는 모든 필드를 전달합니다.
- 숙박 객실과 여행코스 하위 콘텐츠는 `detailInfo2`의 전체 반복 결과를 전달합니다.
- 소비액으로 오해되지 않도록 실제 금액이 아니라 지역·업종 소비지수를 제공합니다.
- 계절 사진은 대한민국 관광사진 API의 촬영일을 사용합니다. 수정일을 촬영일로 대체하지 않습니다.
- 촬영일이 없으면 `season=UNKNOWN`으로 제공합니다.
- 만족도 구성요소 하나가 누락되면 중립값 50과 `imputed=true`를 제공합니다. 모든 구성요소가 누락되면 `satisfaction=null`로 제공합니다.

## 3. 위시 등록

```http
POST /api/v1/wishes
```

관련 기능:

* 회원이 콘텐츠 카드 또는 상세정보의 하트 버튼으로 위시를 등록합니다.
* 서버가 관광공사 상세정보를 조회하여 충청남도 시군구별로 자동 분류합니다.

인증: 필수

### Query Parameter

없습니다.

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `contentId` | String | Y | 저장할 관광 콘텐츠 ID입니다. |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `id` | Long | Y | 위시 ID입니다. |
| `contentId` | String | Y | 콘텐츠 ID입니다. |
| `contentTypeId` | String | N | 관광타입 ID입니다. |
| `title` | String | Y | 콘텐츠명입니다. |
| `firstImage` | String | N | 대표 이미지입니다. |
| `addr1` | String | N | 주소입니다. |
| `lDongSignguCd` | String | N | 법정동 시군구 코드입니다. |
| `sigunguName` | String | N | 시군구명입니다. |
| `folderName` | String | Y | 자동 분류된 폴더명입니다. |
| `createdAt` | LocalDateTime | Y | 등록 시각입니다. |

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
| `CONTENT_OUT_OF_REGION` | 서비스 지역 아님 | 422 | 충청남도 관광 콘텐츠만 저장할 수 있습니다. |
| `TOURISM_API_UNAVAILABLE` | 관광 API 장애 | 503 | 잠시 후 다시 시도해 주세요. |

### Validation

- 동일 회원·콘텐츠 조합은 한 번만 저장합니다.
- 중복 요청이면 새 행을 만들지 않고 기존 위시를 반환합니다.
- 콘텐츠 정보는 요청에서 받지 않고 서버가 관광공사에서 조회합니다.

### 구현 계획

- 이번 검토에서 요청 DTO를 `contentId` 하나로 축소하였습니다.
- 시군구 코드표를 충청남도 15개 시군구 코드로 수정하였습니다.
- 현재는 기존 `WorkshopDetailService`를 재사용합니다. 통합 상세 API 구현 시 관광 콘텐츠 서비스로 이동합니다.

## 4. 위시 삭제

```http
DELETE /api/v1/wishes
```

관련 기능:

* 회원이 하트 버튼을 해제하여 위시를 삭제합니다.

인증: 필수

### Query Parameter

없습니다.

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `contentId` | String | Y | 삭제할 관광 콘텐츠 ID입니다. |

### Response Body

본문이 없는 `200 OK`를 반환합니다.

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

### 구현 계획

- 현재 Controller 계약을 유지합니다.
- 멱등 삭제가 필요하면 없는 위시도 동일하게 200으로 처리합니다.

## 5. 위시 폴더 목록 조회

```http
GET /api/v1/wishes/folders
```

관련 기능:

* 마이페이지 위시리스트 화면에 시군구별 폴더와 위시 수를 표시합니다.

인증: 필수

### Query Parameter

없습니다.

### Request Body

없습니다.

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `folderName` | String | Y | 시군구명 또는 기본 위시리스트입니다. |
| `lDongSignguCd` | String | N | 법정동 시군구 코드입니다. |
| `count` | Long | Y | 폴더의 위시 수입니다. |
| `thumbnailUrl` | String | N | 폴더에서 가장 최근에 저장한 위시의 이미지입니다. |

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
GET /api/v1/wishes/folders/{folderName}
```

관련 기능:

* 선택한 지역 폴더의 콘텐츠 카드와 지도 좌표를 표시합니다.

인증: 필수

### Path Parameter

| 이름 | 타입 | 필수 | 설명 | 예시 |
|---|---|---|---|---|
| `folderName` | String | Y | 시군구명 또는 기본 위시리스트입니다. | `아산시` |

### Query Parameter

없습니다.

### Request Body

없습니다.

### Response Body

위시 등록 응답 배열을 반환합니다. 지도 표시를 위해 향후 `mapx`, `mapy`를 위시 저장 모델과 응답에 추가해야 합니다.

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

## 7. AI 일정 생성 및 저장

```http
POST /api/v1/plans/generate
```

관련 기능:

* 상세정보의 단일 콘텐츠 또는 일정설계에서 선택한 최대 10개 콘텐츠로 AI 일정을 생성합니다.
* AI 서버 응답을 저장한 뒤 화면에 일자별 일정과 이동시간을 제공합니다.

인증: 필수

### Query Parameter

없습니다.

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `title` | String | N | 일정 제목입니다. |
| `startDate` | LocalDate | Y | 시작일입니다. |
| `endDate` | LocalDate | Y | 종료일입니다. |
| `participantCount` | Integer | Y | 참가 인원입니다. |
| `ageGroups` | String[] | N | 참가자 연령대입니다. |
| `maleRatio` | Integer | N | 남성 비율입니다. |
| `femaleRatio` | Integer | N | 여성 비율입니다. |
| `selectedContentIds` | String[] | Y | 선택한 콘텐츠 ID입니다. 리스트 순서를 사용자가 선택한 우선순위로 사용합니다. |
| `budgetPerPerson` | Long | N | 1인 예산입니다. |
| `additionalRequest` | String | N | AI에 전달할 추가 요청입니다. |

### Response Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `planId` | Long | Y | 저장된 일정 ID입니다. |
| `scheduleId` | String | N | AI 서버 일정 식별자입니다. |
| `title` | String | Y | 일정 제목입니다. |
| `startDate` | LocalDate | Y | 시작일입니다. |
| `endDate` | LocalDate | Y | 종료일입니다. |
| `participantCount` | Integer | Y | 참가 인원입니다. |
| `estimatedBudget` | EstimatedBudget | N | 예상 예산입니다. |
| `days` | PlanDay[] | Y | 일자별 일정입니다. |
| `warnings` | String[] | Y | 데이터 부족 또는 휴무 경고입니다. |
| `createdAt` | OffsetDateTime | Y | 저장 시각입니다. |

`PlanDay`는 `dayNumber`, `date`, `contents`를 제공합니다.

`contents[]`는 `sequence`, `contentId`, `title`, `contentTypeId`, `addr1`, `mapx`,
`mapy`, `startTime`, `endTime`, `durationMinutes`, `travelTimeMinutes`,
`travelDistanceMeters`, `estimatedCost`, `memo`를 제공합니다.

### Response Example

```json
{
  "planId": 77,
  "scheduleId": "ai-schedule-20260728-001",
  "title": "아산 역사 워크숍",
  "startDate": "2026-08-20",
  "endDate": "2026-08-21",
  "participantCount": 8,
  "estimatedBudget": {
    "totalAmount": 640000,
    "perPersonAmount": 80000,
    "currency": "KRW",
    "breakdown": []
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
          "memo": "오전 방문을 권장합니다."
        }
      ]
    }
  ],
  "warnings": [],
  "createdAt": "2026-07-28T15:40:00+09:00"
}
```

### Error Code

| code | name | http code | description |
|---|---|---:|---|
| `INVALID_REQUEST` | 요청값 오류 | 400 | 일정 생성 입력을 확인해 주세요. |
| `UNAUTHORIZED` | 인증 실패 | 401 | 로그인해 주세요. |
| `CONTENT_NOT_FOUND` | 콘텐츠 없음 | 404 | 선택한 콘텐츠를 찾을 수 없습니다. |
| `AI_RESPONSE_INVALID` | AI 응답 오류 | 502 | AI 일정 응답 형식을 확인해 주세요. |
| `AI_SERVER_UNAVAILABLE` | AI 서버 장애 | 503 | 잠시 후 다시 생성해 주세요. |

### Validation

- 시작일은 종료일보다 늦을 수 없습니다.
- 일정 기간은 최대 7일입니다.
- 참가 인원은 1~100명입니다.
- `selectedContentIds`는 중복 없이 1~10개입니다.
- AI 응답의 날짜는 요청 기간 안에 있어야 합니다.
- 일자별 `sequence`는 1부터 중복 없이 이어져야 합니다.

### 구현 계획

- 백엔드는 선택 콘텐츠의 타입별 상세정보, 좌표, 운영정보, 만족도와 소비지수를 AI 요청에 포함합니다.
- AI 서버가 이동시간과 이동거리를 제공합니다.
- AI 응답 원문 JSON과 조회에 필요한 정규화 데이터를 한 트랜잭션으로 저장합니다.
- AI 응답 계약은 GitHub Issue #10에서 확정합니다.

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
| `page` | Integer | N | 0부터 시작하는 페이지입니다. | `0` |
| `size` | Integer | N | 페이지 크기입니다. 기본값은 `20`, 최댓값은 `50`입니다. | `20` |
| `sort` | Enum | N | `LATEST`, `START_DATE_ASC`를 사용하며 기본값은 `LATEST`입니다. | `LATEST` |

### Request Body

없습니다.

### Response Body

`items[]`는 `planId`, `title`, `startDate`, `endDate`, `participantCount`,
`thumbnailUrl`, `contentCount`, `createdAt`, `updatedAt`을 제공합니다.

### Response Example

```json
{
  "items": [
    {
      "planId": 77,
      "title": "아산 역사 워크숍",
      "startDate": "2026-08-20",
      "endDate": "2026-08-21",
      "participantCount": 8,
      "thumbnailUrl": "https://example.com/main.jpg",
      "contentCount": 5,
      "createdAt": "2026-07-28T15:40:00+09:00",
      "updatedAt": "2026-07-28T15:40:00+09:00"
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
| `planId` | Long | Y | 일정 ID입니다. | `77` |

### Query Parameter

없습니다.

### Request Body

없습니다.

### Response Body

API 7의 응답과 동일하게 `days[].contents[]` 전체를 제공하며 `updatedAt`을 추가합니다.

### Response Example

```json
{
  "planId": 77,
  "scheduleId": "ai-schedule-20260728-001",
  "title": "아산 역사 워크숍",
  "startDate": "2026-08-20",
  "endDate": "2026-08-21",
  "participantCount": 8,
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
  "warnings": [],
  "createdAt": "2026-07-28T15:40:00+09:00",
  "updatedAt": "2026-07-28T15:40:00+09:00"
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
| `title` | String | N | 변경할 일정 제목입니다. |
| `participantCount` | Integer | N | 변경할 참가 인원입니다. |

### Response Body

`planId`, `title`, `participantCount`, `estimatedBudget`, `updatedAt`을 제공합니다.

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
    "breakdown": []
  },
  "updatedAt": "2026-07-28T16:00:00+09:00"
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
- 참가 인원은 1~100명이어야 합니다.

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
| `days` | PlanDayUpdate[] | Y | 일자별 콘텐츠 목록입니다. |
| `days[].dayNumber` | Integer | Y | 1부터 시작하는 일차입니다. |
| `days[].date` | LocalDate | Y | 해당 일차의 날짜입니다. |
| `days[].contentIds` | String[] | Y | 해당 일차의 콘텐츠 ID 목록입니다. 리스트 순서가 표시 순서입니다. |

### Response Body

API 9의 일정 상세 응답을 반환합니다. AI 재계산 전까지 변경된 구간의 이동시간과 예산은 `null`로 제공합니다.

### Request Example

```json
{
  "days": [
    {
      "dayNumber": 1,
      "date": "2026-08-20",
      "contentIds": [
        "126508",
        "2754012"
      ]
    },
    {
      "dayNumber": 2,
      "date": "2026-08-21",
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
  "warnings": [
    "콘텐츠 배치가 변경되어 이동시간을 다시 계산해야 합니다."
  ],
  "updatedAt": "2026-07-28T16:10:00+09:00"
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

- `days`의 날짜는 일정 시작일부터 종료일까지 중복 없이 이어져야 합니다.
- `dayNumber`는 1부터 중복 없이 이어져야 합니다.
- 모든 `contentIds`를 합친 결과는 중복 없이 1~10개여야 합니다.
- 모든 콘텐츠는 충청남도 범위여야 합니다.

### 구현 계획

- 각 `days[].contentIds`의 리스트 index를 해당 일차의 `sequence`로 저장합니다.
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

없습니다.

### Response Body

`204 No Content`를 반환합니다.

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

## 13. AI 제안서 DOCS 생성

```http
POST /api/v1/plans/{planId}/proposals
```

관련 기능:

* 저장된 일정으로 외부 AI 서버에 제안서 생성을 요청합니다.
* 생성된 DOCS 파일을 응답합니다.

인증: 필수

### Request Body

| 이름 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `proposalTitle` | String | N | 제안서 제목입니다. |
| `organizationName` | String | N | 조직명입니다. |
| `purpose` | String | N | 워크숍 목적입니다. |
| `additionalRequest` | String | N | AI 서버에 전달할 추가 요청입니다. |

### Response Body

- Content-Type: `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
- Content-Disposition: `attachment; filename="{proposalTitle}.docx"`
- Body: DOCS 파일 binary

### Response Example

```http
HTTP/1.1 200 OK
Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document
Content-Disposition: attachment; filename="asan-workshop-proposal.docx"

{binary}
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
- 제목과 조직명은 각각 1~100자여야 합니다.
- 추가 요청은 최대 1000자입니다.
- 응답 파일은 DOCX signature와 크기를 검증해야 합니다.

### 구현 계획

- 백엔드는 저장된 일정 전체를 AI 제안서 서버에 전달합니다.
- AI 서버에서 받은 DOCX bytes를 검증한 뒤 스트리밍 응답합니다.
- 장기 보관 요구가 확정되지 않았으므로 현재 설계에서는 파일을 DB에 저장하지 않습니다.
