package com.wordiga.dto.ai;

import lombok.Data;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AiPlanResponse {
    private String scheduleId; private EstimatedBudget estimatedBudget; private List<Day> days;
    @Data @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) public static class EstimatedBudget {
        private Long totalAmount; private Long perPersonAmount; private String currency; private Object breakdown;
    }
    @Data @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) public static class Day { private Integer dayNumber; private LocalDate date; private List<Content> contents; }
    @Data @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class) public static class Content {
        private Integer sequence; private String contentId; private String title; private String contentTypeId; private String addr1;
        private BigDecimal mapx; private BigDecimal mapy; private LocalTime startTime; private LocalTime endTime;
        private Integer durationMinutes; private Integer travelTimeMinutes; private Integer travelDistanceMeters;
        private Long estimatedCost; private String memo;
    }
}
