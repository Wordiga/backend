package com.wordiga.dto.plan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PlanUpdateRequest {
    @Size(max = 100) private String title;
    @Min(1) @Max(50) private Integer participantCount;
}
