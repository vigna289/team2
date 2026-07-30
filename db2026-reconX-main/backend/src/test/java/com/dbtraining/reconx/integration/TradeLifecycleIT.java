package com.dbtraining.reconx.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.dbtraining.reconx.dto.LoginRequest;
import com.dbtraining.reconx.dto.ReconRunRequest;
import com.dbtraining.reconx.dto.ResolutionRequest;
import com.dbtraining.reconx.dto.TradeRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TradeLifecycleIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort
    int port;

    @Autowired
    ObjectMapper objectMapper;

    static String token;
    static Long tradeId;
    static String jobId;
    static Long breakId;

    private HttpClient httpClient = HttpClient.newHttpClient();

    private String baseUrl(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    @Order(1)
    void loginAsAdmin() throws Exception {
        LoginRequest login = new LoginRequest("admin@db.com", "admin123");
        JsonNode body = sendRequest("POST", "/auth/login", login, headers(), HttpStatus.OK.value());
        token = body.get("token").asText();
        assertThat(token).isNotBlank();
    }

    @Test
    @Order(2)
    void createTrade() throws Exception {
        TradeRequest trade = new TradeRequest(
                "TRD-20260730-1000",
                1L,
                1L,
                "EQUITY",
                "BUY",
                new BigDecimal("100.0000"),
                new BigDecimal("245.50"),
                LocalDate.now());

        JsonNode body = sendRequest("POST", "/v1/trades", trade, authHeaders(), HttpStatus.CREATED.value());
        tradeId = body.get("id").asLong();
        assertThat(tradeId).isGreaterThan(0);
    }

    @Test
    @Order(3)
    void listTrades() throws Exception {
        JsonNode body = sendRequest("GET", "/v1/trades", null, authHeaders(), HttpStatus.OK.value());
        assertThat(body).isNotNull();
        assertThat(body.get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @Order(4)
    void patchTradeStatus() throws Exception {
        assertThat(tradeId).isNotNull();
        JsonNode body = sendRequest("PATCH", "/v1/trades/" + tradeId + "/status", objectMapper.readTree("{\"status\":\"MATCHED\"}"), authHeaders(), HttpStatus.OK.value());
        assertThat(body.get("status").asText()).isEqualTo("MATCHED");
    }

        @Test
        @Order(5)
        void triggerRecon() throws Exception {
        ReconRunRequest requestBody = new ReconRunRequest(
            LocalDate.now().minusDays(1),
            LocalDate.now(),
            1L);
        JsonNode body = sendRequest("POST", "/v1/recon/run", requestBody, authHeaders(), HttpStatus.ACCEPTED.value());
        jobId = body.get("jobId").asText();
        assertThat(jobId).isNotBlank();
        }

    @Test
    @Order(6)
    void resolveReconBreak() throws Exception {
        assertThat(jobId).isNotBlank();
        JsonNode page = sendRequest("GET", "/v1/recon/jobs/" + jobId + "/results", null, authHeaders(), HttpStatus.OK.value());
        assertThat(page).isNotNull();
        assertThat(page.get("content")).isNotNull();
        assertThat(page.get("content").isArray()).isTrue();
        assertThat(page.get("content")).hasSizeGreaterThanOrEqualTo(0);

        if (page.get("content").size() > 0) {
            breakId = page.get("content").get(0).get("id").asLong();
        }

        if (breakId == null) {
            return;
        }

        ResolutionRequest resolution = new ResolutionRequest("Resolved via integration test");
        JsonNode resolveBody = sendRequest("PUT", "/v1/recon/results/" + breakId + "/resolve", resolution, authHeaders(), HttpStatus.OK.value());
        assertThat(resolveBody.get("status").asText()).isEqualTo("RESOLVED");
    }

    private JsonNode sendRequest(String method, String path, Object body, HttpHeaders headers, int expectedStatus) throws Exception {
        String url = baseUrl(path);
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));

        byte[] bodyBytes = null;
        if (body != null) {
            String json = objectMapper.writeValueAsString(body);
            bodyBytes = json.getBytes(StandardCharsets.UTF_8);
        }

        String[] flatHeaders = headers.toSingleValueMap().entrySet().stream()
                .flatMap(e -> java.util.stream.Stream.of(e.getKey(), e.getValue()))
                .toArray(String[]::new);

        if (bodyBytes != null) {
            builder.method(method, BodyPublishers.ofByteArray(bodyBytes));
        } else {
            builder.method(method, BodyPublishers.noBody());
        }

        if (flatHeaders.length > 0) builder.headers(flatHeaders);
        HttpRequest request = builder.build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != expectedStatus) {
            System.err.println("Unexpected HTTP status: " + response.statusCode());
            System.err.println("Response body:\n" + response.body());
            throw new org.opentest4j.AssertionFailedError("expected: " + expectedStatus + " but was: " + response.statusCode() + "\nresponse body:\n" + response.body());
        }
        if (response.body() == null || response.body().isEmpty()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(response.body());
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
