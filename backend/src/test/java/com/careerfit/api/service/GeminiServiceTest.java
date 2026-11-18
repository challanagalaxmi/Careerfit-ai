package com.careerfit.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.careerfit.api.dto.GeminiAnalysisResult;
import com.careerfit.api.exception.GeminiServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class GeminiServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void analyzeParsesValidGeminiJson() throws Exception {
        String geminiPayload = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"matchScore\\":82,\\"matchingSkills\\":[\\"Java\\"],\\"missingSkills\\":[\\"Docker\\"],\\"suggestions\\":[\\"Add metrics.\\"],\\"recommendedKeywords\\":[\\"REST API\\"],\\"skillGapRoadmap\\":[{\\"skill\\":\\"Docker\\",\\"priority\\":\\"HIGH\\",\\"reason\\":\\"Required by the role.\\"}]}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        GeminiService service = serviceWithResponse(geminiPayload);

        GeminiAnalysisResult result = service.analyze("Java resume", "Java Docker job");

        assertThat(result.matchScore()).isEqualTo(82);
        assertThat(result.matchingSkills()).containsExactly("Java");
        assertThat(result.missingSkills()).containsExactly("Docker");
        assertThat(result.skillGapRoadmap()).hasSize(1);
        assertThat(result.skillGapRoadmap().get(0).priority()).isEqualTo("HIGH");
    }

    @Test
    void analyzeRejectsMalformedGeminiJson() throws Exception {
        GeminiService service = serviceWithResponse("""
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          { "text": "{not-json}" }
                        ]
                      }
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> service.analyze("resume", "job"))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("malformed JSON");
    }

    @Test
    void analyzeFailsCleanlyWhenApiKeyMissing() {
        GeminiService service = new GeminiService(
                RestClient.builder(),
                new ObjectMapper(),
                "http://localhost:1",
                "",
                "gemini-test"
        );

        assertThatThrownBy(() -> service.analyze("resume", "job"))
                .isInstanceOf(GeminiServiceException.class)
                .hasMessageContaining("Gemini API key is not configured");
    }

    private GeminiService serviceWithResponse(String responseBody) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1beta/models/gemini-test:generateContent", exchange -> {
            byte[] requestBody = exchange.getRequestBody().readAllBytes();
            String requestText = new String(requestBody, StandardCharsets.UTF_8);
            String apiKeyHeader = exchange.getRequestHeaders().getFirst("x-goog-api-key");

            if (!"test-key".equals(apiKeyHeader) || !requestText.contains("responseMimeType")) {
                exchange.sendResponseHeaders(400, 0);
                exchange.close();
                return;
            }

            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream body = exchange.getResponseBody()) {
                body.write(bytes);
            }
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort() + "/v1beta";
        return new GeminiService(RestClient.builder(), new ObjectMapper(), baseUrl, "test-key", "gemini-test");
    }
}
