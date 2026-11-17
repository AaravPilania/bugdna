package com.bugdna.ai;

import com.bugdna.ai.AiAnalysisDtos.*;
import com.bugdna.analytics.BugSummaryView;
import com.bugdna.analytics.BugSummaryViewRepository;
import com.bugdna.bug.Bug;
import com.bugdna.bug.BugRepository;
import com.bugdna.common.ResourceNotFoundException;
import com.bugdna.event.TimelineService;
import com.bugdna.event.dto.TimelineEventDto;
import com.bugdna.event.dto.TimelineResponseDto;
import com.bugdna.ingest.FingerprintService;
import com.bugdna.ingest.Sanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiAnalysisService.class);

    private final BugRepository bugRepository;
    private final BugSummaryViewRepository bugSummaryViewRepository;
    private final TimelineService timelineService;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final FingerprintService fingerprintService;
    private final Sanitizer sanitizer;
    private final GeminiProvider geminiProvider;
    private final OpenAiProvider openAiProvider;
    private final MockAiProvider mockAiProvider;
    private final ObjectMapper objectMapper;

    @Value("${bugdna.ai.provider:mock}")
    private String configuredProvider = "mock";

    @Value("${bugdna.ai.model:gemini-2.5-flash}")
    private String configuredModel = "gemini-2.5-flash";

    public AiAnalysisService(
            BugRepository bugRepository,
            BugSummaryViewRepository bugSummaryViewRepository,
            TimelineService timelineService,
            AiAnalysisRepository aiAnalysisRepository,
            FingerprintService fingerprintService,
            Sanitizer sanitizer,
            GeminiProvider geminiProvider,
            OpenAiProvider openAiProvider,
            MockAiProvider mockAiProvider,
            ObjectMapper objectMapper
    ) {
        this.bugRepository = bugRepository;
        this.bugSummaryViewRepository = bugSummaryViewRepository;
        this.timelineService = timelineService;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.fingerprintService = fingerprintService;
        this.sanitizer = sanitizer;
        this.geminiProvider = geminiProvider;
        this.openAiProvider = openAiProvider;
        this.mockAiProvider = mockAiProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public Optional<AiAnalysisResponse> getLatestAnalysis(Long bugId) {
        if (!bugRepository.existsById(bugId)) {
            throw new ResourceNotFoundException("Bug not found with id: " + bugId);
        }
        return aiAnalysisRepository.findFirstByBugIdOrderByCreatedAtDesc(bugId)
                .map(entity -> toResponse(entity, false));
    }

    @Transactional
    public AiAnalysisResponse analyzeBug(Long bugId, boolean force) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + bugId));

        BugSummaryView summary = bugSummaryViewRepository.findById(bugId).orElse(null);
        TimelineResponseDto timeline = timelineService.getTimeline(bugId, null);

        // 1. Build strictly sanitized prompt input (zero PII, zero sessionId, zero userAgent)
        SanitizedBugInput sanitizedInput = buildSanitizedInput(bug, summary, timeline);
        String promptJson = serializePromptInput(sanitizedInput);
        String inputHash = fingerprintService.computeFingerprint(promptJson);

        // 2. Cache check by input_hash unless force=true
        if (!force) {
            Optional<AiAnalysis> cachedOpt = aiAnalysisRepository.findByBugIdAndInputHash(bugId, inputHash)
                    .filter(a -> a.getStatus() == AiStatus.OK || a.getStatus() == AiStatus.MOCK);
            if (cachedOpt.isPresent()) {
                log.info("AI analysis cache hit for bug #{} (inputHash={})", bugId, inputHash.substring(0, 12));
                return toResponse(cachedOpt.get(), true);
            }
        }

        // 3. Resolve provider (falls back to mock when no key is configured)
        AiProvider provider = resolveActiveProvider();
        String modelName = provider.isMock() ? "bugdna-mock-v1" : configuredModel;

        log.info("Running AI analysis for bug #{} using provider={} model={}", bugId, provider.getProviderName(), modelName);

        AiAnalysis entity = new AiAnalysis();
        entity.setBug(bug);
        entity.setProvider(provider.getProviderName());
        entity.setModel(modelName);
        entity.setInputHash(inputHash);

        try {
            AiStructuredOutput rawOutput = provider.analyze(sanitizedInput, promptJson, modelName);

            // 4. Grounding validation: drop hallucinated eventIds and reasoning-less hypotheses
            Set<String> validEventIds = sanitizedInput.timeline().stream()
                    .map(SanitizedTimelineItem::eventId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            AiStructuredOutput validated = validateOutput(rawOutput, validEventIds);

            entity.setStatus(provider.isMock() ? AiStatus.MOCK : AiStatus.OK);
            entity.setSummary(sanitizer.sanitizeText(validated.summary(), 2000));
            entity.setEvidenceJson(objectMapper.writeValueAsString(validated.evidence()));
            entity.setHypothesesJson(objectMapper.writeValueAsString(validated.hypotheses()));
            entity.setDebugStepsJson(objectMapper.writeValueAsString(validated.debuggingSteps()));
            entity.setReproStepsJson(objectMapper.writeValueAsString(validated.reproductionSteps()));
            entity.setErrorMessage(null);
        } catch (Exception ex) {
            // Never log or persist API keys; store a safe sanitized failure summary
            String safeErr = sanitizer.sanitizeText(ex.getMessage(), 450);
            if (safeErr == null || safeErr.isBlank()) {
                safeErr = "AI provider call failed";
            }
            log.warn("AI analysis failed for bug #{} via provider={}: {}", bugId, provider.getProviderName(), safeErr);
            entity.setStatus(AiStatus.FAILED);
            entity.setErrorMessage(safeErr);
        }

        AiAnalysis saved = aiAnalysisRepository.saveAndFlush(entity);
        return toResponse(saved, false);
    }

    /**
     * Constructs the exact allowlisted prompt input from §6.4.
     * Deliberately strips sessionId and userAgent and re-sanitizes every string field.
     */
    public SanitizedBugInput buildSanitizedInput(Bug bug, BugSummaryView summary, TimelineResponseDto timeline) {
        long sessionsAffected = summary != null ? summary.getSessionsAffected() : 1L;

        List<SanitizedTimelineItem> timelineItems = new ArrayList<>();
        if (timeline != null && timeline.events() != null) {
            for (TimelineEventDto ev : timeline.events()) {
                timelineItems.add(new SanitizedTimelineItem(
                        ev.eventId(),
                        ev.type() != null ? ev.type().name() : null,
                        sanitizer.sanitizeText(ev.actionLabel(), 80),
                        ev.pagePath() != null ? sanitizer.sanitizePath(ev.pagePath()) : null,
                        ev.httpMethod(),
                        ev.httpStatus() != null ? ev.httpStatus().intValue() : null,
                        ev.endpoint() != null ? sanitizer.sanitizePath(ev.endpoint()) : null,
                        sanitizer.sanitizeText(ev.errorCode(), 80),
                        sanitizer.sanitizeText(ev.message(), 300),
                        sanitizer.sanitizeText(ev.stackTop(), 400)
                ));
            }
        }

        String canonical = fingerprintService.computeCanonical(
                bug.getErrorType(),
                bug.getHttpMethod(),
                bug.getEndpoint(),
                null,
                bug.getErrorCode(),
                null,
                bug.getTitle(),
                null
        );

        return new SanitizedBugInput(
                bug.getId(),
                sanitizer.sanitizeText(bug.getTitle(), 200),
                bug.getErrorType() != null ? bug.getErrorType().name() : null,
                sanitizer.sanitizeText(bug.getErrorCode(), 80),
                bug.getHttpMethod(),
                bug.getEndpoint() != null ? sanitizer.sanitizePath(bug.getEndpoint()) : null,
                bug.getPagePath() != null ? sanitizer.sanitizePath(bug.getPagePath()) : null,
                canonical,
                bug.getOccurrenceCount(),
                sessionsAffected,
                bug.isRegression(),
                bug.getRegressionCount(),
                timelineItems
        );
    }

    public String serializePromptInput(SanitizedBugInput input) {
        return objectMapper.writeValueAsString(input);
    }

    /**
     * Validates AI output against recorded reality:
     * - Drops any evidence item whose eventIds are empty or do not exist in the real input timeline.
     * - Drops any hypothesis lacking a cause or reasoning, and normalizes confidence labels.
     */
    public AiStructuredOutput validateOutput(AiStructuredOutput raw, Set<String> validEventIds) {
        if (raw == null) {
            return new AiStructuredOutput("No analysis produced.", List.of(), List.of(), List.of(), List.of());
        }

        List<EvidenceItem> validEvidence = new ArrayList<>();
        if (raw.evidence() != null) {
            for (EvidenceItem item : raw.evidence()) {
                if (item == null || item.statement() == null || item.statement().isBlank() || item.eventIds() == null) {
                    continue;
                }
                List<String> verifiedIds = item.eventIds().stream()
                        .filter(Objects::nonNull)
                        .filter(validEventIds::contains)
                        .distinct()
                        .toList();
                // Drop evidence item if none of its cited eventIds exist in the recorded timeline
                if (!verifiedIds.isEmpty()) {
                    validEvidence.add(new EvidenceItem(item.statement().trim(), verifiedIds));
                }
            }
        }

        List<HypothesisItem> validHypotheses = new ArrayList<>();
        if (raw.hypotheses() != null) {
            for (HypothesisItem h : raw.hypotheses()) {
                if (h == null || h.cause() == null || h.cause().isBlank()
                        || h.reasoning() == null || h.reasoning().isBlank()) {
                    continue;
                }
                String conf = normalizeConfidence(h.confidence());
                validHypotheses.add(new HypothesisItem(h.cause().trim(), conf, h.reasoning().trim()));
            }
        }

        List<String> debugSteps = raw.debuggingSteps() != null
                ? raw.debuggingSteps().stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList()
                : List.of();

        List<String> reproSteps = raw.reproductionSteps() != null
                ? raw.reproductionSteps().stream().filter(s -> s != null && !s.isBlank()).map(String::trim).toList()
                : List.of();

        String summary = raw.summary() != null ? raw.summary().trim() : "";
        return new AiStructuredOutput(summary, validEvidence, validHypotheses, debugSteps, reproSteps);
    }

    private String normalizeConfidence(String confidence) {
        if (confidence == null) return "medium";
        return switch (confidence.trim().toLowerCase()) {
            case "high" -> "high";
            case "low" -> "low";
            default -> "medium";
        };
    }

    private AiProvider resolveActiveProvider() {
        String p = configuredProvider != null ? configuredProvider.trim().toLowerCase() : "mock";
        if ("gemini".equals(p) && geminiProvider.hasKey()) {
            return geminiProvider;
        }
        if ("openai".equals(p) && openAiProvider.hasKey()) {
            return openAiProvider;
        }
        return mockAiProvider;
    }

    private AiAnalysisResponse toResponse(AiAnalysis entity, boolean cached) {
        List<EvidenceItem> evidence = parseJsonList(entity.getEvidenceJson(), new TypeReference<>() {});
        List<HypothesisItem> hypotheses = parseJsonList(entity.getHypothesesJson(), new TypeReference<>() {});
        List<String> debugSteps = parseJsonList(entity.getDebugStepsJson(), new TypeReference<>() {});
        List<String> reproSteps = parseJsonList(entity.getReproStepsJson(), new TypeReference<>() {});

        return new AiAnalysisResponse(
                entity.getId(),
                entity.getBug() != null ? entity.getBug().getId() : null,
                entity.getProvider(),
                entity.getModel(),
                entity.getStatus(),
                cached,
                entity.getSummary(),
                evidence,
                hypotheses,
                debugSteps,
                reproSteps,
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }

    private <T> List<T> parseJsonList(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception ex) {
            return List.of();
        }
    }
}

