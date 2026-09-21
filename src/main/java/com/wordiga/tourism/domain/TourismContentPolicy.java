package com.wordiga.tourism.domain;

import java.util.Set;

public final class TourismContentPolicy {
    private static final Set<String> INDOOR_CODES = Set.of("VE06", "VE07", "EX02");

    private TourismContentPolicy() {
    }

    public static boolean isCamping(String middleCode) {
        return middleCode != null && middleCode.startsWith("AC05");
    }

    public static boolean isIndoorAlternative(String middleCode) {
        return middleCode != null && INDOOR_CODES.contains(middleCode);
    }

    public static Boolean isOutdoor(String contentTypeId, String largeCode, String middleCode) {
        if (isIndoorAlternative(middleCode) || "32".equals(contentTypeId) && !isCamping(middleCode)
                || "39".equals(contentTypeId)) return false;
        if ("NA".equals(largeCode) || "LS".equals(largeCode)) return true;
        return null;
    }
}
