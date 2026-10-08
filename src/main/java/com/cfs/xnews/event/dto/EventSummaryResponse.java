package com.cfs.xnews.event.dto;

import java.time.LocalDateTime;

public record EventSummaryResponse(
        Long id,
        String title,
        // Gemini-written title, null until titled (title stays the first headline)
        String generatedTitle,
        String description,
        String summary,
        LocalDateTime createdAt,
        Long sourceCount,
        String verificationStatus,
        String disagreementLevel
) {
}