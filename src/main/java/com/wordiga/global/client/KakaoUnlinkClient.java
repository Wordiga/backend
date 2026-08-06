package com.wordiga.global.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class KakaoUnlinkClient {
    private final RestClient restClient;
    private final String adminKey;

    public KakaoUnlinkClient(RestClient restClient, @Value("${kakao.admin-key:}") String adminKey) {
        this.restClient = restClient;
        this.adminKey = adminKey;
    }

    public void unlink(String providerId) {
        if (adminKey.isBlank()) unavailable(null);
        var body = new LinkedMultiValueMap<String, String>();
        body.add("target_id_type", "user_id");
        body.add("target_id", providerId);
        try {
            restClient.post().uri("https://kapi.kakao.com/v1/user/unlink")
                    .header("Authorization", "KakaoAK " + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body).retrieve().toBodilessEntity();
        } catch (RuntimeException exception) {
            unavailable(exception);
        }
    }

    private void unavailable(RuntimeException cause) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "카카오 연결을 해제할 수 없습니다.", cause);
    }
}
