package com.wordiga.global.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
public class KakaoUnlinkClient {
    private final RestClient restClient;
    private final String adminKey;
    private final String unlinkUrl;

    public KakaoUnlinkClient(
            RestClient restClient,
            @Value("${kakao.admin-key:}") String adminKey,
            @Value("${kakao.url.unlink}") String unlinkUrl) {
        this.restClient = restClient;
        this.adminKey = adminKey;
        this.unlinkUrl = unlinkUrl;
    }

    public void unlink(String providerId) {
        if (adminKey.isBlank()) unavailable(null);
        var body = new LinkedMultiValueMap<String, String>();
        body.add("target_id_type", "user_id");
        body.add("target_id", providerId);
        try {
            restClient.post().uri(unlinkUrl)
                    .header("Authorization", "KakaoAK " + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body).retrieve().toBodilessEntity();
        } catch (RuntimeException exception) {
            unavailable(exception);
        }
    }

    private void unavailable(RuntimeException cause) {
        log.error("[KakaoUnlinkClient] 카카오 연동 해제 중 오류 발생: {}", cause != null ? cause.getMessage() : "Unknown", cause);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "카카오 연결을 해제할 수 없습니다.", cause);
    }
}
