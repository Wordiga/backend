package com.wordiga.tourism.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum TourismCategory {
    FESTIVAL("EV", "축제/공연/행사", TourismTheme.ATTRACTION_EXPERIENCE, Level.LARGE),
    EXPERIENCE("EX", "체험관광", TourismTheme.ATTRACTION_EXPERIENCE, Level.LARGE),
    HISTORY("HS", "역사관광", TourismTheme.ATTRACTION_EXPERIENCE, Level.LARGE),
    LEISURE_SPORTS("LS", "레저스포츠", TourismTheme.ATTRACTION_EXPERIENCE, Level.LARGE),
    NATURE("NA", "자연관광", TourismTheme.ATTRACTION_EXPERIENCE, Level.LARGE),
    SHOPPING("SH", "쇼핑", TourismTheme.ATTRACTION_EXPERIENCE, Level.LARGE),
    CULTURE("VE", "문화관광", TourismTheme.ATTRACTION_EXPERIENCE, Level.LARGE),

    HOTEL("AC01", "호텔", TourismTheme.LODGING, Level.MIDDLE),
    CONDOMINIUM("AC02", "콘도미니엄", TourismTheme.LODGING, Level.MIDDLE),
    PENSION_HOMESTAY("AC03", "펜션/민박", TourismTheme.LODGING, Level.MIDDLE),
    MOTEL("AC04", "모텔", TourismTheme.LODGING, Level.MIDDLE),
    CAMPING("AC05", "캠핑", TourismTheme.LODGING, Level.MIDDLE),
    HOSTEL("AC06", "호스텔", TourismTheme.LODGING, Level.MIDDLE),

    KOREAN("FD01", "한식", TourismTheme.FOOD_CAFE, Level.MIDDLE),
    CHINESE("FD020100", "중식", TourismTheme.FOOD_CAFE, Level.SMALL),
    JAPANESE("FD020200", "일식", TourismTheme.FOOD_CAFE, Level.SMALL),
    WESTERN("FD020300", "양식", TourismTheme.FOOD_CAFE, Level.SMALL),
    OTHER_FOREIGN("FD020400", "기타외국식", TourismTheme.FOOD_CAFE, Level.SMALL),
    FUSION("FD020500", "퓨전음식", TourismTheme.FOOD_CAFE, Level.SMALL),
    SNACK("FD03", "간이음식", TourismTheme.FOOD_CAFE, Level.MIDDLE),
    BAR("FD04", "주점", TourismTheme.FOOD_CAFE, Level.MIDDLE),
    CAFE("FD05", "카페", TourismTheme.FOOD_CAFE, Level.MIDDLE);

    private final String code;
    private final String displayName;
    private final TourismTheme theme;
    private final Level level;

    public static TourismCategory resolve(String large, String middle, String small) {
        return Arrays.stream(values())
                .filter(category -> category.code.equals(category.level.value(large, middle, small)))
                .findFirst()
                .orElse(null);
    }

    private enum Level {
        LARGE { String value(String large, String middle, String small) { return large; } },
        MIDDLE { String value(String large, String middle, String small) { return middle; } },
        SMALL { String value(String large, String middle, String small) { return small; } };

        abstract String value(String large, String middle, String small);
    }
}
