package com.wordiga.client;

import com.wordiga.dto.ai.AiPlanRequest;
import com.wordiga.dto.ai.AiPlanResponse;
import com.wordiga.dto.ai.AiProposalRequest;
import com.wordiga.global.config.AiServerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;

@Component
public class AiServerClient {
    private final RestClient client;

    public AiServerClient(AiServerProperties p) {
        HttpClient http = HttpClient.newBuilder().connectTimeout(p.connectTimeout()).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(p.readTimeout());
        this.client = RestClient.builder().baseUrl(p.baseUrl()).requestFactory(factory).build();
    }

    public AiPlanResponse generatePlan(AiPlanRequest request) {
        try {
            AiPlanResponse response = client.post().uri("/api/v1/schedule/generate")
                    .contentType(MediaType.APPLICATION_JSON).body(request).retrieve().body(AiPlanResponse.class);
            if (response == null) throw invalid();
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 서버를 사용할 수 없습니다.", e);
        }
    }

    public byte[] generateProposal(AiProposalRequest request) {
        try {
            byte[] response = client.post().uri("/internal/v1/proposals")
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(request).retrieve().body(byte[].class);
            if (response == null) throw invalid();
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI 서버를 사용할 수 없습니다.", e);
        }
    }

    private ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 응답이 올바르지 않습니다.");
    }
}
