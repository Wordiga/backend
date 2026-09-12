package com.wordiga.plan.service;

import com.wordiga.tourism.dto.detail.TourismCommonDetailDto;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import com.wordiga.tourism.dto.detail.TourismDetailInfoDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanCostPolicyUnitTest {
    @Test
    void usesActualAdmissionFeeBeforeFallback() {
        var result = PlanCostPolicy.estimate(detail("12", List.of(TourismDetailInfoDto.builder()
                .infoName("입장료").infoText("성인 5,000원").build())), 10);

        assertThat(result.amount()).isEqualTo(5_000);
        assertThat(result.calculatedAmount()).isEqualTo(50_000);
        assertThat(result.fallbackApplied()).isFalse();
    }

    @Test
    void appliesRestaurantFallbackPerParticipant() {
        var result = PlanCostPolicy.estimate(detail("39", List.of()), 10);

        assertThat(result.amount()).isEqualTo(15_000);
        assertThat(result.calculatedAmount()).isEqualTo(150_000);
        assertThat(result.fallbackApplied()).isTrue();
    }

    @Test
    void calculatesLodgingByRequiredRoomCount() {
        var room = TourismDetailInfoDto.builder().roomMaxCount("4명")
                .roomOffSeasonWeekdayMinFee("80,000원").build();

        var result = PlanCostPolicy.estimate(detail("32", List.of(room)), 10);

        assertThat(result.quantity()).isEqualTo(3);
        assertThat(result.calculatedAmount()).isEqualTo(240_000);
    }

    private TourismContentDetailResponse detail(String type, List<TourismDetailInfoDto> details) {
        return TourismContentDetailResponse.builder()
                .common(TourismCommonDetailDto.builder().contentId("1").contentTypeId(type).build())
                .details(details).build();
    }
}
