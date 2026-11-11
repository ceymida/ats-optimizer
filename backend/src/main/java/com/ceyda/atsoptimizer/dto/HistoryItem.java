package com.ceyda.atsoptimizer.dto;

import java.time.Instant;

public record HistoryItem(
        Long id,
        String fileName,
        int overallScore,
        int keywordScore,
        int formatScore,
        Instant createdAt
) {
}
