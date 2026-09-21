package com.wordiga.tourism.service;

import com.wordiga.global.client.dto.ContentDetailDto;
import com.wordiga.global.client.TourismApiClient;
import com.wordiga.global.config.TourismProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourismContentDetailServiceUnitExceptionTest {

    @Mock
    private TourismApiClient tourismApiClient;

    @Spy
    private TourismProperties tourismProperties = tourismProperties();

    @Mock
    private TourismDetailMapper detailMapper;

    @Mock
    private TourismSatisfactionService satisfactionService;

    @InjectMocks
    private TourismContentDetailService service;

    @Test
    void rejectsMissingContent() {
        when(tourismApiClient.fetchCommonDetail("missing")).thenReturn(null);

        assertThatThrownBy(() -> service.getDetail("missing", null, List.of(), null, 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void rejectsContentOutsideChungnam() {
        ContentDetailDto content = new ContentDetailDto();
        content.setLDongRegnCd("11");
        when(tourismApiClient.fetchCommonDetail("126508")).thenReturn(content);

        assertThatThrownBy(() -> service.getDetail("126508", null, List.of(), null, 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void rejectsCampingContent() {
        ContentDetailDto content = new ContentDetailDto();
        content.setLDongRegnCd("44");
        content.setLclsSystm2("AC05");
        when(tourismApiClient.fetchCommonDetail("camping")).thenReturn(content);

        assertThatThrownBy(() -> service.getCommonDetail("camping"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    private static TourismProperties tourismProperties() {
        TourismProperties properties = new TourismProperties();
        properties.getRegion().setChungnamCode("44");
        return properties;
    }
}
