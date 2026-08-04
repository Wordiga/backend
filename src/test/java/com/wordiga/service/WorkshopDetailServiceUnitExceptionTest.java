package com.wordiga.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkshopDetailServiceUnitExceptionTest {
    @Test void convertsMalformedTourismResponseToServiceUnavailable() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        WorkshopDetailService service = new WorkshopDetailService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(service, "baseUrl", "https://example.com");
        ReflectionTestUtils.setField(service, "serviceKey", "key");
        when(restTemplate.exchange(any(java.net.URI.class), any(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("not-json"));

        assertThatThrownBy(() -> service.fetchCommonDetail("1"))
                .hasMessageContaining("503");
    }
}
