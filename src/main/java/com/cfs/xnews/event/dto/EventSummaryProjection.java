package com.cfs.xnews.event.dto;

import java.time.LocalDateTime;

public interface EventSummaryProjection {

    Long getId();

    String getTitle();

    String getDescription();

    String getSummary();

    LocalDateTime getCreatedAt();

    Long getSourceCount();
}