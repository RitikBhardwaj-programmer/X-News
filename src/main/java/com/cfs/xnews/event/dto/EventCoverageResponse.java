package com.cfs.xnews.event.dto;

import java.time.LocalDateTime;
import java.util.List;

// Who reported an event and when (information-layer spec §7-§8): derived from
// the event's member articles, so every number traces back to an article.
public record EventCoverageResponse(
        Long eventId,
        int articleCount,
        int outletCount,
        LocalDateTime firstSeen,
        LocalDateTime lastSeen,
        List<OutletCoverage> outlets,
        List<TimelineEntry> timeline
) {

    public record OutletCoverage(
            String outlet,
            List<String> feeds,
            int articleCount,
            LocalDateTime firstSeen
    ) {
    }

    // publishedAt is what the outlet reports; observedAt is when X-NEWS
    // collected it.
    public record TimelineEntry(
            Long articleId,
            String title,
            String outlet,
            String feed,
            String url,
            LocalDateTime publishedAt,
            LocalDateTime observedAt
    ) {
    }
}
