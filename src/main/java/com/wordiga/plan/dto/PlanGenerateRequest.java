package com.wordiga.plan.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class PlanGenerateRequest {
    @Size(max = 100) private String title;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    @Min(1) @Max(12) private Integer visitMonth;
    @NotNull @Min(1) @Max(3) private Integer stayDays;
    @NotNull @Min(10) @Max(50) private Integer participantCount;
    @Size(max = 10) private List<String> ageGroups;
    @NotEmpty @Size(max = 10) private List<@NotBlank String> selectedContentIds;
}
