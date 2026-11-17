package com.bugdna.ai;

import java.time.LocalDateTime;
import java.util.List;

public final class AiAnalysisDtos {

    private AiAnalysisDtos() {}

    public record EvidenceItem(
        String statement,
        List<String> eventIds
    ) {}

    public record HypothesisItem(
        String cause,
        String confidence,
        String reasoning
    ) {}

    public record AiStructuredOutput(
        String summary,
        List<EvidenceItem> evidence,
        List<HypothesisItem> hypotheses,
        List<String> debuggingSteps,
        List<String> reproductionSteps
    ) {}

    /**
     * Strictly sanitized input passed to AI providers.
     * Deliberately excludes sessionId, userAgent, cookies, auth headers, and raw bodies.
     */
    public record SanitizedTimelineItem(
        String eventId,
        String type,
        String actionLabel,
        String pagePath,
        String method,
        Integer status,
        String endpoint,
        String errorCode,
        String message,
        String stackTop
    ) {}

    public record SanitizedBugInput(
        Long bugId,
        String title,
        String errorType,
        String errorCode,
        String httpMethod,
        String endpoint,
        String pagePath,
        String canonicalFingerprint,
        int occurrenceCount,
        long sessionsAffected,
        boolean isRegression,
        int regressionCount,
        List<SanitizedTimelineItem> timeline
    ) {}

    public record AiAnalysisResponse(
        Long id,
        Long bugId,
        String provider,
        String model,
        AiStatus status,
        boolean cached,
        String summary,
        List<EvidenceItem> evidence,
        List<HypothesisItem> hypotheses,
        List<String> debuggingSteps,
        List<String> reproductionSteps,
        String errorMessage,
        LocalDateTime createdAt
    ) {}
}

