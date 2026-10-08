package com.cfs.xnews.title;

import com.cfs.xnews.ai.AIServiceUnavailableException;
import com.cfs.xnews.event.NewsEventRepository;
import com.google.genai.errors.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Titles open events once they reach EventTitleService.MIN_ARTICLES. Off
 * unless xnews.titles.enabled is true. The Gemini key is on the free tier
 * (20 requests a day, shared with Analyze), so calls are capped per run
 * (runs also share the single scheduler thread) and per UTC day, and a 429
 * pauses titling until the next UTC day. The daily count comes from
 * news_events.title_attempted_at, so a restart doesn't reset it; only the
 * 429 pause is in memory (a restart costs at most one more 429).
 */
@Component
public class EventTitleScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventTitleScheduler.class);

    private static final int RATE_LIMITED = 429;

    private final NewsEventRepository eventRepository;
    private final EventTitleService titleService;
    private final boolean enabled;
    private final int maxPerRun;
    private final int maxPerDay;
    private final Clock clock;

    // The UTC day Gemini answered 429; titling waits for the next day.
    private LocalDate pausedOn;

    @Autowired
    public EventTitleScheduler(
            NewsEventRepository eventRepository,
            EventTitleService titleService,

            @Value("${xnews.titles.enabled:false}")
            boolean enabled,

            @Value("${xnews.titles.max-per-run:5}")
            int maxPerRun,

            @Value("${xnews.titles.max-per-day:12}")
            int maxPerDay
    ) {
        this(eventRepository, titleService, enabled, maxPerRun, maxPerDay, Clock.systemUTC());
    }

    EventTitleScheduler(
            NewsEventRepository eventRepository,
            EventTitleService titleService,
            boolean enabled,
            int maxPerRun,
            int maxPerDay,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.titleService = titleService;
        this.enabled = enabled;
        this.maxPerRun = maxPerRun;
        this.maxPerDay = maxPerDay;
        this.clock = clock;
    }

    @Scheduled(initialDelay = 120_000, fixedDelay = 900_000)
    public int titleEvents() {

        if (!enabled) {
            return 0;
        }

        LocalDate today = LocalDate.now(clock);
        if (today.equals(pausedOn)) {
            return 0;
        }

        long usedToday = eventRepository.countTitleAttemptsSince(today.atStartOfDay());
        int budget = (int) Math.min(maxPerRun, maxPerDay - usedToday);
        if (budget <= 0) {
            return 0;
        }

        List<Long> eventIds = eventRepository.findEventIdsToTitle(EventTitleService.MIN_ARTICLES, budget);

        int titled = 0;
        int discarded = 0;

        for (Long eventId : eventIds) {

            try {
                switch (titleService.title(eventId)) {
                    case TITLED -> titled++;
                    case DISCARDED -> discarded++;
                    case SKIPPED -> { }
                }

            } catch (ApiException | AIServiceUnavailableException e) {

                if (isRateLimited(e)) {
                    // Daily quota (or a burst limit) reached: wait for the next UTC day.
                    pausedOn = today;
                    log.warn("Event titles: Gemini rate limit reached, paused until the next UTC day");
                } else {
                    log.warn("Event titles: stopping this run after a Gemini error: {}", e.getMessage());
                }
                break;

            } catch (RuntimeException e) {
                log.warn("Event titles: event={} failed: {}", eventId, e.getMessage());
            }
        }

        if (!eventIds.isEmpty()) {
            log.info(
                    "Event titles: candidates={} titled={} discarded={} usedToday={}/{}",
                    eventIds.size(),
                    titled,
                    discarded,
                    usedToday + titled + discarded,
                    maxPerDay
            );
        }

        return titled;
    }

    private static boolean isRateLimited(RuntimeException e) {

        Throwable cause = e instanceof ApiException ? e : e.getCause();
        return cause instanceof ApiException api && api.code() == RATE_LIMITED;
    }
}
