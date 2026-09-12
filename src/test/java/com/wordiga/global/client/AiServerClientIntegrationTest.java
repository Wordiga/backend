package com.wordiga.global.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.wordiga.global.config.AiServerProperties;
import com.wordiga.plan.dto.PlanGenerateRequest;
import com.wordiga.plan.dto.ai.AiPlanRequest;
import com.wordiga.tourism.dto.detail.TourismCommonDetailDto;
import com.wordiga.tourism.dto.detail.TourismContentDetailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiServerClientIntegrationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsTagsOverTcpAndMapsResponse() throws Exception {
        AtomicReference<byte[]> captured = new AtomicReference<>();
        AtomicReference<String> contentLength = new AtomicReference<>();
        AtomicReference<String> transferEncoding = new AtomicReference<>();
        HttpServer server = server(exchange -> {
            captured.set(exchange.getRequestBody().readAllBytes());
            contentLength.set(exchange.getRequestHeaders().getFirst("Content-Length"));
            transferEncoding.set(exchange.getRequestHeaders().getFirst("Transfer-Encoding"));
            byte[] response = """
                    {"plan_id":1,"status":"success","timetable":{"total_days":1,
                    "total_travel_time_min":0,"days":[{"day":1,"date_label":"1일차","items":[{
                    "order":1,"start_time":"10:00","end_time":"11:00","content_id":"129790",
                    "title":"독립기념관","category":"tourist_spot","latitude":36.78,"longitude":127.23,
                    "travel_time_from_prev_min":0,"memo":"관람"}]}]},"warnings":[]}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        try {
            AiPlanRequest request = request(List.of("역사", "기념관"));
            AiServerClient client = client(server);
            client.generatePlan(request);

            JsonNode body = objectMapper.readTree(captured.get());
            assertThat(captured.get()).isEqualTo(objectMapper.writeValueAsBytes(request));
            assertThat(body.at("/saved_contents/0/tags").toString())
                    .isEqualTo("[\"역사\",\"기념관\"]");
            assertThat(contentLength.get()).isEqualTo(String.valueOf(captured.get().length));
            assertThat(transferEncoding.get()).isNull();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void preservesUpstreamFailureAsBadGateway() throws Exception {
        HttpServer server = server(exchange -> {
            byte[] response = "{\"status\":\"error\",\"error_code\":\"LLM_FAILURE\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(502, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        try {
            assertThatThrownBy(() -> client(server).generatePlan(request(List.of())))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("502 BAD_GATEWAY");
        } finally {
            server.stop(0);
        }
    }

    private HttpServer server(com.sun.net.httpserver.HttpHandler handler) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/api/v1/schedule/generate", handler);
        server.start();
        return server;
    }

    private AiServerClient client(HttpServer server) {
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        return new AiServerClient(RestClient.builder(),
                new AiServerProperties(baseUrl, Duration.ofSeconds(2), Duration.ofSeconds(10)), objectMapper);
    }

    private AiPlanRequest request(List<String> tags) {
        PlanGenerateRequest request = new PlanGenerateRequest();
        request.setVisitMonth("2026-09");
        request.setStayDays(1);
        request.setParticipantCount(25);
        request.setSelectedContentIds(List.of("129790"));
        TourismContentDetailResponse detail = TourismContentDetailResponse.builder()
                .common(TourismCommonDetailDto.builder().contentId("129790").contentTypeId("14")
                        .title("독립기념관").addr1("충청남도 천안시").mapx(new java.math.BigDecimal("127.23"))
                        .mapy(new java.math.BigDecimal("36.78")).lDongSignguCd("131").build())
                .build();
        return AiPlanRequest.from(request, List.of(detail), List.of(), Map.of("129790", tags));
    }
}
