package com.wordiga.tourism.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.*;
import com.wordiga.global.config.TourismProperties;
import com.wordiga.plan.dto.ContentCostDto;
import com.wordiga.plan.service.PlanCostPolicy;
import com.wordiga.tourism.domain.TourismCategory;
import com.wordiga.tourism.domain.TourismContentPolicy;
import com.wordiga.tourism.domain.TourismContentType;
import com.wordiga.tourism.domain.TourismTheme;
import com.wordiga.tourism.dto.*;
import com.wordiga.tourism.dto.SigunguResponse;
import com.wordiga.wish.Wish;
import com.wordiga.wish.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.wordiga.global.util.KtoUtils.*;

@Service
@RequiredArgsConstructor
public class TourismContentService {

    private static final int CONTENTS_PER_SIGNGU = 50;
    private static final int DEFAULT_PARTICIPANT_COUNT = 10;

    private final TourismApiClient tourismApiClient;
    private final TourismProperties tourismProperties;
    private final PersonalizedTourismContentService personalizedTourismContentService;
    private final TourismContentDetailService detailService;
    private final TourismSatisfactionService satisfactionService;
    private final WishRepository wishRepository;

    public TourismContentListResponse getContentListWithSatisfaction(
            Long memberId, ListType type, LocalDate visitDate, String keyword, String contentTypeId,
            String theme, List<String> categories, String lDongSignguCd, String referenceContentId,
            Boolean capacitySatisfied, Integer participantCount, List<String> ageGroups,
            Integer stayNights, int page, int size) {
        TourismContentListResponse response = getContentList(memberId, type, visitDate, keyword, contentTypeId,
                theme, categories, lDongSignguCd, referenceContentId, capacitySatisfied,
                participantCount, ageGroups, page, size);
        return addSatisfaction(response, visitDate, ageGroups, stayNights);
    }

    private TourismContentListResponse addSatisfaction(
            TourismContentListResponse response, LocalDate visitDate, List<String> ageGroups, Integer stayNights) {
        Map<String, BigDecimal> ageRatios = TourismContentDetailService.parseAgeRatios(ageGroups);
        var cache = new TourismSatisfactionService.CalculationCache();
        for (TourismContentDto item : response.getItems()) {
            var satisfaction = satisfactionService.calculate(new SatisfactionRequestDto(
                    tourismProperties.getRegion().getChungnamCode(), item.getLDongSignguCd(),
                    item.getTitle(), visitDate, ageRatios, stayNights), cache);
            item.setSatisfactionScore(satisfaction == null ? null : satisfaction.getTotalScore());
        }
        return response;
    }

    public List<SigunguResponse> getSigunguList() {
        List<SigunguItem> items = tourismApiClient.fetchSigunguList();
        return items.stream()
                .map(item -> new SigunguResponse(item.getCode(), item.getName()))
                .toList();
    }

    public TourismContentListResponse getContentList(
            Long memberId, ListType type, LocalDate visitDate, String keyword, String contentTypeId,
            String theme, List<String> categories,
            String lDongSignguCd, String referenceContentId, Boolean capacitySatisfied,
            Integer participantCount, List<String> ageGroups, int page, int size) {
        if (type == ListType.FESTIVAL)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "축제·행사는 /api/v1/tourism/contents/festivals에서 조회해 주세요.");
        if (type == ListType.PERSONALIZED)
            return enrichMemberData(memberId, visitDate, ageGroups,
                    personalizedTourismContentService.get(
                            memberId, visitDate, ageGroups, participantCount, capacitySatisfied,
                            theme, categories, page, size));
        if (type == ListType.RELATED)
            return enrichMemberData(memberId, visitDate, ageGroups,
                    related(referenceContentId, page, size));
        if (keyword != null && !keyword.isBlank()) {
            return enrichMemberData(memberId, visitDate, ageGroups,
                    filterCapacity(search(keyword.trim(), contentTypeId,
                                    lDongSignguCd, page, size, theme, categories,
                                    participantCount),
                            capacitySatisfied, participantCount, visitDate, ageGroups));
        }

