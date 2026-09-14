package com.wordiga.global.client;


import com.wordiga.global.config.AiServerProperties;
import com.wordiga.plan.dto.ai.AiPlanRequest;
import com.wordiga.plan.dto.ai.AiPlanResponse;
import com.wordiga.proposal.dto.AiProposalRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class AiServerClient {

    private final RestClient client;
    private final ObjectMapper objectMapper;

    public AiServerClient(RestClient.Builder restClientBuilder, AiServerProperties properties,
                          ObjectMapper objectMapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());

        this.client = restClientBuilder
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
        this.objectMapper = objectMapper;
    }

    public AiPlanResponse generatePlan(AiPlanRequest request) {
        try {
            byte[] body = serialize(request);
            AiPlanResponse response = client.post()
                    .uri("/api/v1/schedule/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(body.length)
                    .body(outputStream -> outputStream.write(body))
                    .retrieve()
                    .body(AiPlanResponse.class);
            if (response == null) throw invalid("일정 계획 응답이 비어 있습니다.");
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw upstream(e, "일정 생성");
        } catch (Exception e) {
            log.error("[AiServerClient] AI 서버 통신 중 오류 발생 (일정 생성): {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버를 사용할 수 없습니다.", e);
        }
    }

    public byte[] generateProposal(AiProposalRequest request) {
        try {
            byte[] body = serialize(request);
            byte[] response = client.post()
                    .uri(uriBuilder -> uriBuilder.path("/api/v1/proposal/generate")
                            .queryParam("format", "docx").build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(body.length)
                    .body(outputStream -> outputStream.write(body))
                    .exchange((req, res) -> {
                        if (res.getStatusCode().isError()) {
                            throw new RestClientResponseException(
                                    "AI 서버 제안서 생성 실패",
                                    res.getStatusCode().value(),
                                    res.getStatusText(),
                                    res.getHeaders(),
                                    res.getBody().readAllBytes(),
                                    null
                            );
                        }
                        return res.getBody().readAllBytes();
                    });
            if (response == null || response.length == 0) throw invalid("제안서 문서 응답이 비어 있습니다.");
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw upstream(e, "제안서 생성");
        } catch (Exception e) {
            log.error("[AiServerClient] AI 서버 통신 중 오류 발생 (제안서 생성): {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버를 사용할 수 없습니다.", e);
        }
    }

    private ResponseStatusException invalid(String reason) {
        log.error("[AiServerClient] 유효하지 않은 AI 응답: {}", reason);
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 응답이 올바르지 않습니다.");
    }

    private byte[] serialize(Object request) throws JacksonException {
        return objectMapper.writeValueAsBytes(request);
    }

    private ResponseStatusException upstream(RestClientResponseException exception, String operation) {
        int upstreamStatus = exception.getStatusCode().value();
        log.error("[AiServerClient] AI 서버 {} 실패: status={}, body={}",
                operation, upstreamStatus, exception.getResponseBodyAsString(), exception);
        if (upstreamStatus == HttpStatus.GATEWAY_TIMEOUT.value())
            return new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI 서버의 처리 시간이 초과되었습니다.", exception);
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 서버가 " + operation + "에 실패했습니다.", exception);
    }

}
