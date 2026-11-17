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
 * Gemini REST API provider using generateContent with strict JSON responseSchema.
 * API key is sent strictly via the x-goog-api-key header (never in URL or logs).
 */
@Component
public class GeminiProvider implements AiProvider {

    private static final String SYSTEM_INSTRUCTION = """
            You are BugDNA, a strict evidence-grounded debugging analyst.
            Analyze ONLY the provided sanitized bug metadata and session timeline JSON.
            Rules:
            1. Every item in "evidence" MUST be an objective fact directly observed in the timeline and MUST cite exact "eventId" values from the input timeline.
            2. Every item in "hypotheses" MUST be framed as a plausible guess to verify, with confidence ("low", "medium", or "high") and clear reasoning.
            3. Never invent eventIds, stack frames, or user data.
            Return strictly valid JSON matching the requested schema.
            """;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${bugdna.ai.gemini-key:}")
    private String apiKey;

    public GeminiProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    @Override
    public String getProviderName() {
        return "gemini";
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
            throw new IllegalStateException("GEMINI_API_KEY is not configured");
        }

        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";

        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "summary", Map.of("type", "STRING"),
                        "evidence", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "statement", Map.of("type", "STRING"),
                                                "eventIds", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
                                        ),
                                        "required", List.of("statement", "eventIds")
                                )
                        ),
                        "hypotheses", Map.of(
                                "type", "ARRAY",
                                "items", Map.of(
                                        "type", "OBJECT",
                                        "properties", Map.of(
                                                "cause", Map.of("type", "STRING"),
                                                "confidence", Map.of("type", "STRING"),
                                                "reasoning", Map.of("type", "STRING")
                                        ),
                                        "required", List.of("cause", "confidence", "reasoning")
                                )
                        ),
                        "debuggingSteps", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                        "reproductionSteps", Map.of("type", "ARRAY", "items", Map.of("type", "STRING"))
                ),
                "required", List.of("summary", "evidence", "hypotheses", "debuggingSteps", "reproductionSteps")
        );

        Map<String, Object> payload = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", promptJson)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema,
                        "temperature", 0.2
                )
        );

        try {
            String requestBody = objectMapper.writeValueAsString(payload);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                throw new RuntimeException("Gemini API returned HTTP " + resp.statusCode());
            }

            JsonNode root = objectMapper.readTree(resp.body());
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
            if (text == null || text.isBlank()) {
                throw new RuntimeException("Gemini returned empty analysis content");
            }
            return objectMapper.readValue(text, AiStructuredOutput.class);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Gemini request interrupted");
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception ex) {
            throw new RuntimeException("Gemini analysis call failed: " + ex.getClass().getSimpleName());
        }
    }
}
