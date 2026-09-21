package com.wordiga.tourism.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TourismContentPolicyUnitTest {
    @Test
    void distinguishesCertainIndoorOutdoorAndUnknownCategories() {
        assertThat(TourismContentPolicy.isOutdoor("14", "VE", "VE07")).isFalse();
        assertThat(TourismContentPolicy.isOutdoor("12", "NA", "NA04")).isTrue();
        assertThat(TourismContentPolicy.isOutdoor("15", "EV", "EV01")).isNull();
        assertThat(TourismContentPolicy.isCamping("AC050100")).isTrue();
        assertThat(TourismContentPolicy.isCamping("AC01")).isFalse();
    }
}
