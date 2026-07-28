package com.wordiga.service;

import com.wordiga.client.TourismApiClient;
import com.wordiga.config.TourismProperties;
import com.wordiga.dto.tourismContent.ListType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TourismContentServiceUnitExceptionTest {

    @Mock
    private TourismApiClient tourismApiClient;

    private TourismContentService service;

    @BeforeEach
    void setUp() {
        TourismProperties properties = new TourismProperties();
        properties.getRegion().setChungnamCode("44");
        service = new TourismContentService(tourismApiClient, properties);
    }

    @Test
    void returnsEmptyPageWhenPublicApiResponseIsMissing() {
        assertThat(service.getContentList(
                ListType.POPULAR, null, null, null, null, 0, 20).getItems())
                .isEmpty();
    }

    @Test
    void returnsEmptySearchPageWhenResponseIsMissing() {
        assertThat(service.getContentList(
                ListType.POPULAR, null, "unknown", null, null, 0, 20).getItems())
                .isEmpty();
    }
}
