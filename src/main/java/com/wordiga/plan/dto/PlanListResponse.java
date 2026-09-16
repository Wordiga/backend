package com.wordiga.plan.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class PlanListResponse {
    private List<PlanSummaryResponse> items;
    private int page;
    private int size;
    private long totalCount;
    private int totalPages;
    private boolean hasNext;
}
