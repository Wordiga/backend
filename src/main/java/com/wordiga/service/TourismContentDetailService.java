package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.client.dto.KtoApiResponse;
import com.wordiga.client.dto.PhotoGalleryItem;
import com.wordiga.client.dto.PhotoGalleryResponse;
import com.wordiga.dto.ContentDetailDto;
import com.wordiga.dto.DetailImageDto;
import com.wordiga.dto.DetailInfoDto;
import com.wordiga.dto.DetailIntroDto;
import com.wordiga.dto.tourismContent.detail.SeasonalImageDto;
import com.wordiga.dto.tourismContent.detail.TourismCommonDetailDto;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import com.wordiga.dto.tourismContent.detail.TourismDetailImageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
        List<SeasonalImageDto> seasonalImages = seasonalImages(common, images);

        return TourismContentDetailResponse.builder()
                .common(toCommon(common))
                .intro(detailMapper.toIntro(intro))
                .details(detailMapper.toDetails(details))
                .images(images.stream().map(this::toImage).toList())
                .seasonalImages(seasonalImages)
                .satisfaction(satisfactionService.calculate(common, visitDate, ageGroups))
                .build();
    }

    public ContentDetailDto getCommonDetail(String contentId) {
        ContentDetailDto common = workshopDetailService.fetchCommonDetail(contentId);
        if (common == null || !CHUNGNAM_REGION_CODE.equals(common.getLDongRegnCd())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "관광 콘텐츠를 찾을 수 없습니다.");
        }
        return common;
    }

    private List<SeasonalImageDto> seasonalImages(ContentDetailDto common, List<DetailImageDto> fallback) {
        List<PhotoGalleryItem> photos = extractItems(tourismApiClient.searchPhotos(common.getTitle()));
        if (photos.isEmpty()) return fallback.stream().map(this::toSeasonalImage).toList();
        return photos.stream().map(this::toSeasonalImage).toList();
    }

    SeasonalImageDto toSeasonalImage(PhotoGalleryItem source) {
        String value = source.getGalPhotographyMonth();
        Integer month = null;
        LocalDate date = null;
        try {
            if (value != null && value.matches("\\d{8}")) {
                date = LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE); month = date.getMonthValue();
            } else if (value != null && value.matches("\\d{6}")) {
                month = YearMonth.parse(value, DateTimeFormatter.ofPattern("yyyyMM")).getMonthValue();
            }
        } catch (RuntimeException ignored) { }
        return SeasonalImageDto.builder().imageUrl(source.getGalWebImageUrl()).thumbnailUrl(source.getGalWebImageUrl())
                .shootingDate(date).season(toSeason(month)).matchConfidence(BigDecimal.ONE).build();
    }

    private SeasonalImageDto.Season toSeason(Integer month) {
        if (month == null) return SeasonalImageDto.Season.UNKNOWN;
        return switch (month) {
            case 3, 4, 5 -> SeasonalImageDto.Season.SPRING;
            case 6, 7, 8 -> SeasonalImageDto.Season.SUMMER;
            case 9, 10, 11 -> SeasonalImageDto.Season.AUTUMN;
            default -> SeasonalImageDto.Season.WINTER;
        };
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
