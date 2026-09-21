package com.wordiga.plan.dto;

import java.util.List;

public record RainAlternativeResponse(Long planId, List<Source> items) {
    public record Source(String sourceContentId, Boolean isOutdoor, List<Candidate> alternatives) {
    }

    public record Candidate(String contentId, String title, String thumbnailUrl, Integer distanceMeters) {
    }
}
