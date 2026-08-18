package com.wordiga.tourism.service;

import com.wordiga.global.client.dto.DetailInfoDto;
import com.wordiga.global.client.dto.DetailIntroDto;
import com.wordiga.global.client.dto.PhotoGalleryItem;
import com.wordiga.tourism.dto.detail.SeasonalImageDto;
import com.wordiga.tourism.dto.detail.TourismDetailInfoDto;
import com.wordiga.tourism.dto.detail.TourismIntroDetailDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TourismDetailMapperUnitTest {

    private final TourismDetailMapper mapper = new TourismDetailMapper();

    @Test
    void mapsLodgingIntroToPublicFieldNames() {
        DetailIntroDto source = new DetailIntroDto();
        source.setContentid("1");
        source.setContenttypeid("32");
        source.setCheckintime("15:00");
        source.setCheckouttime("11:00");
        source.setRoomcount("30");
        source.setRefundregulation("예약일 기준 환불");

        TourismIntroDetailDto result = mapper.toIntro(source);

        assertThat(result.getContentId()).isEqualTo("1");
        assertThat(result.getCheckInTime()).isEqualTo("15:00");
        assertThat(result.getCheckOutTime()).isEqualTo("11:00");
        assertThat(result.getRoomCount()).isEqualTo("30");
        assertThat(result.getRefundRegulation()).isEqualTo("예약일 기준 환불");
    }

    @Test
    void groupsLodgingRoomImages() {
        DetailInfoDto source = new DetailInfoDto();
        source.setContentid("1");
        source.setContenttypeid("32");
        source.setRoomtitle("스탠다드");
        source.setRoomimg1("https://example.com/1.jpg");
        source.setRoomimg1alt("객실");
        source.setCpyrhtDivCd1("Type1");
        source.setRoomimg2("https://example.com/2.jpg");
        source.setRoomimg3("https://example.com/3.jpg");
        source.setRoomimg4("https://example.com/4.jpg");
        source.setRoomimg5("https://example.com/5.jpg");

        List<TourismDetailInfoDto> result = mapper.toDetails(List.of(source));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getRoomTitle()).isEqualTo("스탠다드");
        assertThat(result.getFirst().getRoomImages()).hasSize(5);
        assertThat(result.getFirst().getRoomImages().getFirst().getImageUrl())
                .isEqualTo("https://example.com/1.jpg");
        assertThat(result.getFirst().getRoomImages().getFirst().getCopyrightTypeCode())
                .isEqualTo("Type1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12", "14", "15", "25", "28", "38", "39", "unknown"})
    void mapsEverySupportedIntroType(String contentTypeId) {
        DetailIntroDto source = new DetailIntroDto();
        source.setContentid("1");
        source.setContenttypeid(contentTypeId);

        TourismIntroDetailDto result = mapper.toIntro(source);

        assertThat(result.getContentTypeId()).isEqualTo(contentTypeId);
    }

    @Test
    void handlesNullAndMalformedDetailValues() {
        assertThat(mapper.toIntro(null)).isNull();
        assertThat(mapper.toDetails(null)).isEmpty();

        DetailInfoDto source = new DetailInfoDto();
        source.setSerialnum("invalid");
        source.setSubnum("");

        assertThat(mapper.toDetails(List.of(source))).singleElement()
                .satisfies(detail -> {
                    assertThat(detail.getSerialNumber()).isNull();
                    assertThat(detail.getSubNumber()).isNull();
                    assertThat(detail.getRoomImages()).isEmpty();
                });
    }

    @Test
    void mapsGalleryShootingDateAndInvalidValues() {
        PhotoGalleryItem exact = new PhotoGalleryItem();
        exact.setGalPhotographyMonth("20260315");
        PhotoGalleryItem invalid = new PhotoGalleryItem();
        invalid.setGalPhotographyMonth("unknown");
        PhotoGalleryItem malformed = new PhotoGalleryItem();
        malformed.setGalPhotographyMonth("20261340");

        assertThat(mapper.toGallerySeasonalImage(exact)).satisfies(image -> {
            assertThat(image.getShootingDate()).isEqualTo("2026-03-15");
            assertThat(image.getSeason()).isEqualTo(SeasonalImageDto.Season.SPRING);
        });
        assertThat(mapper.toGallerySeasonalImage(invalid).getSeason())
                .isEqualTo(SeasonalImageDto.Season.UNKNOWN);
        assertThat(mapper.toGallerySeasonalImage(malformed).getSeason())
                .isEqualTo(SeasonalImageDto.Season.UNKNOWN);
    }
}
