package com.wordiga.dto.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class PlanContentsUpdateRequest {
    @NotEmpty @Valid private List<Day> days;
    @Data public static class Day {
        @NotNull @Positive private Integer dayNumber;
        @NotEmpty @Size(max = 10) private List<@NotBlank String> contentIds;
    }
}
