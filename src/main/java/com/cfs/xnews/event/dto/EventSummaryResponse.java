package com.cfs.xnews.event.dto;

import java.time.LocalDateTime;

public record EventSummaryResponse(
        Long id,
        String title,
        String description,
        String summary,
        LocalDateTime createdAt,
        Long sourceCount
) {
}