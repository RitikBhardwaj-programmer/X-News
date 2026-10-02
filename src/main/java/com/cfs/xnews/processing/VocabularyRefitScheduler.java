package com.cfs.xnews.processing;

import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.dto.ArticleText;
import com.cfs.xnews.event.dto.VocabularyRefitRequest;
import com.cfs.xnews.event.dto.VocabularyRefitResponse;
import com.cfs.xnews.news.articles.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps the v2 matcher's TF-IDF vocabulary current (V4 stage 1b): the AI
 * service refits it on the last 14 days of articles nightly, and again soon
 * after the service reports it has restarted with its shipped default. The
 * AI service has no database access, so the backend sends the texts.
 */
@Component
public class VocabularyRefitScheduler {

    private static final Logger log = LoggerFactory.getLogger(VocabularyRefitScheduler.class);

    static final int WINDOW_DAYS = 14;
    static final int MAX_ITEMS = 20_000;
    static final int MIN_ITEMS = 100;
    static final Duration MIN_GAP_BETWEEN_ATTEMPTS = Duration.ofMinutes(10);

    private final ArticleRepository articleRepository;
    private final EventMatchingClient eventMatchingClient;
    private final MatcherMode matcherMode;
    private final Clock clock;

    private final AtomicBoolean refitRequested = new AtomicBoolean(false);
    private volatile Instant lastAttempt = Instant.MIN;

    @Autowired
    public VocabularyRefitScheduler(
            ArticleRepository articleRepository,
            EventMatchingClient eventMatchingClient,

            @Value("${ai.event-matcher.mode:v1}")
            String matcherMode
    ) {
        this(articleRepository, eventMatchingClient, MatcherMode.parse(matcherMode), Clock.systemDefaultZone());
    }

    VocabularyRefitScheduler(
            ArticleRepository articleRepository,
            EventMatchingClient eventMatchingClient,
            MatcherMode matcherMode,
            Clock clock
    ) {
        this.articleRepository = articleRepository;
        this.eventMatchingClient = eventMatchingClient;
        this.matcherMode = matcherMode;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void nightlyRefit() {

        if (matcherMode != MatcherMode.V1) {
            refit();
        }
    }

    // Called from article processing; the refit itself runs on the
    // scheduler thread, outside the article's transaction.
    public void requestRefit() {
        refitRequested.set(true);
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 60_000)
    public void refitIfRequested() {

        if (matcherMode == MatcherMode.V1 || !refitRequested.get()) {
            return;
        }

        // A failing refit is retried at most every 10 minutes.
        if (Duration.between(lastAttempt, clock.instant()).compareTo(MIN_GAP_BETWEEN_ATTEMPTS) < 0) {
            return;
        }

        refitRequested.set(false);
        refit();
    }

    boolean refit() {

        lastAttempt = clock.instant();
        long startedAt = System.nanoTime();

        List<ArticleText> items = new ArrayList<>();

        for (Object[] row : articleRepository.findRecentTexts(
                LocalDateTime.now(clock).minusDays(WINDOW_DAYS),
                MAX_ITEMS
        )) {
            String title = (String) row[0];

            if (title != null && !title.isBlank()) {
                items.add(new ArticleText(
                        EventMatcherV2.truncate(title, EventMatcherV2.MAX_TITLE_CHARS),
                        EventMatcherV2.truncate((String) row[1], EventMatcherV2.MAX_DESCRIPTION_CHARS)
                ));
            }
        }

        if (items.size() < MIN_ITEMS) {

            log.warn(
                    "Vocabulary refit skipped: only {} articles in the last {} days (need {})",
                    items.size(),
                    WINDOW_DAYS,
                    MIN_ITEMS
            );

            return false;
        }

        try {

            VocabularyRefitResponse response =
                    eventMatchingClient.refitVocabulary(new VocabularyRefitRequest(items));

            log.info(
                    "Vocabulary refit: version={} documents={} ms={}",
                    response.vocabularyVersion(),
                    response.documents(),
                    (System.nanoTime() - startedAt) / 1_000_000
            );

            return true;

        } catch (RestClientException e) {

            // The AI service keeps its previous vocabulary.
            log.warn("Vocabulary refit failed: {}", e.getMessage());

            return false;
        }
    }
}
