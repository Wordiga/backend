package com.wordiga.client;

import com.wordiga.global.client.KakaoUnlinkClient;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
class KakaoUnlinkClientUnitExceptionTest {
    @Test
    void rejectsMissingAdminKey() {
        KakaoUnlinkClient client = new KakaoUnlinkClient(RestClient.builder(), "", "http://localhost");

        assertThatThrownBy(() -> client.unlink("12345")).hasMessageContaining("503");
    }
}
