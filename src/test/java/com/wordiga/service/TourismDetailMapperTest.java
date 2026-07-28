package com.wordiga.service;

import com.wordiga.dto.DetailInfoDto;
import com.wordiga.dto.DetailIntroDto;
import com.wordiga.dto.tourismContent.detail.TourismDetailInfoDto;
import com.wordiga.dto.tourismContent.detail.TourismIntroDetailDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TourismDetailMapperTest {

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

        List<TourismDetailInfoDto> result = mapper.toDetails(List.of(source));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getRoomTitle()).isEqualTo("스탠다드");
        assertThat(result.getFirst().getRoomImages()).singleElement()
                .satisfies(image -> {
                    assertThat(image.getImageUrl()).isEqualTo("https://example.com/1.jpg");
                    assertThat(image.getCopyrightTypeCode()).isEqualTo("Type1");
                });
    }
}
