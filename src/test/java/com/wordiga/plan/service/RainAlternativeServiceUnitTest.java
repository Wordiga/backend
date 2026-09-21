package com.wordiga.plan.service;

import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.client.dto.AreaBasedItem;
import com.wordiga.global.config.TourismProperties;
import com.wordiga.plan.Plan;
import com.wordiga.plan.PlanContent;
import com.wordiga.plan.repository.PlanRepository;
import com.wordiga.tourism.domain.TourismContentSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RainAlternativeServiceUnitTest {
    @Mock PlanRepository planRepository;
    @Mock TourismApiClient tourismApiClient;
    RainAlternativeService service;

    @BeforeEach
    void setUp() {
        TourismProperties properties = new TourismProperties();
        properties.getRegion().setChungnamCode("44");
        service = new RainAlternativeService(planRepository, tourismApiClient, properties);
    }

    @Test
    void mapsNearbyIndoorCandidatesBySourceAndReusesSigunguSearch() {
        TourismContentSnapshot outdoor = snapshot("outdoor", "12", "NA", "NA01");
        TourismContentSnapshot unknown = snapshot("unknown", "15", "EV", "EV01");
        TourismContentSnapshot indoor = snapshot("indoor", "14", "VE", "VE07");
        Plan plan = plan(outdoor, unknown, indoor);
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.of(plan));
        AreaBasedItem exhibition = candidate("museum", "VE07", "14", 126.91, 36.81);
        exhibition.setFirstimage("https://example.com/museum.jpg");
        AreaBasedItem performance = candidate("theater", "VE06", "14", 126.92, 36.81);
        AreaBasedItem camping = candidate("camping", "AC05", "32", 126.901, 36.801);
        when(tourismApiClient.fetchAreaBasedContent("44", "200", 1000))
                .thenReturn(List.of(camping, exhibition, performance,
                        candidate("outdoor", "VE07", "14", 126.9, 36.8),
                        candidate("far", "VE07", "14", 127.5, 37.5)));

        var response = service.get(1L, 9L);

        assertThat(response.items()).extracting("sourceContentId").containsExactly("outdoor", "unknown");
        assertThat(response.items().getFirst().isOutdoor()).isTrue();
        assertThat(response.items().getFirst().alternatives()).extracting("contentId")
                .containsExactly("museum", "theater");
        assertThat(response.items().getFirst().alternatives().getFirst().thumbnailUrl())
                .isEqualTo("https://example.com/museum.jpg");
        assertThat(response.items().get(1).isOutdoor()).isNull();
        assertThat(response.items().get(1).alternatives()).extracting("contentId")
                .containsExactly("theater", "museum");
        verify(tourismApiClient, times(1)).fetchAreaBasedContent("44", "200", 1000);
    }

    @Test
    void rejectsPlansNotOwnedByMember() {
        when(planRepository.findByIdAndMemberId(9L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L, 9L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verifyNoInteractions(tourismApiClient);
    }

    private Plan plan(TourismContentSnapshot... snapshots) {
        Plan plan = mock(Plan.class);
        List<PlanContent> contents = java.util.Arrays.stream(snapshots).map(snapshot -> {
            PlanContent content = mock(PlanContent.class);
            when(content.getContent()).thenReturn(snapshot);
            return content;
        }).toList();
        when(plan.getPlanContents()).thenReturn(contents);
        return plan;
    }

    private TourismContentSnapshot snapshot(String id, String type, String large, String middle) {
        return TourismContentSnapshot.builder().contentId(id).contentTypeId(type).title(id)
                .sigunguCode("200").mapx(BigDecimal.valueOf(126.9)).mapy(BigDecimal.valueOf(36.8))
                .lclsSystem1Code(large).lclsSystem2Code(middle).build();
    }

    private AreaBasedItem candidate(String id, String middle, String type, double x, double y) {
        AreaBasedItem item = new AreaBasedItem();
        item.setContentid(id);
        item.setTitle(id);
        item.setContenttypeid(type);
        item.setLclsSystm2(middle);
        item.setLDongSignguCd("200");
        item.setMapx(String.valueOf(x));
        item.setMapy(String.valueOf(y));
        return item;
    }
}
