package com.cfs.xnews.event.dto;

import java.time.LocalDateTime;

public record EventSummaryResponse(
        Long id,
        String title,
        String description,
        String summary,
        LocalDateTime createdAt,
        Long sourceCount,
        String verificationStatus,
        String disagreementLevel,
        // Deprecated: a single LLM-generated risk number with no evidence
        // behind it (information-layer spec §13-§14). No longer shown in the
        // UI; kept so existing clients don't break, to be removed separately.
        Double misinformationRisk
) {
}