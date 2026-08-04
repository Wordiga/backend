package com.wordiga.dto.ai;

import com.wordiga.dto.plan.PlanGenerateRequest;
import com.wordiga.dto.tourismContent.detail.TourismCommonDetailDto;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import com.wordiga.dto.tourismContent.detail.TourismDetailInfoDto;
import com.wordiga.dto.tourismContent.detail.TourismIntroDetailDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiPlanRequestTest {
    @Test
    void sendsAvailableContentCostsAndTheirTotal() {
        TourismContentDetailResponse museum = detail("museum", "성인 3,000원", List.of());
        TourismDetailInfoDto room = TourismDetailInfoDto.builder()
                .roomOffSeasonWeekdayMinFee("80,000원")
                .roomPeakSeasonWeekendMinFee("120,000원")
                .build();
        TourismContentDetailResponse lodging = detail("lodging", null, List.of(room));
        TourismContentDetailResponse unknown = detail("food", null, List.of());

        AiPlanRequest result = AiPlanRequest.from("request", new PlanGenerateRequest(), List.of(museum, lodging, unknown));

        assertThat(result.getContents()).extracting(AiPlanContent::getEstimatedCost)
                .containsExactly(3_000L, 80_000L, null);
        assertThat(result.getTotalEstimatedCost()).isEqualTo(83_000L);
    }

    @Test
    void treatsFreeAsZeroAndAllUnknownCostsAsNull() {
        assertThat(AiPlanContent.from(detail("free", "무료", List.of())).getEstimatedCost()).isZero();
        assertThat(AiPlanRequest.from("request", new PlanGenerateRequest(),
                List.of(detail("unknown", "문의", List.of()))).getTotalEstimatedCost()).isNull();
    }

    private TourismContentDetailResponse detail(String id, String useFee, List<TourismDetailInfoDto> details) {
        return TourismContentDetailResponse.builder()
                .common(TourismCommonDetailDto.builder().contentId(id).build())
                .intro(TourismIntroDetailDto.builder().useFee(useFee).build())
                .details(details)
                .build();
    }
}
