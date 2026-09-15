package com.wordiga.plan.dto;

import com.wordiga.plan.CostSource;
import com.wordiga.plan.CostUnit;
import com.wordiga.plan.PlanContent;
import com.wordiga.plan.service.PlanCostPolicy;

public record ContentCostDto(
        Long unitAmount,
        CostUnit unit,
        Integer quantity,
        Long totalAmount,
        Long perPersonShare,
        String currency,
        CostSource source) {

    public static ContentCostDto from(PlanCostPolicy.Estimate estimate, int participants) {
        return new ContentCostDto(estimate.amount(), estimate.unit(), estimate.quantity(),
                estimate.calculatedAmount(), estimate.calculatedAmount() / Math.max(1, participants), "KRW",
                estimate.fallbackApplied() ? CostSource.DEFAULT : CostSource.TOUR_API);
    }

    public static ContentCostDto from(PlanContent content) {
        if (content.getUnitAmount() == null && content.getEstimatedCost() == null) return null;
        return new ContentCostDto(content.getUnitAmount(), content.getCostUnit(), content.getCostQuantity(),
                content.getEstimatedCost(), content.getPerPersonShare(), "KRW", content.getCostSource());
    }
}
