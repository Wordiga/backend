package com.wordiga.dto.ai;

import com.wordiga.dto.plan.PlanGenerateRequest;
import com.wordiga.dto.tourismContent.detail.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiPlanRequestTest {
    @Test
    void calculatesActualMinimumAndFallbackMaximum() {
        TourismContentDetailResponse museum = detail("museum", "14", "성인 3,000원", List.of());
        TourismContentDetailResponse food = detail("food", "39", null, List.of());

        AiPlanRequest result = AiPlanRequest.from("request", request(2), List.of(museum, food));

        assertThat(result.getContents().getFirst().getCost()).satisfies(cost -> {
            assertThat(cost.getUnit()).isEqualTo(AiPlanContent.Unit.PERSON);
            assertThat(cost.getCalculatedAmount()).isEqualTo(6_000L);
            assertThat(cost.isFallbackApplied()).isFalse();
        });
        assertThat(result.getContents().get(1).getCost()).satisfies(cost -> {
            assertThat(cost.getCalculatedAmount()).isNull();
            assertThat(cost.getFallbackAmount()).isEqualTo(15_000L);
            assertThat(cost.getFallbackCalculatedAmount()).isEqualTo(30_000L);
            assertThat(cost.isFallbackApplied()).isTrue();
        });
        assertThat(result.getTotalCostRange().minimumAmount()).isEqualTo(6_000L);
        assertThat(result.getTotalCostRange().maximumAmount()).isEqualTo(36_000L);
    }

    @Test
    void calculatesLodgingRoomsAndUsesCheapestCollectedRate() {
        TourismDetailInfoDto room = TourismDetailInfoDto.builder().roomMaxCount("2명")
                .roomOffSeasonWeekdayMinFee("80,000원").roomPeakSeasonWeekendMinFee("120,000원").build();

        AiPlanContent result = AiPlanContent.from(detail("lodging", "32", null, List.of(room)), 3);

        assertThat(result.getCost().getUnit()).isEqualTo(AiPlanContent.Unit.ROOM);
        assertThat(result.getCost().getQuantity()).isEqualTo(2);
        assertThat(result.getCost().getAmount()).isEqualTo(80_000L);
        assertThat(result.getCost().getCalculatedAmount()).isEqualTo(160_000L);
    }

    @Test
    void supportsFreeAndTypeDefaults() {
        assertThat(AiPlanContent.from(detail("free", "28", "무료", List.of()), 4)
                .getCost().getCalculatedAmount()).isZero();
        assertThat(AiPlanContent.from(detail("course", "25", null, List.of()), 4)
                .getCost().getFallbackCalculatedAmount()).isZero();
    }

    private PlanGenerateRequest request(int participants) {
        PlanGenerateRequest request = new PlanGenerateRequest();
        request.setParticipantCount(participants);
        return request;
    }

    private TourismContentDetailResponse detail(String id, String type, String useFee,
                                                List<TourismDetailInfoDto> details) {
        return TourismContentDetailResponse.builder()
                .common(TourismCommonDetailDto.builder().contentId(id).contentTypeId(type).build())
                .intro(TourismIntroDetailDto.builder().useFee(useFee).build())
                .details(details).build();
    }
}
