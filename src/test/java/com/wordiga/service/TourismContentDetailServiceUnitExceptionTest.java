package com.wordiga.service;

import com.wordiga.dto.ContentDetailDto;
import com.wordiga.global.client.TourismApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TourismContentDetailServiceUnitExceptionTest {

    @Mock
    private TourismApiClient tourismApiClient;

    @Mock
    private TourismDetailMapper detailMapper;

    @Mock
    private TourismSatisfactionService satisfactionService;

    @InjectMocks
    private TourismContentDetailService service;

    @Test
    void rejectsMissingContent() {
        when(tourismApiClient.fetchCommonDetail("missing")).thenReturn(null);

        assertThatThrownBy(() -> service.getDetail("missing", null, List.of(), 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void rejectsContentOutsideChungnam() {
        ContentDetailDto content = new ContentDetailDto();
        content.setLDongRegnCd("11");
        when(tourismApiClient.fetchCommonDetail("126508")).thenReturn(content);

        assertThatThrownBy(() -> service.getDetail("126508", null, List.of(), 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }
}
