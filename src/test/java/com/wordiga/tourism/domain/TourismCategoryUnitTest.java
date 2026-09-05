package com.wordiga.tourism.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TourismCategoryUnitTest {

    @Test
    void resolvesCategoryByPolicyLevel() {
        assertThat(TourismCategory.resolve("EV", "EV01", "EV010100"))
                .isEqualTo(TourismCategory.FESTIVAL);
        assertThat(TourismCategory.resolve("AC", "AC02", "AC020100"))
                .isEqualTo(TourismCategory.CONDOMINIUM);
        assertThat(TourismCategory.resolve("FD", "FD02", "FD020300"))
                .isEqualTo(TourismCategory.WESTERN);
        assertThat(TourismCategory.resolve("FD", "FD05", "FD050100"))
                .isEqualTo(TourismCategory.CAFE);
        assertThat(TourismCategory.resolve("CO", "CO01", "CO010100")).isNull();
    }
}
