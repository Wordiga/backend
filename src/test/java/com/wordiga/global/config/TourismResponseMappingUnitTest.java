package com.wordiga.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.global.client.dto.PhotoGalleryResponse;
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
    void mapsEmptyItemsStringToNull() throws Exception {
        PhotoGalleryResponse result = RestClientConfig.jsonMapper().readValue("""
                {"response":{"header":{"resultCode":"0000"},
                "body":{"items":"","numOfRows":0,"pageNo":1,"totalCount":0}}}
                """, PhotoGalleryResponse.class);

        assertThat(result.getResponse().getBody().getItems()).isNull();
    }
}
