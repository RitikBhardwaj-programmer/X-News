package com.cfs.xnews.title;

import com.cfs.xnews.ai.AIService;
import com.cfs.xnews.ai.AIServiceUnavailableException;
import com.cfs.xnews.ai.EventAITitle;
import com.cfs.xnews.event.NewsEvent;
import com.cfs.xnews.event.NewsEventRepository;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.provenance.ExtractionRunService;
import com.google.genai.errors.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Titles one event with Gemini (V4 roadmap: auto-titles), once, when it
 * reaches MIN_ARTICLES. The first headline in news_events.title is never
 * changed; the generated title is stored next to it with its run.
 */
@Service
public class EventTitleService {

    private static final Logger log = LoggerFactory.getLogger(EventTitleService.class);

    static final String RUN_KIND = "event-title";
    // Big events only: the free-tier key allows 20 Gemini requests a day,
    // shared with the Analyze button.
    static final int MIN_ARTICLES = 5;

    // Gemini rejected this event's request itself (not the key or the
    // service), so retrying the same event would fail the same way.
    private static final int BAD_REQUEST = 400;

    public enum Outcome { TITLED, DISCARDED, SKIPPED }

    private final NewsEventRepository eventRepository;
    private final AIService aiService;
    private final ExtractionRunService extractionRunService;
    private final Clock clock;

    @Autowired
    public EventTitleService(
            NewsEventRepository eventRepository,
            AIService aiService,
            ExtractionRunService extractionRunService
    ) {
        this(eventRepository, aiService, extractionRunService, Clock.systemUTC());
    }

    EventTitleService(
            NewsEventRepository eventRepository,
            AIService aiService,
            ExtractionRunService extractionRunService,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.aiService = aiService;
        this.extractionRunService = extractionRunService;
        this.clock = clock;
    }

    /**
     * Rate limits, server errors and key problems are rethrown and the
     * attempt is not recorded (the transaction rolls back), so the event is
     * tried again later. A request Gemini rejects for this event, an
     * unusable reply, or a title that fails validation records the attempt,
     * so the event isn't tried again.
     */
    @Transactional
    public Outcome title(Long eventId) {

        NewsEvent event = eventRepository.findById(eventId).orElse(null);

        if (event == null || event.getMemberCount() < MIN_ARTICLES) {
            return Outcome.SKIPPED;
        }

        event.setTitleAttemptedAt(LocalDateTime.now(clock));

        EventAITitle raw;

        try {
            raw = aiService.titleEvent(event);

        } catch (ApiException e) {
            if (e.code() != BAD_REQUEST) {
                throw e;
            }
            log.warn("Event title: event={} request rejected: {}", eventId, e.getMessage());
            return Outcome.DISCARDED;

        } catch (AIServiceUnavailableException e) {
            throw e;

        } catch (RuntimeException e) {
            log.warn("Event title: event={} unusable reply: {}", eventId, e.getMessage());
            return Outcome.DISCARDED;
        }

        Set<Long> articleIds = event.getArticles().stream()
                .map(Article::getId)
                .collect(Collectors.toSet());

        Optional<GeneratedTitle> title = GeneratedTitle.validate(raw, articleIds);

        if (title.isEmpty()) {
            log.info("Event title: event={} discarded (blank, too long or fewer than 2 citations)", eventId);
            return Outcome.DISCARDED;
        }

        event.setGeneratedTitle(title.get().text());
        event.setTitleRunId(
                extractionRunService.runId(
                        RUN_KIND,
                        aiService.modelName(),
                        AIService.TITLE_PROMPT_VERSION
                )
        );

        return Outcome.TITLED;
    }
}
