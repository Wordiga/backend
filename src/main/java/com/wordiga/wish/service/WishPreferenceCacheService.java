package com.wordiga.wish.service;

import com.wordiga.repository.WishRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishPreferenceCacheService {

    private final WishRepository wishRepository;

    /**
     * 유저의 시군구별 선호 비중(Ratio)을 조회 (인메모리 캐싱)
     * Key: memberId / Value: Map<SigunguCode, PreferenceRatio>
     */
    @Cacheable(value = "userSigunguPreferences", key = "#memberId")
    public Map<String, BigDecimal> getSigunguPreferenceRatios(Long memberId) {
        List<WishRepository.SigunguCountDto> counts = wishRepository.countWishesBySigunguGroup(memberId);
        if (counts.isEmpty()) {
            return Map.of();
        }

        long totalCount = counts.stream().mapToLong(WishRepository.SigunguCountDto::count).sum();

        // (해당 구 위시 개수 / 전체 위시 개수) 계산하여 비중 산출
        return counts.stream().collect(Collectors.toMap(
                WishRepository.SigunguCountDto::sigunguCode,
                dto -> BigDecimal.valueOf((double) dto.count() / totalCount).setScale(2, RoundingMode.HALF_UP)
        ));
    }

    /**
     * 위시 추가/삭제 발생 시 해당 유저의 선호도 캐시 삭제 (Evict)
     */
    @CacheEvict(value = "userSigunguPreferences", key = "#memberId")
    public void evictUserPreferenceCache(Long memberId) {
        // 캐시 갱신을 위해 비워둠
    }
}