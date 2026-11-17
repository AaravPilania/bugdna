package com.bugdna.ai;

import com.bugdna.ai.AiAnalysisDtos.AiStructuredOutput;
import com.bugdna.ai.AiAnalysisDtos.SanitizedBugInput;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions provider with structured JSON output and 20s timeout.
 * API key is sent strictly via Authorization header and never logged.
 */
@Component
public class OpenAiProvider implements AiProvider {

    private static final String SYSTEM_PROMPT = """
            You are BugDNA, a strict evidence-grounded debugging analyst.
            Return ONLY valid JSON with exact keys:
            {
              "summary": "string",
              "evidence": [{"statement": "string", "eventIds": ["exact eventId from timeline"]}],
              "hypotheses": [{"cause": "string", "confidence": "low|medium|high", "reasoning": "string"}],
              "debuggingSteps": ["string"],
              "reproductionSteps": ["string"]
            }
            Every evidence item MUST cite exact eventId values from the provided timeline. Never fabricate IDs.
            """;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${bugdna.ai.openai-key:}")
    private String apiKey;

    public OpenAiProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public boolean isMock() {
        return false;
    }

    public boolean hasKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    public AiStructuredOutput analyze(SanitizedBugInput input, String promptJson, String model) {
        if (!hasKey()) {
            throw new IllegalStateException("OPENAI_API_KEY is not configured");
        }

        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", promptJson)
                )
        );

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("OpenAI API returned HTTP " + resp.statusCode());
            }

            JsonNode root = objectMapper.readTree(resp.body());
            String content = root.path("choices").path(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new RuntimeException("OpenAI returned empty analysis content");
            }
            return objectMapper.readValue(content, AiStructuredOutput.class);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("OpenAI request interrupted");
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            throw new RuntimeException("OpenAI analysis call failed: " + ex.getClass().getSimpleName());
        }
    }
}
