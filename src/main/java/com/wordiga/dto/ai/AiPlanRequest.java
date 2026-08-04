package com.wordiga.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordiga.dto.plan.PlanGenerateRequest;
import com.wordiga.dto.tourismContent.detail.TourismContentDetailResponse;
import com.wordiga.dto.tourismContent.detail.TourismIntroDetailDto;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record AiPlanRequest(
        @JsonProperty("visit_month") int visitMonth,
        @JsonProperty("num_people") int numPeople,
        @JsonProperty("num_days") int numDays,
        @JsonProperty("saved_content_ids") List<String> savedContentIds,
        @JsonProperty("saved_contents") List<Content> savedContents,
        @JsonProperty("regional_contents") List<Content> regionalContents,
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
                request.getStartDate().getMonthValue(),
                request.getParticipantCount(),
                (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1,
                request.getSelectedContentIds(),
                savedDetails.stream().map(detail -> Content.from(
                        detail, tagsByContentId.get(detail.getCommon().getContentId()))).toList(),
                regionalDetails.stream().map(detail -> Content.from(
                        detail, tagsByContentId.get(detail.getCommon().getContentId()))).toList(),
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
            @JsonProperty("avg_visit_duration_min") Integer averageVisitDurationMinutes,
            Double dist,
            String eventstartdate,
            String eventenddate) {

        static Content from(TourismContentDetailResponse detail, List<String> tags) {
            var common = detail.getCommon();
            var intro = detail.getIntro();
            return new Content(
                    common.getContentId(), common.getTitle(), category(common.getContentTypeId()),
                    join(common.getAddr1(), common.getAddr2()), common.getMapy(), common.getMapx(),
                    common.getLDongSignguCd(), operatingHours(intro), tags,
                    common.getTel(), truncate(common.getOverview()), common.getFirstImage(), duration(intro), null,
                    intro == null ? null : intro.getEventStartDate(), intro == null ? null : intro.getEventEndDate());
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

        private static Integer duration(TourismIntroDetailDto intro) {
            if (intro == null || intro.getSpendTime() == null) return null;
            var matcher = java.util.regex.Pattern.compile("\\d+").matcher(intro.getSpendTime());
            return matcher.find() ? Integer.valueOf(matcher.group()) : null;
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
                                 @JsonProperty("closed_days") List<String> closedDays, String raw) { }
}
