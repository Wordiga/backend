package com.wordiga.plan.dto.ai;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
public class AiPlanResponse {
    @com.fasterxml.jackson.annotation.JsonProperty("plan_id")
    private Long scheduleId;
    private EstimatedBudget estimatedBudget;
    private List<Day> days;

    @com.fasterxml.jackson.annotation.JsonProperty("timetable")
    public void readTimetable(Timetable timetable) {
        if (timetable == null || timetable.getDays() == null) {
            this.days = List.of();
            return;
        }
        this.days = timetable.getDays().stream().map(source -> {
            Day day = new Day();
            day.setDayNumber(source.getDay());
            List<Content> contents = new ArrayList<>();
            if (source.getItems() != null) source.getItems().forEach(item -> {
                Content content = new Content();
                content.setSequence(item.getOrder());
                content.setContentId(item.getContentId());
                content.setTitle(item.getTitle());
                content.setMapx(item.getLongitude());
                content.setMapy(item.getLatitude());
                content.setStartTime(parseTime(item.getStartTime()));
                content.setEndTime(parseTime(item.getEndTime()));
                if (content.getStartTime() != null && content.getEndTime() != null)
                    content.setDurationMinutes((int) Duration.between(content.getStartTime(), content.getEndTime()).toMinutes());
                content.setTravelTimeMinutes(item.getTravelTimeFromPrevMin());
                content.setMemo(item.getMemo());
                contents.add(content);
            });
            day.setContents(contents);
            return day;
        }).toList();
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalTime.parse(value.length() == 5 ? value + ":00" : value);
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class Timetable { private List<TimetableDay> days; }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimetableDay { private Integer day; private List<TimetableItem> items; }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class TimetableItem {
        private Integer order;
        private String startTime;
        private String endTime;
        private String contentId;
        private String title;
        private java.math.BigDecimal latitude;
        private java.math.BigDecimal longitude;
        private Integer travelTimeFromPrevMin;
        private String memo;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class EstimatedBudget {
        private Long totalAmount;
        private Long perPersonAmount;
        private String currency;
        private Map<String, Long> breakdown;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Day {
        private Integer dayNumber;
        private LocalDate date;
        private List<Content> contents;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Content {
        private Integer sequence;
        private String contentId;
        private String title;
        private String contentTypeId;
        private String addr1;
        private java.math.BigDecimal mapx;
        private java.math.BigDecimal mapy;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer durationMinutes;
        private Integer travelTimeMinutes;
        private Integer travelDistanceMeters;
        private Long estimatedCost;
        private String memo;
    }
}
