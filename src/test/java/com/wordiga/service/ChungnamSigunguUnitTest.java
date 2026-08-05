package com.wordiga.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChungnamSigunguUnitTest {

    @Test
    void mapsAllTourApiSigunguCodesToTourismDataLabCodes() {
        assertThat(ChungnamSigungu.DATA_LAB_CODES).containsExactlyInAnyOrderEntriesOf(
                java.util.Map.ofEntries(
                        java.util.Map.entry("110", "44131"), java.util.Map.entry("120", "44133"),
                        java.util.Map.entry("150", "44150"), java.util.Map.entry("180", "44180"),
                        java.util.Map.entry("200", "44200"), java.util.Map.entry("210", "44210"),
                        java.util.Map.entry("230", "44230"), java.util.Map.entry("250", "44250"),
                        java.util.Map.entry("270", "44270"), java.util.Map.entry("310", "44710"),
                        java.util.Map.entry("330", "44760"), java.util.Map.entry("340", "44770"),
                        java.util.Map.entry("350", "44790"), java.util.Map.entry("360", "44800"),
                        java.util.Map.entry("370", "44810"), java.util.Map.entry("380", "44825")));
    }
}
