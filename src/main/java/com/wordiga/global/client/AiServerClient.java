package com.wordiga.global.client;

import com.wordiga.dto.ai.AiPlanRequest;
import com.wordiga.dto.ai.AiPlanResponse;
import com.wordiga.dto.ai.AiProposalRequest;
import com.wordiga.global.config.AiServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.http.HttpClient;

@Component
@Slf4j
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
            if (response == null) throw invalid("일정 계획 응답이 비어 있습니다.");
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("[AiServerClient] AI 서버 통신 중 오류 발생 (일정 생성): {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버를 사용할 수 없습니다.", e);
        }
    }

    public byte[] generateProposal(AiProposalRequest request) {
        try {
            byte[] response = client.post().uri("/internal/v1/proposals")
                    .contentType(MediaType.APPLICATION_JSON).accept(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .body(request).retrieve().body(byte[].class);
            if (response == null) throw invalid("제안서 문서 응답이 비어 있습니다.");
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("[AiServerClient] AI 서버 통신 중 오류 발생 (제안서 생성): {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버를 사용할 수 없습니다.", e);
        }
    }

    private ResponseStatusException invalid(String reason) {
        log.error("[AiServerClient] 유효하지 않은 AI 응답: {}", reason);
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 응답이 올바르지 않습니다.");
    }
}
