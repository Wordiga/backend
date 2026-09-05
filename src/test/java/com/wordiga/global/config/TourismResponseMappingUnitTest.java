package com.wordiga.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.global.client.dto.AreaBasedItem;
import com.wordiga.global.client.dto.PhotoGalleryResponse;
import com.wordiga.global.client.dto.SigunguItem;
import com.wordiga.global.config.RestClientConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TourismResponseMappingUnitTest {

    private final ObjectMapper objectMapper = new RestClientConfig().objectMapper();

    @Test
    void mapsLowerCamelCaseLegalDistrictCodes() throws Exception {
        ContentDetailDto result = objectMapper.readValue(
                "{\"lDongRegnCd\":\"44\",\"lDongSignguCd\":\"150\"}", ContentDetailDto.class);

        assertThat(result.getLDongRegnCd()).isEqualTo("44");
        assertThat(result.getLDongSignguCd()).isEqualTo("150");
    }

    @Test
    void mapsAreaBasedLegalDistrictCodes() throws Exception {
        AreaBasedItem result = objectMapper.readValue(
                "{\"lDongRegnCd\":\"44\",\"lDongSignguCd\":\"150\"}", AreaBasedItem.class);

        assertThat(result.getLDongRegnCd()).isEqualTo("44");
        assertThat(result.getLDongSignguCd()).isEqualTo("150");
    }

    @Test
    void mapsLowerCamelCaseSigunguFields() throws Exception {
        SigunguItem result = objectMapper.readValue(
                "{\"lDongSignguCd\":\"200\",\"lDongSignguNm\":\"아산시\"}", SigunguItem.class);

        assertThat(result.getCode()).isEqualTo("200");
        assertThat(result.getName()).isEqualTo("아산시");
    }

    @Test
    void mapsEmptyItemsStringToNull() throws Exception {
        PhotoGalleryResponse result = objectMapper.readValue("""
                {"response":{"header":{"resultCode":"0000"},
                "body":{"items":"","numOfRows":0,"pageNo":1,"totalCount":0}}}
                """, PhotoGalleryResponse.class);

        assertThat(result.getResponse().getBody().getItems()).isNull();
    }
}
