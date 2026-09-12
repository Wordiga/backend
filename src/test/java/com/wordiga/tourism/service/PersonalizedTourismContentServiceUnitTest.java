package com.wordiga.tourism.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.AreaBasedItem;
import com.wordiga.global.config.TourismProperties;
import com.wordiga.wish.service.WishPreferenceCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalizedTourismContentServiceUnitTest {
    @Mock TourismApiClient tourismApiClient;
    @Mock WishPreferenceCacheService preferenceCacheService;
    @Mock TourismSatisfactionService satisfactionService;
    @Mock TourismContentDetailService detailService;

    PersonalizedTourismContentService service;
    LocalDate visitDate = LocalDate.of(2026, 8, 1);

    @BeforeEach
    void setUp() {
        TourismProperties properties = new TourismProperties();
        properties.getRegion().setChungnamCode("44");
        service = new PersonalizedTourismContentService(tourismApiClient, preferenceCacheService,
                properties, satisfactionService, detailService);
    }

    @Test
    void ageFitChangesOrderWhileKeepingWishAndSeasonWeights() {
        when(preferenceCacheService.getSigunguPreferenceRatios(1L))
                .thenReturn(Map.of("200", new BigDecimal("0.60"), "340", new BigDecimal("0.40")));
        when(preferenceCacheService.getCategoryPreferenceRatios(1L)).thenReturn(Map.of());
        when(tourismApiClient.fetchAreaBasedContent("44", "200", 30))
                .thenReturn(List.of(item("wish-favorite", "200", "12")));
        when(tourismApiClient.fetchAreaBasedContent("44", "340", 20))
                .thenReturn(List.of(item("age-favorite", "340", "12")));
        Map<String, BigDecimal> ages = Map.of("20S", new BigDecimal("1.00"));
        doReturn(BigDecimal.valueOf(20)).when(satisfactionService).ageFitForRegion("44", "200", ages);
        doReturn(BigDecimal.valueOf(100)).when(satisfactionService).ageFitForRegion("44", "340", ages);

        var result = service.get(1L, visitDate, List.of("20S"), 10, null, null, List.of(), 0, 20);

        assertThat(result.getItems()).extracting("contentId")
                .containsExactly("age-favorite", "wish-favorite");
        assertThat(result.getItems()).extracting("recommendationScore")
                .containsExactly(new BigDecimal("63.5"), new BigDecimal("59.5"));
        verify(satisfactionService).ageFitForRegion("44", "200", ages);
    }

    @Test
    void participantCountChangesLodgingEligibilityAndPerPersonCost() {
        when(preferenceCacheService.getSigunguPreferenceRatios(1L))
                .thenReturn(Map.of("200", BigDecimal.ONE));
        when(preferenceCacheService.getCategoryPreferenceRatios(1L)).thenReturn(Map.of());
        when(tourismApiClient.fetchAreaBasedContent("44", "200", 50))
                .thenReturn(List.of(item("hotel", "200", "32")));
        when(satisfactionService.ageFitForRegion("44", "200", Map.of()))
                .thenReturn(BigDecimal.valueOf(50));
        when(detailService.capacitySatisfied("hotel", 10)).thenReturn(true);
        when(detailService.capacitySatisfied("hotel", 25)).thenReturn(false);

        var ten = service.get(1L, visitDate, List.of(), 10, null, null, List.of(), 0, 20);
        var twentyFive = service.get(1L, visitDate, List.of(), 25, null, null, List.of(), 0, 20);

        assertThat(ten.getItems()).hasSize(1);
        assertThat(ten.getItems().getFirst().getEstimatedCost()).isEqualTo(10_000);
        assertThat(twentyFive.getItems()).isEmpty();
        assertThat(twentyFive.getTotalCount()).isZero();
    }

    @Test
    void unknownCapacityIsKeptUnlessConfirmedCapacityIsRequested() {
        when(preferenceCacheService.getSigunguPreferenceRatios(1L))
                .thenReturn(Map.of("200", BigDecimal.ONE));
        when(preferenceCacheService.getCategoryPreferenceRatios(1L)).thenReturn(Map.of());
        when(tourismApiClient.fetchAreaBasedContent("44", "200", 50))
                .thenReturn(List.of(item("hotel", "200", "32"), item("place", "200", "12")));
        when(satisfactionService.ageFitForRegion("44", "200", Map.of()))
                .thenReturn(BigDecimal.valueOf(50));
        when(detailService.capacitySatisfied("hotel", 10)).thenReturn(null);

        assertThat(service.get(1L, visitDate, List.of(), 10, null, null, List.of(), 0, 20)
                .getItems()).extracting("contentId").containsExactlyInAnyOrder("hotel", "place");
        assertThat(service.get(1L, visitDate, List.of(), 10, true, null, List.of(), 0, 20)
                .getItems()).isEmpty();
    }

    private AreaBasedItem item(String id, String sigungu, String type) {
        AreaBasedItem item = new AreaBasedItem();
        item.setContentid(id);
        item.setLDongSignguCd(sigungu);
        item.setContenttypeid(type);
        item.setTitle(id);
        return item;
    }
}
