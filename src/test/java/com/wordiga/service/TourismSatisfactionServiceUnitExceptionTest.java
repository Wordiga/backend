package com.wordiga.service;

import com.wordiga.dto.ContentDetailDto;
import com.wordiga.global.client.TourismApiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TourismSatisfactionServiceUnitExceptionTest {

    @Mock
    private TourismApiClient tourismApiClient;

    @InjectMocks
    private TourismSatisfactionService service;

    @Test
    void returnsNullWhenEveryPublicDataSourceIsMissing() {
        ContentDetailDto content = new ContentDetailDto();
        content.setTitle("현충사");
        content.setLDongRegnCd("44");
        content.setLDongSignguCd("200");

        assertThat(service.calculate(content, null, java.util.Arrays.asList(null, "invalid", "999S")))
                .isNull();
    }
}
