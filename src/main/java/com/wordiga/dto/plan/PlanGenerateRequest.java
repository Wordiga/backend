package com.wordiga.dto.plan;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class PlanGenerateRequest {
    @Size(max = 100) private String title;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    @NotNull @Min(1) @Max(100) private Integer participantCount;
    @Size(max = 10) private List<String> ageGroups;
    @Min(0) @Max(100) private Integer maleRatio;
    @Min(0) @Max(100) private Integer femaleRatio;
    @NotEmpty @Size(max = 10) private List<@NotBlank String> selectedContentIds;
    @PositiveOrZero private Long budgetPerPerson;
    @Size(max = 1000) private String additionalRequest;
}
