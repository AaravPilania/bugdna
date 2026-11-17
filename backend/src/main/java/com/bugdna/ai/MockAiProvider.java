package com.bugdna.ai;

import com.bugdna.ai.AiAnalysisDtos.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic fallback provider used when AI_PROVIDER=mock or no API key is configured.
 * Inspects only the sanitized timeline input and cites real recorded eventIds so demos never break.
 */
@Component
public class MockAiProvider implements AiProvider {

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public boolean isMock() {
        return true;
    }

    @Override
    public AiStructuredOutput analyze(SanitizedBugInput input, String promptJson, String model) {
        List<SanitizedTimelineItem> timeline = input.timeline() != null ? input.timeline() : List.of();
        List<EvidenceItem> evidence = new ArrayList<>();
        List<HypothesisItem> hypotheses = new ArrayList<>();
        List<String> debuggingSteps = new ArrayList<>();
        List<String> reproductionSteps = new ArrayList<>();

        // Find notable clicks and the error trigger event from the real recorded timeline
        List<SanitizedTimelineItem> clicks = timeline.stream()
                .filter(e -> "CLICK".equals(e.type()) && e.actionLabel() != null)
                .toList();

        SanitizedTimelineItem trigger = timeline.stream()
                .filter(e -> "API_ERROR".equals(e.type()) || "JS_ERROR".equals(e.type())
                        || "PROMISE_REJECTION".equals(e.type()) || "NETWORK_ERROR".equals(e.type()))
                .reduce((first, second) -> second)
                .orElse(null);

        for (SanitizedTimelineItem click : clicks) {
            evidence.add(new EvidenceItem(
                    "User triggered action " + click.actionLabel() + " on page " + (click.pagePath() != null ? click.pagePath() : "/") + ".",
                    List.of(click.eventId())
            ));
        }

        if (trigger != null) {
            String desc = switch (trigger.type()) {
                case "API_ERROR" -> String.format("Endpoint %s %s returned HTTP %s%s.",
                        trigger.method() != null ? trigger.method() : "HTTP",
                        trigger.endpoint() != null ? trigger.endpoint() : "/",
                        trigger.status() != null ? trigger.status() : "error",
                        trigger.errorCode() != null ? " (" + trigger.errorCode() + ")" : "");
                case "JS_ERROR", "PROMISE_REJECTION" -> String.format("Client raised %s on %s: %s",
                        trigger.type(),
                        trigger.pagePath() != null ? trigger.pagePath() : "/",
                        trigger.message() != null ? trigger.message() : input.title());
                default -> String.format("Network request to %s failed on %s.",
                        trigger.endpoint() != null ? trigger.endpoint() : "/",
                        trigger.pagePath() != null ? trigger.pagePath() : "/");
            };
            evidence.add(new EvidenceItem(desc, List.of(trigger.eventId())));
        }

        // Tailor hypotheses based on error signature (clearly framed as guesses to verify)
        if ("DEMO_COUPON_RULE_ERROR".equals(input.errorCode())) {
            hypotheses.add(new HypothesisItem(
                    "Server-side checkout discount rule evaluation fails when coupon SAVE20 is active",
                    "high",
                    "The recorded timeline shows APPLY_COUPON succeeded prior to navigating to /checkout, where POST /demo-api/checkout failed with HTTP 500 DEMO_COUPON_RULE_ERROR."
            ));
            hypotheses.add(new HypothesisItem(
                    "Validation mismatch between /demo-api/cart/coupon and /demo-api/checkout",
                    "medium",
                    "Coupon validation accepts the promo code in cart, but the checkout rule engine throws an unhandled exception when calculating the discount total."
            ));
            debuggingSteps.add("Inspect the checkout handler for POST /demo-api/checkout and trace how the applied coupon object is evaluated.");
            debuggingSteps.add("Add a unit test exercising checkout with coupon SAVE20 alongside valid cart items.");
            debuggingSteps.add("Ensure coupon rule exceptions return a structured 4xx validation error instead of HTTP 500.");
        } else if ("JS_ERROR".equals(input.errorType())) {
            hypotheses.add(new HypothesisItem(
                    "Unsafe property access on an undefined or unloaded object in the client component",
                    "high",
                    "The error message and top stack frame indicate a dereference before data is checked for null/undefined."
            ));
            debuggingSteps.add("Check the component referenced in stack_top and guard nested property reads with optional chaining.");
            debuggingSteps.add("Verify API loading state handles missing fields gracefully before rendering.");
        } else if ("API_ERROR".equals(input.errorType())) {
            hypotheses.add(new HypothesisItem(
                    "Backend endpoint rejected or failed the request due to invalid parameters or missing entity",
                    "medium",
                    "The timeline shows a non-2xx HTTP response from " + (input.endpoint() != null ? input.endpoint() : "the API") + "."
            ));
            debuggingSteps.add("Review server logs and controller validation for " + (input.endpoint() != null ? input.endpoint() : "the failing endpoint") + ".");
            debuggingSteps.add("Verify frontend sends expected payload types and handles HTTP error statuses gracefully.");
        } else {
            hypotheses.add(new HypothesisItem(
                    "Unhandled asynchronous failure or transient service unavailability",
                    "medium",
                    "The error occurred without a caught rejection handler in the client session."
            ));
            debuggingSteps.add("Add a .catch() or try/catch around the failing async call and display a retry action.");
        }

        // Build numbered reproduction steps from timeline
        int step = 1;
        for (SanitizedTimelineItem ev : timeline) {
            if ("PAGE_VIEW".equals(ev.type()) || "ROUTE_CHANGE".equals(ev.type())) {
                reproductionSteps.add("Navigate to " + (ev.pagePath() != null ? ev.pagePath() : "/") + ".");
                step++;
            } else if ("CLICK".equals(ev.type()) && ev.actionLabel() != null) {
                reproductionSteps.add("Click the control tagged " + ev.actionLabel() + " on " + (ev.pagePath() != null ? ev.pagePath() : "/") + ".");
                step++;
            }
            if (step > 5) break;
        }
        if (reproductionSteps.isEmpty()) {
            reproductionSteps.add("Navigate to " + (input.pagePath() != null ? input.pagePath() : "/") + " and trigger the failing flow.");
        }
        reproductionSteps.add("Observe " + input.title() + ".");

        String summary = String.format(
                "%s has occurred %d time(s) across %d session(s)%s. Recorded session events trace the failure to %s.",
                input.title(),
                input.occurrenceCount(),
                Math.max(1, input.sessionsAffected()),
                input.isRegression() ? " (flagged as a REGRESSION after prior resolution)" : "",
                input.endpoint() != null ? input.endpoint() : (input.pagePath() != null ? input.pagePath() : "the active route")
        );

        return new AiStructuredOutput(summary, evidence, hypotheses, debuggingSteps, reproductionSteps);
    }
}
