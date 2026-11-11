package com.ceyda.atsoptimizer.dto;

import java.time.Instant;
import java.util.List;

public record AnalysisResponse(
        Long id,
        int overallScore,
        int keywordScore,
        int formatScore,
        int readabilityScore,
        String grade,
        String summary,
        int wordCount,
        List<String> matchedKeywords,
        List<KeywordDetail> missingKeywords,
        List<String> suggestions,
        List<CheckItem> checks,
        boolean aiEnhanced,
        String fileName,
        Instant createdAt
) {
    public record KeywordDetail(String term, double weight) {
    }

    public record CheckItem(String label, boolean passed, int points) {
    }
}
