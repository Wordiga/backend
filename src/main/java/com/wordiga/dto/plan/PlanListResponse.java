package com.wordiga.dto.plan;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class PlanListResponse {
    private List<PlanSummaryResponse> items; private int page; private int size; private boolean hasNext;
}
