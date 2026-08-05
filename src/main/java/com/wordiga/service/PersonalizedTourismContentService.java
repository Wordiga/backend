package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.client.dto.AreaBasedItem;
import com.wordiga.domain.Wish;
import com.wordiga.dto.tourismContent.TourismContentDto;
import com.wordiga.dto.tourismContent.TourismContentListResponse;
import com.wordiga.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class PersonalizedTourismContentService {
    private static final String CHUNGNAM = "44";
    private final WishRepository wishRepository;
    private final TourismApiClient tourismApiClient;
    private final TourismSatisfactionService satisfactionService;

    public TourismContentListResponse get(Long memberId, LocalDate visitDate, int page, int size) {
        if (memberId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        List<Wish> wishes = wishRepository.findByMemberIdOrderByCreatedAtDescIdDesc(memberId);
        if (wishes.isEmpty()) return TourismContentListResponse.builder()
                .items(List.of()).page(page).size(size).hasNext(false).build();
        Set<String> wishIds = wishes.stream().map(Wish::getContentId).collect(Collectors.toSet());
        Set<String> themes = wishes.stream().map(this::theme).filter(Objects::nonNull).collect(Collectors.toSet());
        List<String> signgus = wishes.stream().map(Wish::getSigunguCode).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting())).entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()).limit(3).map(Map.Entry::getKey).toList();
        Map<String, TourismContentDto> candidates = new LinkedHashMap<>();
        var satisfactionContext = new TourismSatisfactionService.Context();
        for (String signgu : signgus) {
            for (AreaBasedItem item : tourismApiClient.fetchAreaBasedContent(CHUNGNAM, signgu, 50)) {
                if (wishIds.contains(item.getContentid()) || !matchesTheme(item, themes)
                        || candidates.containsKey(item.getContentid())) continue;
                var satisfaction = satisfactionService.calculate(satisfactionContext, CHUNGNAM,
                        item.getLDongSignguCd(), item.getTitle(), visitDate, List.of());
                BigDecimal score = satisfaction == null ? BigDecimal.ZERO : satisfaction.getTotalScore();
                candidates.put(item.getContentid(), dto(item, score));
                if (candidates.size() == 30) break;
            }
            if (candidates.size() == 30) break;
        }
        List<TourismContentDto> sorted = candidates.values().stream()
                .sorted(Comparator.comparing(TourismContentDto::getRecommendationScore).reversed()).toList();
        int from = Math.min(page * size, sorted.size()); int to = Math.min(from + size, sorted.size());
        return TourismContentListResponse.builder().items(sorted.subList(from, to)).page(page).size(size)
                .hasNext(to < sorted.size()).build();
    }

    private String theme(Wish wish) {
        if (wish.getLclsSystem3Code() != null) return wish.getLclsSystem3Code();
        if (wish.getLclsSystem2Code() != null) return wish.getLclsSystem2Code();
        if (wish.getLclsSystem1Code() != null) return wish.getLclsSystem1Code();
        return wish.getContentTypeId();
    }

    private boolean matchesTheme(AreaBasedItem item, Set<String> themes) {
        return themes.isEmpty() || themes.contains(item.getLclsSystm3()) || themes.contains(item.getLclsSystm2())
                || themes.contains(item.getLclsSystm1()) || themes.contains(item.getContenttypeid());
    }

    private TourismContentDto dto(AreaBasedItem item, BigDecimal score) {
        return TourismContentDto.builder().contentId(item.getContentid()).contentTypeId(item.getContenttypeid())
                .title(item.getTitle()).addr1(item.getAddr1()).firstImage(item.getFirstimage())
                .lDongSignguCd(item.getLDongSignguCd()).mapx(number(item.getMapx())).mapy(number(item.getMapy()))
                .recommendationScore(score).build();
    }

    private BigDecimal number(String value) {
        try { return value == null || value.isBlank() ? null : new BigDecimal(value); }
        catch (NumberFormatException ignored) { return null; }
    }
}