        LocalDate targetDate = visitDate == null ? LocalDate.now() : visitDate;
        List<ScoredCandidate> candidates = switch (type) {
            case POPULAR -> fetchPopularCandidates(targetDate);
            case SEASONAL -> fetchSeasonalCandidates(targetDate);
            default -> throw new IllegalStateException("지원하지 않는 추천 타입입니다.");
        };

        List<ScoredCandidate> filtered = candidates.stream()
                .filter(candidate -> !"15".equals(candidate.item().getContenttypeid()))
                .filter(candidate -> !TourismContentPolicy.isCamping(candidate.item().getLclsSystm2()))
                .filter(candidate -> contentTypeId == null || contentTypeId.isBlank()
                                     || contentTypeId.equals(candidate.item().getContenttypeid()))
                .filter(candidate -> matchesCategory(candidate.item(), theme, categories))
                .filter(candidate -> lDongSignguCd == null || lDongSignguCd.isBlank()
                                     || lDongSignguCd.equals(candidate.item().getLDongSignguCd()))
                .collect(
                        LinkedHashMap<String, ScoredCandidate>::new,
                        (items, candidate) -> items.putIfAbsent(candidate.item().getContentid(), candidate),
                        LinkedHashMap::putAll
                )
                .values().stream().toList();

        return enrichMemberData(memberId, visitDate, ageGroups,
                filterCapacity(page(filtered, type, page, size, participantCount), capacitySatisfied, participantCount,
                        visitDate, ageGroups));
    }

    public TourismContentListResponse getPlaces(Long memberId, ListType type, String keyword,
                                                String lDongSignguCd, int page, int size) {
        if (type == ListType.FESTIVAL || type == ListType.RELATED)
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "장소 목록 타입을 확인해 주세요.");
        TourismContentListResponse response = getContentList(memberId, type, null, keyword, null,
                null, List.of(), lDongSignguCd, null, null, DEFAULT_PARTICIPANT_COUNT,
                null, page, size);
        return excludeContentTypes(response, Set.of(
                TourismContentType.FESTIVAL.getCode(), TourismContentType.LODGING.getCode()));
    }

    public TourismContentListResponse getPlacesWithSatisfaction(
            Long memberId, ListType type, LocalDate visitDate, String keyword,
            String lDongSignguCd, List<String> ageGroups, Integer stayNights, int page, int size) {
        return addSatisfaction(getPlaces(memberId, type, keyword, lDongSignguCd, page, size),
                visitDate, ageGroups, stayNights);
    }

    public TourismContentListResponse getLodgings(Long memberId, String keyword,
                                                  String lDongSignguCd, int page, int size) {
        AreaBasedResponse response = keyword == null || keyword.isBlank()
                ? tourismApiClient.fetchLodgings(tourismProperties.getRegion().getChungnamCode(),
                lDongSignguCd, page + 1, size)
                : tourismApiClient.searchContent(keyword.trim(), TourismContentType.LODGING.getCode(),
                tourismProperties.getRegion().getChungnamCode(), lDongSignguCd, page + 1, size);
        return enrichMemberData(memberId, null, null,
                externalPage(response, page, size, Set.of(TourismContentType.LODGING.getCode())));
    }

    public TourismContentFestivalListResponse getFestivals(Long memberId, LocalDate visitDate,
                                                           String lDongSignguCd, int page, int size) {
        return enrichMemberDataFestival(memberId,
                festivals(visitDate, lDongSignguCd, page, size, DEFAULT_PARTICIPANT_COUNT));
    }

    public List<TourismCategoryGroupDto> getCategories() {
        return Arrays.stream(TourismTheme.values())
                .map(theme -> new TourismCategoryGroupDto(codeName(theme), Arrays.stream(TourismCategory.values())
                        .filter(category -> category != TourismCategory.FESTIVAL && category != TourismCategory.CAMPING)
                        .filter(category -> category.getTheme() == theme)
                        .map(this::codeName)
                        .toList()))
                .toList();
    }

    public TourismContentListResponse getContentList(
            Long memberId, ListType type, LocalDate visitDate, String keyword, String contentTypeId,
            String lDongSignguCd, Boolean capacitySatisfied,
            Integer participantCount, List<String> ageGroups, int page, int size) {
        return getContentList(memberId, type, visitDate, keyword, contentTypeId, lDongSignguCd,
                null, capacitySatisfied, participantCount, ageGroups, page, size);
    }

    public TourismContentListResponse getContentList(
            Long memberId, ListType type, LocalDate visitDate, String keyword, String contentTypeId,
            String lDongSignguCd, String referenceContentId, Boolean capacitySatisfied,
            Integer participantCount, List<String> ageGroups, int page, int size) {
        return getContentList(memberId, type, visitDate, keyword, contentTypeId, null, List.of(), lDongSignguCd,
                referenceContentId, capacitySatisfied, participantCount, ageGroups, page, size);
    }

    private TourismContentListResponse related(String referenceContentId, int page, int size) {
        if (referenceContentId == null || referenceContentId.isBlank())
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "연관 관광지 조회에는 기준 콘텐츠 ID가 필요합니다.");
        ContentDetailDto reference = tourismApiClient.fetchCommonDetail(referenceContentId);
        if (reference == null || !tourismProperties.getRegion().getChungnamCode().equals(reference.getLDongRegnCd()))
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "기준 관광 콘텐츠를 찾을 수 없습니다.");

        String areaCode = tourismProperties.getRegion().getChungnamCode();
        String signguCode = reference.getLDongSignguCd() == null ? null : areaCode + reference.getLDongSignguCd();
        String baseYm = LocalDate.now().minusMonths(2).format(DateTimeFormatter.ofPattern("yyyyMM"));
        List<RelatedTourismItem> related = tourismApiClient.fetchRelatedTourism(
                baseYm, areaCode, signguCode, reference.getTitle(), 50);

        LinkedHashMap<String, TourismContentDto> resolved = new LinkedHashMap<>();
        for (RelatedTourismItem item : related) {
            if (item.getRlteTatsNm() == null || resolved.size() >= (page + 1) * size + 1) continue;
            AreaBasedResponse search = tourismApiClient.searchContent(
                    item.getRlteTatsNm(), null, areaCode, null, 1, 10);
            extractItems(search).stream()
                    .filter(candidate -> item.getRlteTatsNm().equals(candidate.getTitle()))
                    .filter(candidate -> !TourismContentPolicy.isCamping(candidate.getLclsSystm2()))
                    .filter(candidate -> !referenceContentId.equals(candidate.getContentid()))
                    .findFirst()
                    .ifPresent(candidate -> resolved.putIfAbsent(candidate.getContentid(),
                            toDto(candidate, rankScore(item.getRlteRank() == null ? resolved.size() : item.getRlteRank() - 1))));
        }

        if (resolved.isEmpty() && reference.getLDongSignguCd() != null) {
            TourismCategory referenceCategory = TourismCategory.resolve(
                    reference.getLclsSystm1(), reference.getLclsSystm2(), reference.getLclsSystm3());
            tourismApiClient.fetchAreaBasedContent(areaCode, reference.getLDongSignguCd(), 50).stream()
                    .filter(candidate -> !referenceContentId.equals(candidate.getContentid()))
                    .filter(candidate -> !TourismContentPolicy.isCamping(candidate.getLclsSystm2()))
                    .sorted(Comparator.comparingInt((AreaBasedItem candidate) -> {
                        TourismCategory category = TourismCategory.resolve(
                                candidate.getLclsSystm1(), candidate.getLclsSystm2(), candidate.getLclsSystm3());
                        return category == referenceCategory ? 0 : 1;
                    }).thenComparingInt(candidate -> Objects.hash(referenceContentId, candidate.getContentid())))
                    .limit((long) (page + 1) * size + 1)
                    .forEach(candidate -> resolved.putIfAbsent(candidate.getContentid(),
                            toDto(candidate, rankScore(resolved.size()))));
        }

        List<TourismContentDto> items = new ArrayList<>(resolved.values());
        int from = Math.min(page * size, items.size());
        int to = Math.min(from + size, items.size());
        return TourismContentListResponse.builder().items(items.subList(from, to)).page(page).size(size)
                .hasNext(to < items.size() || related.size() == 50).build();
    }

    private TourismContentListResponse enrichMemberData(
            Long memberId, LocalDate visitDate, List<String> ageGroups, TourismContentListResponse response) {
        Set<String> wishedContentIds = memberId == null ? Set.of()
                : wishRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId).stream()
                  .map(Wish::getContentId)
                  .collect(Collectors.toSet());
        response.getItems().forEach(item -> {
            item.setWished(wishedContentIds.contains(item.getContentId()));
        });
        return response;
    }

    private TourismContentFestivalListResponse enrichMemberDataFestival(
            Long memberId, TourismContentFestivalListResponse response) {
        Set<String> wishedContentIds = memberId == null ? Set.of()
                : wishRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId).stream()
                  .map(Wish::getContentId)
                  .collect(Collectors.toSet());
        response.getItems().forEach(item -> {
            item.setWished(wishedContentIds.contains(item.getContentId()));
        });
        return response;
    }

    private TourismContentListResponse filterCapacity(TourismContentListResponse response,
                                                      Boolean required, Integer participantCount,
                                                      LocalDate visitDate, List<String> ageGroups) {
        if (!Boolean.TRUE.equals(required)) return response;
        int participants = participantCount == null ? DEFAULT_PARTICIPANT_COUNT : participantCount;
        List<TourismContentDto> items = response.getItems().stream()
                .filter(item -> TourismContentType.LODGING.getCode().equals(item.getContentTypeId()))
                .filter(item -> Boolean.TRUE.equals(detailService.getDetail(
                        item.getContentId(), visitDate, ageGroups, null, participants).getCapacitySatisfied()))
                .toList();
        return TourismContentListResponse.builder().items(items).page(response.getPage()).size(response.getSize())
                .totalCount(items.size()).totalPages(items.isEmpty() ? 0 : 1)
                .hasNext(response.isHasNext()).build();
    }

    private TourismContentListResponse search(
            String keyword, String contentTypeId, String lDongSignguCd, int page, int size,
            String theme, List<String> categories, Integer participantCount) {
        AreaBasedResponse response = tourismApiClient.searchContent(
                keyword,
                contentTypeId,
                tourismProperties.getRegion().getChungnamCode(),
                lDongSignguCd,
                page + 1,
                size
        );
        List<AreaBasedItem> items = extractItems(response).stream()
                .filter(item -> !"15".equals(item.getContenttypeid()))
                .filter(item -> !TourismContentPolicy.isCamping(item.getLclsSystm2()))
                .filter(item -> matchesCategory(item, theme, categories)).toList();
        int totalCount = response == null || response.getResponse() == null
                         || response.getResponse().getBody() == null
                ? items.size()
                : response.getResponse().getBody().getTotalCount();

        List<TourismContentDto> result = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            result.add(toDto(
                    items.get(index),
                    rankScore(page * size + index), participantCount
            ));
        }
        return TourismContentListResponse.builder()
                .items(result)
                .page(page)
                .size(size)
                .totalCount(totalCount)
                .totalPages((totalCount + size - 1) / size)
                .hasNext((long) (page + 1) * size < totalCount)
                .build();
    }

    private TourismContentFestivalListResponse festivals(LocalDate visitDate, String lDongSignguCd,
                                                         int page, int size, Integer participantCount) {
        if (visitDate == null) throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST, "축제 조회에는 방문 월이 필요합니다.");
        YearMonth visitMonth = YearMonth.from(visitDate);
        DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;
        AreaBasedResponse response = tourismApiClient.fetchFestivals(
                visitMonth.atDay(1).format(formatter), visitMonth.atEndOfMonth().format(formatter),
                tourismProperties.getRegion().getChungnamCode(), lDongSignguCd, page + 1, size);
        List<AreaBasedItem> items = extractItems(response).stream()
                .filter(item -> "15".equals(item.getContenttypeid()))
                .toList();
        int totalCount = response == null || response.getResponse() == null
                         || response.getResponse().getBody() == null ? items.size()
                : response.getResponse().getBody().getTotalCount();
        List<TourismContentFestivalDto> result = new ArrayList<>();
        for (int index = 0; index < items.size(); index++)
            result.add(toFestivalDto(items.get(index), rankScore(page * size + index), participantCount));
        return TourismContentFestivalListResponse.builder()
                .items(result).page(page).size(size).totalCount(totalCount)
                .totalPages((totalCount + size - 1) / size)
                .hasNext((long) (page + 1) * size < totalCount).build();
    }

    private TourismContentListResponse externalPage(AreaBasedResponse response, int page, int size,
                                                    Set<String> includedContentTypes) {
        List<AreaBasedItem> source = extractItems(response).stream()
                .filter(item -> includedContentTypes.contains(item.getContenttypeid()))
                .filter(item -> !TourismContentPolicy.isCamping(item.getLclsSystm2()))
                .toList();
        List<TourismContentDto> items = new ArrayList<>();
        for (int index = 0; index < source.size(); index++)
            items.add(toDto(source.get(index), rankScore(page * size + index)));
        int totalCount = response == null || response.getResponse() == null
                         || response.getResponse().getBody() == null ? items.size()
                : response.getResponse().getBody().getTotalCount();
        return TourismContentListResponse.builder().items(items).page(page).size(size)
                .totalCount(totalCount).totalPages((totalCount + size - 1) / size)
                .hasNext((long) (page + 1) * size < totalCount).build();
    }

    private TourismContentListResponse excludeContentTypes(TourismContentListResponse response,
                                                           Set<String> excludedContentTypes) {
        List<TourismContentDto> items = response.getItems().stream()
                .filter(item -> !excludedContentTypes.contains(item.getContentTypeId()))
                .toList();
        return TourismContentListResponse.builder().items(items).page(response.getPage()).size(response.getSize())
                .totalCount(response.getTotalCount()).totalPages(response.getTotalPages())
                .hasNext(response.isHasNext()).build();
    }

    private TourismContentListResponse page(
            List<ScoredCandidate> candidates, ListType type, int page, int size, Integer participantCount) {
        int fromIndex = Math.min(page * size, candidates.size());
        int toIndex = Math.min(fromIndex + size, candidates.size());
        List<TourismContentDto> items = new ArrayList<>();
        for (int index = fromIndex; index < toIndex; index++) {
            ScoredCandidate candidate = candidates.get(index);
            items.add(toDto(candidate.item(), candidate.score(), participantCount));
        }
        return TourismContentListResponse.builder()
                .items(items)
                .page(page)
                .size(size)
                .totalCount(candidates.size())
                .totalPages((candidates.size() + size - 1) / size)
                .hasNext(toIndex < candidates.size())
                .build();
    }

    private List<ScoredCandidate> fetchPopularCandidates(LocalDate targetDate) {
        String chungnamCode = tourismProperties.getRegion().getChungnamCode();
        String currentYm = getCurrentYm();
        AreaTarExpDsResponse expenditure = tourismApiClient.fetchExpenditureIntensity(
                currentYm, chungnamCode, null, "2201");
        AreaTarSjrnDsResponse stay = tourismApiClient.fetchStayIntensity(
                currentYm, chungnamCode, null, "2103");

        Map<String, Double> scores = calculatePopularityScore(expenditure, stay);
        List<String> signguCodes = scores
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .toList();
        return fetchContentsBySignguCodes(signguCodes, scores, null, "POPULAR:" + targetDate);
    }

    private List<ScoredCandidate> fetchSeasonalCandidates(LocalDate visitDate) {
        String baseYm = visitDate.minusYears(1).format(DateTimeFormatter.ofPattern("yyyyMM"));
        AreaTarSvcDemResponse response = tourismApiClient.fetchServiceDemand(
                baseYm, tourismProperties.getRegion().getChungnamCode(), null, "11");

        Map<String, Double> scores = extractItems(response).stream().collect(Collectors.toMap(
                AreaTarSvcDemItem::getSignguCd, item -> parseDouble(item.getTarSvcDemIxVal()), Math::max));
        List<String> signguCodes = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(
                        b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        return fetchContentsBySignguCodes(signguCodes, scores, visitDate.getMonthValue(),
                "SEASONAL:" + java.time.YearMonth.from(visitDate));
    }

    private List<ScoredCandidate> fetchContentsBySignguCodes(
            List<String> signguCodes, Map<String, Double> scores, Integer month, String orderSeed) {
        String regionCode = tourismProperties.getRegion().getChungnamCode();
        List<List<ScoredCandidate>> byRegion = signguCodes.stream().map(this::convertToLDongSignguCd)
                .map(signguCode -> tourismApiClient.fetchAreaBasedContent(regionCode, signguCode, CONTENTS_PER_SIGNGU)
                        .stream().map(item -> new ScoredCandidate(item, score(
                                scores.getOrDefault(regionCode + signguCode, 0D), item, month)))
                        .sorted(Comparator.comparingLong(candidate ->
                                stableOrderKey(orderSeed, signguCode, candidate.item().getContentid())))
                        .toList())
                .toList();
        List<ScoredCandidate> result = new ArrayList<>();
        int maxSize = byRegion.stream().mapToInt(List::size).max().orElse(0);
        for (int index = 0; index < maxSize; index++)
            for (List<ScoredCandidate> items : byRegion) if (index < items.size()) result.add(items.get(index));
        return result;
    }

    private long stableOrderKey(String... values) {
        long value = Objects.hash((Object[]) values);
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        return value ^ value >>> 33;
    }

    private boolean matchesCategory(AreaBasedItem item, String theme, List<String> categories) {
        TourismCategory resolved = TourismCategory.resolve(
                item.getLclsSystm1(), item.getLclsSystm2(), item.getLclsSystm3());
        if (theme != null && !theme.isBlank()
            && (resolved == null || !theme.equals(resolved.getTheme().getCode()))) return false;
        if (categories == null || categories.isEmpty()) return true;
        Set<String> codes = categories.stream().filter(Objects::nonNull)
                .flatMap(value -> Arrays.stream(value.split(","))).map(String::trim)
                .filter(value -> !value.isEmpty()).collect(Collectors.toSet());
        return codes.isEmpty() || resolved != null && codes.contains(resolved.getCode());
    }

    private BigDecimal score(double regionalScore, AreaBasedItem item, Integer month) {
        if (month == null) return BigDecimal.valueOf(regionalScore);
        double seasonalFit = switch (month) {
            case 3, 4, 5 -> List.of("12", "25").contains(item.getContenttypeid()) ? 100 : 50;
            case 6, 7, 8 -> List.of("12", "15", "28").contains(item.getContenttypeid()) ? 100 : 50;
            case 9, 10, 11 -> List.of("12", "14", "25").contains(item.getContenttypeid()) ? 100 : 50;
            default -> List.of("14", "28", "32").contains(item.getContenttypeid()) ? 100 : 50;
        };
        return BigDecimal.valueOf(regionalScore * 0.7 + seasonalFit * 0.3)
                .setScale(1, java.math.RoundingMode.HALF_UP);
    }

    private TourismContentDto toDto(AreaBasedItem item, BigDecimal recommendationScore) {
        return toDto(item, recommendationScore, DEFAULT_PARTICIPANT_COUNT);
    }

    private TourismContentDto toDto(
            AreaBasedItem item, BigDecimal recommendationScore, Integer participantCount) {
        int participants = participantCount == null ? DEFAULT_PARTICIPANT_COUNT : participantCount;
        TourismCategory category = TourismCategory.resolve(
                item.getLclsSystm1(), item.getLclsSystm2(), item.getLclsSystm3());
        return TourismContentDto.builder()
                .contentId(item.getContentid())
                .contentTypeId(item.getContenttypeid())
                .title(item.getTitle())
                .addr1(item.getAddr1())
                .lDongSignguCd(item.getLDongSignguCd())
                .mapx(parseBigDecimal(item.getMapx()))
                .mapy(parseBigDecimal(item.getMapy()))
                .firstImage(item.getFirstimage())
                .theme(category == null ? null : codeName(category.getTheme()))
                .category(category == null ? null : codeName(category))
                .recommendationScore(recommendationScore)
                .estimatedCost(PlanCostPolicy.defaultPerPersonAmount(item.getContenttypeid(), participants))
                .cost(ContentCostDto.from(PlanCostPolicy.defaultEstimate(item.getContenttypeid(), participants),
                        participants))
                .build();
    }

    private TourismContentFestivalDto toFestivalDto(
            AreaBasedItem item, BigDecimal recommendationScore, Integer participantCount) {
        int participants = participantCount == null ? DEFAULT_PARTICIPANT_COUNT : participantCount;
        TourismCategory category = TourismCategory.resolve(
                item.getLclsSystm1(), item.getLclsSystm2(), item.getLclsSystm3());
        return TourismContentFestivalDto.builder()
                .contentId(item.getContentid())
                .contentTypeId(item.getContenttypeid())
                .eventStartDate(formatFestivalDate(item.getEventstartdate()))
                .eventEndDate(formatFestivalDate(item.getEventenddate()))
                .title(item.getTitle())
                .addr1(item.getAddr1())
                .lDongSignguCd(item.getLDongSignguCd())
                .mapx(parseBigDecimal(item.getMapx()))
                .mapy(parseBigDecimal(item.getMapy()))
                .firstImage(item.getFirstimage())
                .theme(category == null ? null : codeName(category.getTheme()))
                .category(category == null ? null : codeName(category))
                .recommendationScore(recommendationScore)
                .estimatedCost(PlanCostPolicy.defaultPerPersonAmount(item.getContenttypeid(), participants))
                .cost(ContentCostDto.from(PlanCostPolicy.defaultEstimate(item.getContenttypeid(), participants),
                        participants))
                .build();
    }

    private String formatFestivalDate(String value) {
        if (value == null || !value.matches("\\d{8}")) return null;
        return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE).toString();
    }

    private String convertToLDongSignguCd(String signguCd) {
        return signguCd == null || signguCd.length() <= 2 ? null : signguCd.substring(2);
    }

    private BigDecimal rankScore(int index) {
        return BigDecimal.valueOf(Math.max(1, 100 - index));
    }

    private CodeNameDto codeName(TourismTheme theme) {
        return new CodeNameDto(theme.getCode(), theme.getDisplayName());
    }

    private CodeNameDto codeName(TourismCategory category) {
        return new CodeNameDto(category.getCode(), category.getDisplayName());
    }

    private String getCurrentYm() {
        LocalDate now = LocalDate.now();
        LocalDate target = now.getDayOfMonth() >= 16 ? now.minusMonths(1) : now.minusMonths(2);
        return target.format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private Map<String, Double> calculatePopularityScore(
            AreaTarExpDsResponse expenditure, AreaTarSjrnDsResponse stay) {
        Map<String, Double> scores = new HashMap<>();
        extractItems(expenditure).forEach(item ->
                scores.merge(item.getSignguCd(), parseDouble(item.getTarExpDsIxVal()) * 0.6, Double::sum));
        extractItems(stay).forEach(item ->
                scores.merge(item.getSignguCd(), parseDouble(item.getTarSjrnDsIxVal()) * 0.4, Double::sum));
        return scores;
    }

    private record ScoredCandidate(AreaBasedItem item, BigDecimal score) {
    }
}
