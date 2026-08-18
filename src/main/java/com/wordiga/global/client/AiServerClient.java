package com.wordiga.global.client;

import com.wordiga.plan.dto.ai.AiPlanRequest;
import com.wordiga.plan.dto.ai.AiPlanResponse;
import com.wordiga.proposal.dto.AiProposalRequest;
import com.wordiga.global.config.AiServerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;

@Component
@Slf4j
public class AiServerClient {

    private final AiHttpExchangeClient client;

    public AiServerClient(RestClient.Builder restClientBuilder, AiServerProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(properties.readTimeout());

        RestClient restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();

        HttpServiceProxyFactory proxyFactory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        this.client = proxyFactory.createClient(AiHttpExchangeClient.class);
    }

    public AiPlanResponse generatePlan(AiPlanRequest request) {
        try {
            AiPlanResponse response = client.generatePlan(request);
            if (response == null) throw invalid("일정 계획 응답이 비어 있습니다.");
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AiServerClient] AI 서버 통신 중 오류 발생 (일정 생성): {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버를 사용할 수 없습니다.", e);
        }
    }

    public byte[] generateProposal(AiProposalRequest request) {
        try {
            byte[] response = client.generateProposal("docx", request);
            if (response == null) throw invalid("제안서 문서 응답이 비어 있습니다.");
            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[AiServerClient] AI 서버 통신 중 오류 발생 (제안서 생성): {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서버를 사용할 수 없습니다.", e);
        }
    }

    private ResponseStatusException invalid(String reason) {
        log.error("[AiServerClient] 유효하지 않은 AI 응답: {}", reason);
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 응답이 올바르지 않습니다.");
    }

    @HttpExchange
    private interface AiHttpExchangeClient {

        @PostExchange(value = "/api/v1/schedule/generate", contentType = MediaType.APPLICATION_JSON_VALUE)
        AiPlanResponse generatePlan(@RequestBody AiPlanRequest request);

        @PostExchange(value = "/api/v1/proposal/generate", contentType = MediaType.APPLICATION_JSON_VALUE)
        byte[] generateProposal(
                @org.springframework.web.bind.annotation.RequestParam("format") String format,
                @RequestBody AiProposalRequest request
        );
    }
}
