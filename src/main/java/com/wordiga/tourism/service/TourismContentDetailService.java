package com.wordiga.tourism.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.*;
import com.wordiga.global.config.TourismProperties;
import com.wordiga.global.util.KtoUtils;
import com.wordiga.tourism.domain.TourismContentType;
import com.wordiga.tourism.domain.TourismContentPolicy;
import com.wordiga.tourism.dto.SatisfactionRequestDto;
import com.wordiga.tourism.dto.detail.SeasonalImageDto;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import com.wordiga.wish.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.wordiga.global.util.KtoUtils.extractItems;

@Service
@RequiredArgsConstructor
public class TourismContentDetailService {

    private final TourismProperties tourismProperties;
    private final TourismApiClient tourismApiClient;
    private final TourismDetailMapper detailMapper;
    private final TourismSatisfactionService satisfactionService;
    private final WishRepository wishRepository;

    static Map<String, BigDecimal> parseAgeRatios(List<String> ageGroups) {
        if (ageGroups == null || ageGroups.isEmpty()) return Map.of();
        BigDecimal ratio = BigDecimal.ONE.divide(BigDecimal.valueOf(ageGroups.size()), 2, RoundingMode.HALF_UP);
        Map<String, BigDecimal> ratios = new LinkedHashMap<>();
        for (String age : ageGroups) {
            if (age != null && age.toLowerCase(Locale.ROOT).replace("_", "-").equals("50s-plus")) {
                BigDecimal half = ratio.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
                ratios.merge("50S", half, BigDecimal::add);
                ratios.merge("60S", half, BigDecimal::add);
            } else if (age != null) {
                ratios.merge(age, ratio, BigDecimal::add);
            }
        }
        return ratios;
    }

    public TourismContentDetailResponse getDetail(
            Long memberId,
            String contentId, LocalDate visitDate, List<String> ageGroups, Integer stayNights, Integer participantCount) {
        ContentDetailDto common = getCommonDetail(contentId);
        DetailIntroDto intro = tourismApiClient.fetchIntroDetail(contentId, common.getContenttypeid());
        List<DetailInfoDto> details = tourismApiClient.fetchRepeatInfo(contentId, common.getContenttypeid(), 1, 100);
        List<DetailImageDto> images = tourismApiClient.fetchImages(contentId, "Y", 1, 100);
        List<SeasonalImageDto> seasonalImages = fetchSeasonalImages(common, images);

        var satisfactionRequest = new SatisfactionRequestDto(
                common.getLDongRegnCd(),
                common.getLDongSignguCd(),
                common.getTitle(),
                visitDate,
                parseAgeRatios(ageGroups),
                stayNights
        );

        boolean isWished = memberId != null && wishRepository.existsByMemberIdAndContent_ContentId(memberId, contentId);

        return TourismContentDetailResponse.builder()
                .common(detailMapper.toCommon(common))
                .intro(detailMapper.toIntro(intro))
                .details(detailMapper.toDetails(details))
                .images(images.stream().map(detailMapper::toImage).toList())
                .seasonalImages(seasonalImages)
                .satisfaction(satisfactionService.calculate(satisfactionRequest, null))
                .capacitySatisfied(capacitySatisfied(common.getContenttypeid(), intro, details, participantCount))
                .isWished(isWished)
                .build();
    }

    public TourismContentDetailResponse getDetail(
            String contentId, LocalDate visitDate, List<String> ageGroups, Integer stayNights, Integer participantCount) {
        return getDetail(null, contentId, visitDate, ageGroups, stayNights, participantCount);
    }

    public ContentDetailDto getCommonDetail(String contentId) {
        ContentDetailDto common = tourismApiClient.fetchCommonDetail(contentId);
        if (common == null || !isChungnam(common) || TourismContentPolicy.isCamping(common.getLclsSystm2())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "충청남도 지역의 관광 콘텐츠를 찾을 수 없습니다.");
        }
        return common;
    }

    public Boolean capacitySatisfied(String contentId, int participantCount) {
        ContentDetailDto common = getCommonDetail(contentId);
        if (!TourismContentType.LODGING.getCode().equals(common.getContenttypeid())) return null;
        DetailIntroDto intro = tourismApiClient.fetchIntroDetail(contentId, common.getContenttypeid());
        List<DetailInfoDto> details = tourismApiClient.fetchRepeatInfo(contentId, common.getContenttypeid(), 1, 100);
        return capacitySatisfied(common.getContenttypeid(), intro, details, participantCount);
    }

    // ─── Helper Methods ───

    public TourismContentDetailResponse getAiDetail(String contentId, LocalDate visitDate) {
        ContentDetailDto common = getCommonDetail(contentId);
        DetailIntroDto intro = tourismApiClient.fetchIntroDetail(contentId, common.getContenttypeid());
        List<DetailInfoDto> details = tourismApiClient.fetchRepeatInfo(contentId, common.getContenttypeid(), 1, 100);

        return TourismContentDetailResponse.builder()
                .common(detailMapper.toCommon(common))
                .intro(detailMapper.toIntro(intro))
                .details(detailMapper.toDetails(details))
                .images(List.of())
                .seasonalImages(List.of())
                .build();
    }

    private Boolean capacitySatisfied(String contentTypeId, DetailIntroDto intro, List<DetailInfoDto> details, Integer participantCount) {
        if (!TourismContentType.LODGING.getCode().equals(contentTypeId)) return null;

        int participants = participantCount == null ? 10 : participantCount;
        int roomCapacity = details.stream()
                .mapToInt(d -> parseCapacity(d.getRoommaxcount()) * Math.max(1, parseCapacity(d.getRoomcount())))
                .sum();

        int capacity = roomCapacity > 0 ? roomCapacity : parseCapacity(intro == null ? null : intro.getAccomcountlodging());
        return capacity == 0 ? null : capacity >= participants;
    }

    private List<SeasonalImageDto> fetchSeasonalImages(ContentDetailDto common, List<DetailImageDto> fallbackImages) {
        List<PhotoGalleryItem> photos = extractItems(tourismApiClient.searchPhotos(common.getTitle()));
        if (photos.isEmpty()) {
            return fallbackImages.stream().map(detailMapper::toFallbackSeasonalImage).toList();
        }
        return photos.stream().map(detailMapper::toGallerySeasonalImage).toList();
    }

    private boolean isChungnam(ContentDetailDto content) {
        return tourismProperties.getRegion().getChungnamCode().equals(content.getLDongRegnCd());
    }

    private int parseCapacity(String value) {
        Integer parsed = KtoUtils.parseInteger(value);
        return parsed != null ? parsed : 0;
    }
}
