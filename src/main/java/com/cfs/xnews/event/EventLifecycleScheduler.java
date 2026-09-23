package com.cfs.xnews.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Bounds how long a story stays open for new matches, so an event can't
 * keep absorbing unrelated articles forever.
 */
@Component
public class EventLifecycleScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventLifecycleScheduler.class);

    private final NewsEventRepository eventRepository;
    private final int inactivityDays;

    public EventLifecycleScheduler(
            NewsEventRepository eventRepository,

            @Value("${xnews.event.inactivity-days:10}")
            int inactivityDays
    ) {
        this.eventRepository = eventRepository;
        this.inactivityDays = inactivityDays;
    }

    @Scheduled(fixedRate = 3_600_000)
    @Transactional
    public void closeInactiveEvents() {

        LocalDateTime cutoff = LocalDateTime.now().minusDays(inactivityDays);

        int closed = eventRepository.closeEventsInactiveSince(
                EventStatus.OPEN,
                EventStatus.CLOSED,
                cutoff
        );

        log.info(
                "Event lifecycle: closed={} inactivityDays={} cutoff={}",
                closed,
                inactivityDays,
                cutoff
        );
    }
}
