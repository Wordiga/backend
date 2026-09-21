package com.wordiga.proposal.dto;

import com.wordiga.plan.dto.PlanDetailResponse;
import lombok.Builder;
import lombok.Data;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AiProposalRequest {
    private Long planId;
    private CompanyInfo companyInfo;
    private Timetable timetable;
    private EstimatedCost estimatedCost;
    private String workshopPurpose;
    private Integer visitMonth;
    private Integer numPeople;

    public static AiProposalRequest from(PlanDetailResponse plan) {
        Integer formattedVisitMonth = null;
        if (plan.getVisitMonth() != null && !plan.getVisitMonth().isBlank()) {
            String sanitized = plan.getVisitMonth().replaceAll("[^0-9]", "");
            if (sanitized.length() == 6 && sanitized.startsWith("20")) {
                int value = Integer.parseInt(sanitized);
                int currentYm = Integer.parseInt(java.time.YearMonth.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")));
                if (value < currentYm) {
                    throw new IllegalArgumentException("과거 연월(" + value + ")은 제안서 생성 대상이 될 수 없습니다.");
                }
                formattedVisitMonth = value;
            } else {
                throw new IllegalArgumentException("visitMonth는 20으로 시작하는 6자리 숫자여야 합니다: " + plan.getVisitMonth());
            }
        }

        return builder()
                .planId(plan.getPlanId())
                .companyInfo(new CompanyInfo("Wordiga"))
                .timetable(Timetable.from(plan))
                .estimatedCost(EstimatedCost.from(plan.getEstimatedBudget()))
                .workshopPurpose(null)
                .visitMonth(formattedVisitMonth)
                .numPeople(plan.getParticipantCount())
                .build();
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record CompanyInfo(String companyName) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record EstimatedCost(Long total, Long perPerson, Map<String, Long> breakdown) {
        static EstimatedCost from(PlanDetailResponse.EstimatedCost source) {
            return source == null ? null : new EstimatedCost(
                    source.totalAmount(), source.perPersonAmount(), source.breakdown());
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Timetable(Integer totalDays, Integer totalTravelTimeMin, List<Day> days) {
        static Timetable from(PlanDetailResponse plan) {
            List<Day> days = plan.getDays().stream().map(Day::from).toList();
            int travel = days.stream().flatMap(day -> day.items().stream())
                    .map(Item::travelTimeFromPrevMin).filter(java.util.Objects::nonNull)
                    .mapToInt(Integer::intValue).sum();
            return new Timetable(days.size(), travel, days);
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Day(Integer day, String dateLabel, List<Item> items) {
        static Day from(PlanDetailResponse.Day source) {
            return new Day(source.getDayNumber(), source.getDayNumber() + "일차",
                    source.getContents().stream().map(Item::from).toList());
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Item(Integer order, String startTime, String endTime, String contentId,
                       String title, String category, String address, String firstimage,
                       BigDecimal latitude, BigDecimal longitude,
                       Integer travelTimeFromPrevMin, Integer travelDistanceMeters, String memo) {
        static Item from(PlanDetailResponse.Content source) {
            return new Item(source.getSequence(), time(source.getStartTime()), time(source.getEndTime()),
                    source.getContentId(), source.getTitle(), category(source.getContentTypeId()),
                    source.getAddr1(), source.getThumbnailUrl(), source.getMapy(), source.getMapx(),
                    source.getTravelTimeMinutes(), source.getTravelDistanceMeters(), source.getMemo());
        }

        private static String time(java.time.LocalTime value) {
            return value == null ? "" : value.toString();
        }

        private static String category(String type) {
            if (type == null) return "tourist_spot";
            return switch (type) {
                case "15" -> "event";
                case "28" -> "leports";
                case "32" -> "accommodation";
                case "38" -> "shopping";
                case "39" -> "restaurant";
                default -> "tourist_spot";
            };
        }
    }
}
