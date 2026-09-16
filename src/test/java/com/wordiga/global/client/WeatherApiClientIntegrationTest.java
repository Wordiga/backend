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
    void readsOneMonthOfAwsDailyDataWithApiHubAuthKey() throws Exception {
        AtomicReference<String> query = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/getDailyAwsData", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            byte[] response = """
                    {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
                    "body":{"items":{"item":[{"awsmdays":{"stn_id":"634","stn_ko":"아산","info":[
                    {"tm":"01","ta_min":"15.0","ta_max":"25.0","rn_day":"3.5"}]}}]}}}}
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
                    new WeatherProperties(baseUrl, "api-key"));

            var observations = client.daily("634", 2025, 9);

            assertThat(query.get()).contains("authKey=api-key", "year=2025", "month=09", "station=634");
            assertThat(observations).singleElement().satisfies(observation -> {
                assertThat(observation.getMinTemperature()).isEqualTo("15.0");
                assertThat(observation.getDailyPrecipitation()).isEqualTo("3.5");
            });
        } finally {
            server.stop(0);
        }
    }
}
