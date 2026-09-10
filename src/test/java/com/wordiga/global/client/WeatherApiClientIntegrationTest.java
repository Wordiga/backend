package com.wordiga.global.client;

import com.sun.net.httpserver.HttpServer;
import com.wordiga.global.config.WeatherProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherApiClientIntegrationTest {

    @Test
    void decodesConfiguredServiceKeyBeforeBuildingQuery() throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/getWthrDataList", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] response = """
                    {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
                    "body":{"items":{"item":[]}}}}
                    """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            WeatherApiClient client = new WeatherApiClient(RestClient.builder(),
                    new WeatherProperties(baseUrl, "encoded%2Bkey%3D", 5));

            client.daily("232", LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 31));

            assertThat(query.get()).contains("serviceKey=encoded%2Bkey%3D")
                    .doesNotContain("%252B", "%253D");
        } finally {
            server.stop(0);
        }
    }
}
