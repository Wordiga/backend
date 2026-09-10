package com.wordiga.plan.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wordiga.plan.dto.PlanGenerateRequest;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import com.wordiga.tourism.dto.detail.TourismIntroDetailDto;

import java.time.temporal.ChronoUnit;
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
        @JsonProperty("age_groups") List<String> ageGroups,
        @JsonProperty("gender_ratio") Object genderRatio,
        @JsonProperty("monthly_weather") MonthlyWeather monthlyWeather,
        Object preferences) {

    public static AiPlanRequest from(PlanGenerateRequest request, List<TourismContentDetailResponse> details) {
        return from(request, details, List.of(), Map.of());
    }

    public static AiPlanRequest from(PlanGenerateRequest request, List<TourismContentDetailResponse> savedDetails,
                                     List<TourismContentDetailResponse> regionalDetails,
                                     Map<String, List<String>> tagsByContentId) {
        return new AiPlanRequest(
                System.currentTimeMillis(),
                request.getVisitMonth() == null ? request.getStartDate().getMonthValue() : request.getVisitMonth(),
                request.getParticipantCount(),
                (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1,
                request.getSelectedContentIds(),
                savedDetails.stream().map(detail -> Content.from(
                        detail, tagsByContentId.get(detail.getCommon().getContentId()))).toList(),
                regionalDetails.stream().map(detail -> Content.from(
                        detail, tagsByContentId.get(detail.getCommon().getContentId()))).toList(),
                request.getAgeGroups(),
                null,
                MonthlyWeather.from(savedDetails.getFirst().getMonthlyWeather()),
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
            String eventenddate,
            @JsonProperty("is_outdoor") Boolean isOutdoor) {

        static Content from(TourismContentDetailResponse detail, List<String> tags) {
            var common = detail.getCommon();
            var intro = detail.getIntro();
            return new Content(
                    common.getContentId(), common.getTitle(), category(common.getContentTypeId()),
                    join(common.getAddr1(), common.getAddr2()), common.getMapy(), common.getMapx(),
                    common.getLDongSignguCd(), operatingHours(intro), tags == null ? List.of() : tags,
                    common.getTel(), truncate(common.getOverview()), common.getFirstImage(), duration(intro), null,
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
                                 @JsonProperty("closed_days") List<String> closedDays, String raw) {
    }

    public record MonthlyWeather(
            @JsonProperty("target_month") Integer targetMonth,
            @JsonProperty("estimated_average_temperature_celsius") java.math.BigDecimal averageTemperature,
            @JsonProperty("estimated_monthly_precipitation_millimeters") java.math.BigDecimal precipitation,
            @JsonProperty("historical_years") Integer historicalYears,
            String basis) {
        static MonthlyWeather from(com.wordiga.tourism.dto.detail.MonthlyWeatherDto source) {
            return source == null ? null : new MonthlyWeather(source.getTargetMonth(),
                    source.getAvgTemp(),
                    source.getMonthlyPrecipitation(), source.getHistoricalYears(), source.getBasis());
        }
    }
}
