package com.wordiga.client;

import com.wordiga.global.client.KakaoUnlinkClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class KakaoUnlinkClientUnitExceptionTest {
    @Test
    void rejectsMissingAdminKey() {
        KakaoUnlinkClient client = new KakaoUnlinkClient(mock(RestClient.class), "", "");

        assertThatThrownBy(() -> client.unlink("12345")).hasMessageContaining("503");
    }
}
