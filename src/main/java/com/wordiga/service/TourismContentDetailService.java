package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.client.dto.AreaTarExpDsItem;
import com.wordiga.client.dto.AreaTarExpDsResponse;
import com.wordiga.client.dto.KtoApiResponse;
import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.DetailImageDto;
import com.wordiga.dto.DetailInfoDto;
import com.wordiga.dto.DetailIntroDto;
import com.wordiga.dto.tourismContent.detail.SeasonalImageDto;
import com.wordiga.dto.tourismContent.detail.SpendingIndexDto;
import com.wordiga.dto.tourismContent.detail.TourismCommonDetailDto;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import com.wordiga.dto.tourismContent.detail.TourismDetailImageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TourismContentDetailService {

    private static final String CHUNGNAM_REGION_CODE = "44";

    private final WorkshopDetailService workshopDetailService;
    private final TourismApiClient tourismApiClient;
    private final TourismDetailMapper detailMapper;
    private final TourismSatisfactionService satisfactionService;

    public TourismContentDetailResponse getDetail(
            String contentId, LocalDate visitDate, List<String> ageGroups) {
        ContentDetailDto common = workshopDetailService.fetchCommonDetail(contentId);
        if (common == null || !CHUNGNAM_REGION_CODE.equals(common.getLDongRegnCd())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "관광 콘텐츠를 찾을 수 없습니다.");
        }

        DetailIntroDto intro = workshopDetailService.fetchIntroDetail(
                contentId, common.getContenttypeid());
        List<DetailInfoDto> details = workshopDetailService.fetchRepeatInfo(
                contentId, common.getContenttypeid(), 1, 100);
        List<DetailImageDto> images = workshopDetailService.fetchImages(
                contentId, "Y", 1, 100);

        return TourismContentDetailResponse.builder()
                .common(toCommon(common))
                .intro(detailMapper.toIntro(intro))
                .details(detailMapper.toDetails(details))
                .images(images.stream().map(this::toImage).toList())
                .spendingIndex(fetchSpendingIndex(common))
                .seasonalImages(images.stream().map(this::toSeasonalImage).toList())
                .satisfaction(satisfactionService.calculate(common, visitDate, ageGroups))
                .build();
    }

    private TourismCommonDetailDto toCommon(ContentDetailDto source) {
        return TourismCommonDetailDto.builder()
                .contentId(source.getContentid())
                .contentTypeId(source.getContenttypeid())
                .title(source.getTitle())
                .createdTime(source.getCreatedtime())
                .modifiedTime(source.getModifiedtime())
                .tel(source.getTel())
                .telName(source.getTelname())
                .homepage(source.getHomepage())
                .firstImage(source.getFirstimage())
                .firstImage2(source.getFirstimage2())
                .copyrightTypeCode(source.getCpyrhtDivCd())
                .addr1(source.getAddr1())
                .addr2(source.getAddr2())
                .zipcode(source.getZipcode())
                .mapx(toBigDecimal(source.getMapx()))
                .mapy(toBigDecimal(source.getMapy()))
                .mapLevel(source.getMlevel())
                .overview(source.getOverview())
                .lDongRegnCd(source.getLDongRegnCd())
                .lDongSignguCd(source.getLDongSignguCd())
                .lclsSystm1(source.getLclsSystm1())
                .lclsSystm2(source.getLclsSystm2())
                .lclsSystm3(source.getLclsSystm3())
                .build();
    }

    private TourismDetailImageDto toImage(DetailImageDto source) {
        return TourismDetailImageDto.builder()
                .imageName(source.getImgname())
                .imageUrl(source.getOriginimgurl())
                .thumbnailUrl(source.getSmallimageurl())
                .copyrightTypeCode(source.getCpyrhtDivCd())
                .serialNumber(toInteger(source.getSerialnum()))
                .build();
    }

    private SeasonalImageDto toSeasonalImage(DetailImageDto source) {
        return SeasonalImageDto.builder()
                .imageUrl(source.getOriginimgurl())
                .thumbnailUrl(source.getSmallimageurl())
                .shootingDate(null)
                .season(SeasonalImageDto.Season.UNKNOWN)
                .matchConfidence(BigDecimal.ONE)
                .build();
    }

    private SpendingIndexDto fetchSpendingIndex(ContentDetailDto common) {
        String referencePeriod = currentBaseYm();
        String signguCode = common.getLDongSignguCd() == null
                ? null
                : CHUNGNAM_REGION_CODE + common.getLDongSignguCd();
        AreaTarExpDsResponse response = tourismApiClient.fetchExpenditureIntensity(
                referencePeriod, CHUNGNAM_REGION_CODE, signguCode, "2201");
        List<AreaTarExpDsItem> items = extractItems(response);
        if (items.isEmpty()) {
            return null;
        }
        AreaTarExpDsItem item = items.getFirst();
        return SpendingIndexDto.builder()
                .regionName(item.getSignguNm())
                .categoryName(categoryName(common.getContenttypeid()))
                .indexValue(toBigDecimal(item.getTarExpDsIxVal()))
                .referencePeriod(item.getBaseYm())
                .build();
    }

    private String currentBaseYm() {
        LocalDate now = LocalDate.now();
        LocalDate target = now.getDayOfMonth() >= 16 ? now.minusMonths(1) : now.minusMonths(2);
        return target.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private String categoryName(String contentTypeId) {
        return switch (contentTypeId) {
            case "12" -> "관광지";
            case "14" -> "문화시설";
            case "15" -> "행사/공연/축제";
            case "25" -> "여행코스";
            case "28" -> "레포츠";
            case "32" -> "숙박";
            case "38" -> "쇼핑";
            case "39" -> "음식점";
            default -> "기타";
        };
    }

    private <T> List<T> extractItems(KtoApiResponse<T> response) {
        if (response == null || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse().getBody().getItems().getItem() == null) {
            return Collections.emptyList();
        }
        return response.getResponse().getBody().getItems().getItem();
    }

    private BigDecimal toBigDecimal(String value) {
        try {
            return value == null || value.isBlank() ? null : new BigDecimal(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer toInteger(String value) {
        try {
            return value == null || value.isBlank() ? null : Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
