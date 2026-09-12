package com.wordiga.plan.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordiga.plan.dto.PlanGenerateRequest;
import com.wordiga.plan.service.PlanCostPolicy;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import com.wordiga.tourism.dto.detail.TourismIntroDetailDto;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record AiPlanRequest(
        @JsonProperty("plan_id") long planId,
        @JsonProperty("visit_month") int visitMonth,
        @JsonProperty("num_people") int numPeople,
        @JsonProperty("num_days") int numDays,
        @JsonProperty("saved_content_ids") List<String> savedContentIds,
        @JsonProperty("saved_contents") List<Content> savedContents,
        @JsonProperty("regional_contents") List<Content> regionalContents,
        @JsonProperty("estimated_budget") EstimatedBudget estimatedBudget,
        @JsonProperty("age_groups") List<String> ageGroups,
        @JsonProperty("gender_ratio") Object genderRatio,
        Object preferences) {

    public static AiPlanRequest from(PlanGenerateRequest request, List<TourismContentDetailResponse> details) {
        return from(request, details, List.of(), Map.of());
    }

    public static AiPlanRequest from(PlanGenerateRequest request, List<TourismContentDetailResponse> savedDetails,
                                     List<TourismContentDetailResponse> regionalDetails,
                                     Map<String, List<String>> tagsByContentId) {
        return new AiPlanRequest(
                System.currentTimeMillis(),
                YearMonth.parse(request.getVisitMonth()).getMonthValue(),
                request.getParticipantCount(),
                request.getStayDays(),
                request.getSelectedContentIds(),
                savedDetails.stream().map(detail -> Content.from(detail,
                        tagsByContentId.get(detail.getCommon().getContentId()), request.getParticipantCount())).toList(),
                regionalDetails.stream().map(detail -> Content.from(detail,
                        tagsByContentId.get(detail.getCommon().getContentId()), request.getParticipantCount())).toList(),
                budget(savedDetails, request.getParticipantCount()),
                request.getAgeGroups(),
                null,
                null);
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Content(
            @JsonProperty("content_id") String contentId,
            String title,
            String category,
            String address,
            java.math.BigDecimal latitude,
            java.math.BigDecimal longitude,
            @JsonProperty("sigungu_code") String sigunguCode,
            @JsonProperty("operating_hours") OperatingHours operatingHours,
            List<String> tags,
            String tel,
            String overview,
            String firstimage,
            String thumbnail,
            @JsonProperty("estimated_cost") PlanCostPolicy.Estimate estimatedCost,
            Double dist,
            String eventstartdate,
            String eventenddate,
            @JsonProperty("is_outdoor") Boolean isOutdoor) {

        static Content from(TourismContentDetailResponse detail, List<String> tags, int participantCount) {
            var common = detail.getCommon();
            var intro = detail.getIntro();
            return new Content(
                    common.getContentId(), common.getTitle(), category(common.getContentTypeId()),
                    join(common.getAddr1(), common.getAddr2()), common.getMapy(), common.getMapx(),
                    common.getLDongSignguCd(), operatingHours(intro), tags == null ? List.of() : tags,
                    common.getTel(), truncate(common.getOverview()), common.getFirstImage(), common.getFirstImage(),
                    PlanCostPolicy.estimate(detail, participantCount), null,
                    intro == null ? null : intro.getEventStartDate(), intro == null ? null : intro.getEventEndDate(),
                    outdoor(tags));
        }

        private static Boolean outdoor(List<String> tags) {
            if (tags == null || tags.isEmpty()) return null;
            String joined = String.join(" ", tags);
            if (java.util.stream.Stream.of("실내", "박물관", "미술관", "전시", "공연", "숙박", "음식점")
                    .anyMatch(joined::contains)) return false;
            if (java.util.stream.Stream.of("자연", "야외", "해수욕장", "산", "공원", "레포츠", "축제")
                    .anyMatch(joined::contains)) return true;
            return null;
        }

        private static String category(String type) {
            return switch (type) {
                case "12", "14", "25" -> "tourist_spot";
                case "15" -> "event";
                case "28" -> "leports";
                case "32" -> "accommodation";
                case "38" -> "shopping";
                case "39" -> "restaurant";
                default -> "tourist_spot";
            };
        }

        private static OperatingHours operatingHours(TourismIntroDetailDto intro) {
            if (intro == null) return null;
            String raw = first(intro.getUseTime(), intro.getOpenTime(), intro.getCheckInTime());
            return raw == null && intro.getRestDate() == null ? null
                    : new OperatingHours(null, null, List.of(), join(raw, intro.getRestDate()));
        }


        private static String first(String... values) {
            return java.util.Arrays.stream(values).filter(value -> value != null && !value.isBlank()).findFirst().orElse(null);
        }

        private static String join(String first, String second) {
            return java.util.stream.Stream.of(first, second).filter(value -> value != null && !value.isBlank())
                    .collect(java.util.stream.Collectors.joining(" "));
        }

        private static String truncate(String value) {
            return value == null || value.length() <= 200 ? value : value.substring(0, 200);
        }
    }

    public record OperatingHours(String open, String close,
                                 @JsonProperty("closed_days") List<String> closedDays, String raw) {
    }

    public record EstimatedBudget(long total, @JsonProperty("per_person") long perPerson,
                                  String currency, Map<String, Long> breakdown) {
    }

    private static EstimatedBudget budget(List<TourismContentDetailResponse> details, int participants) {
        var estimates = details.stream().map(detail -> PlanCostPolicy.estimate(detail, participants)).toList();
        long total = estimates.stream().mapToLong(PlanCostPolicy.Estimate::calculatedAmount).sum();
        Map<String, Long> breakdown = estimates.stream().collect(java.util.stream.Collectors.groupingBy(
                PlanCostPolicy.Estimate::category, java.util.LinkedHashMap::new,
                java.util.stream.Collectors.summingLong(PlanCostPolicy.Estimate::calculatedAmount)));
        return new EstimatedBudget(total, participants == 0 ? 0 : total / participants, "KRW", breakdown);
    }

}
