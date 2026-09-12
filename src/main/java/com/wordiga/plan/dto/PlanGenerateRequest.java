package com.wordiga.plan.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class PlanGenerateRequest {
    @Size(max = 100) private String title;
    @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])", message = "방문 월은 YYYY-MM 형식이어야 합니다.")
    private String visitMonth;
    @NotNull @Min(1) @Max(3) private Integer stayDays;
    @NotNull @Min(10) @Max(50) private Integer participantCount;
    @Size(max = 10) private List<String> ageGroups;
    @NotEmpty @Size(max = 10) private List<@NotBlank String> selectedContentIds;
}
