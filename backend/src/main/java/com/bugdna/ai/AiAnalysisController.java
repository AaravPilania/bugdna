package com.bugdna.ai;

import com.bugdna.ai.AiAnalysisDtos.AiAnalysisResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bugs/{id}/analysis")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @PostMapping
    public ResponseEntity<AiAnalysisResponse> runAnalysis(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean force
    ) {
        AiAnalysisResponse result = aiAnalysisService.analyzeBug(id, force);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<AiAnalysisResponse> getLatestAnalysis(@PathVariable Long id) {
        return aiAnalysisService.getLatestAnalysis(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
