package com.cfs.xnews.storyline;

import com.cfs.xnews.provenance.ExtractionRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Nightly storyline links (V4 roadmap step 7): one SQL statement over the
 * last 14 days, so it never holds the shared scheduler thread for long.
 */
@Component
public class EventRelationScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventRelationScheduler.class);

    static final Duration WINDOW = Duration.ofDays(14);
    static final int MIN_SHARED_ENTITIES = 2;
    static final double MIN_COSINE = 0.6;
    // An entity in more than max(5, 2% of recent events) is too generic.
    static final int MIN_GENERIC_EVENTS = 5;
    static final String RUN_KIND = "event-relations";
    static final String RUN_MODEL =
            "entities>=" + MIN_SHARED_ENTITIES + ",cosine>=" + MIN_COSINE + ",window=" + WINDOW.toDays() + "d";

    private final EventRelationRepository repository;
    private final ExtractionRunService extractionRunService;
    private final Clock clock;

    @Autowired
    public EventRelationScheduler(
            EventRelationRepository repository,
            ExtractionRunService extractionRunService
    ) {
        this(repository, extractionRunService, Clock.systemDefaultZone());
    }

    EventRelationScheduler(
            EventRelationRepository repository,
            ExtractionRunService extractionRunService,
            Clock clock
    ) {
        this.repository = repository;
        this.extractionRunService = extractionRunService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "UTC")
    @Transactional
    public int linkNightly() {

        long startedAt = System.nanoTime();

        Long runId = extractionRunService.runId(RUN_KIND, RUN_MODEL, ExtractionRunService.NO_PROMPT);

        int linked = repository.linkRecentEvents(
                LocalDateTime.now(clock).minus(WINDOW),
                MIN_SHARED_ENTITIES,
                MIN_COSINE,
                MIN_GENERIC_EVENTS,
                runId
        );

        log.info(
                "Event relations: upserted={} ms={}",
                linked,
                (System.nanoTime() - startedAt) / 1_000_000
        );

        return linked;
    }
}
