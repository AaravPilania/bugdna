package com.bugdna.ai;

import com.bugdna.ai.AiAnalysisDtos.AiStructuredOutput;
import com.bugdna.ai.AiAnalysisDtos.SanitizedBugInput;

public interface AiProvider {
    String getProviderName();
    boolean isMock();
    AiStructuredOutput analyze(SanitizedBugInput input, String promptJson, String model);
}

