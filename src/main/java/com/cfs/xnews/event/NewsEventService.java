package com.cfs.xnews.event;

import com.cfs.xnews.analysis.FactCheckRepository;
import com.cfs.xnews.event.dto.EventCoverageResponse;
import com.cfs.xnews.event.dto.EventMatchConfidence;
import com.cfs.xnews.event.dto.EventCoverageResponse.OutletCoverage;
import com.cfs.xnews.event.dto.EventCoverageResponse.TimelineEntry;
import com.cfs.xnews.event.dto.EventSummaryResponse;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class NewsEventService {

    private static final String SOURCE_SEPARATOR = " -> ";

    private final NewsEventRepository eventRepository;
    private final ArticleRepository articleRepository;
    private final FactCheckRepository factCheckRepository;
    private final EventMatchDecisionRepository eventMatchDecisionRepository;

    public NewsEventService(
            NewsEventRepository eventRepository,
            ArticleRepository articleRepository,
            FactCheckRepository factCheckRepository,
            EventMatchDecisionRepository eventMatchDecisionRepository
    ) {
        this.eventRepository = eventRepository;
        this.articleRepository = articleRepository;
        this.factCheckRepository = factCheckRepository;
        this.eventMatchDecisionRepository = eventMatchDecisionRepository;
    }

    // Empty when the event doesn't exist (404); an empty list when it exists
    // but none of its articles has a recorded decision.
    @Transactional(readOnly = true)
    public Optional<List<EventMatchConfidence>> getMatchConfidence(Long id) {

        if (!eventRepository.existsById(id)) {
            return Optional.empty();
        }

        return Optional.of(
                eventMatchDecisionRepository.findAppliedForEvent(id)
                        .stream()
                        .map(NewsEventService::toMatchConfidence)
                        .toList()
        );
    }

    static EventMatchConfidence toMatchConfidence(EventMatchDecision decision) {

        return new EventMatchConfidence(
                decision.getArticleId(),
                decision.getChosenEventId() == null ? "started" : "joined",
                decision.getMatcher(),
                decision.getProbability(),
                decision.getThreshold(),
                decision.getModelVersion(),
                decision.getCreatedAt()
        );
    }

    public NewsEvent createEvent(Article article) {

        NewsEvent event = new NewsEvent(
                article.getTitle(),
                article.getDescription()
        );

        event.addArticle(article);

        return eventRepository.save(event);
    }

    public List<EventSummaryResponse> getAllEvents() {

        return eventRepository
                .findAllEventSummaries()
                .stream()
                .map(event -> new EventSummaryResponse(
                        event.getId(),
                        event.getTitle(),
                        event.getGeneratedTitle(),
                        event.getDescription(),
                        event.getSummary(),
                        event.getCreatedAt(),
                        event.getSourceCount(),
                        event.getVerificationStatus(),
                        event.getDisagreementLevel()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<EventCoverageResponse> getCoverage(Long id) {

        return eventRepository
                .findById(id)
                .map(event -> buildCoverage(event.getId(), event.getArticles()));
    }

    static EventCoverageResponse buildCoverage(Long eventId, List<Article> articles) {

        List<TimelineEntry> timeline = articles.stream()
                .map(article -> new TimelineEntry(
                        article.getId(),
                        article.getTitle(),
                        outletOf(article.getSource()),
                        feedOf(article.getSource()),
                        article.getUrl(),
                        article.getPublishedAt(),
                        article.getCreatedAt()
                ))
                .sorted(Comparator.comparing(NewsEventService::seenAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // Insertion order follows the timeline, so outlets are listed in the
        // order they first reported the event.
        Map<String, List<TimelineEntry>> byOutlet = new LinkedHashMap<>();
        for (TimelineEntry entry : timeline) {
            byOutlet.computeIfAbsent(entry.outlet(), key -> new ArrayList<>()).add(entry);
        }

        List<OutletCoverage> outlets = byOutlet.entrySet().stream()
                .map(outlet -> new OutletCoverage(
                        outlet.getKey(),
                        outlet.getValue().stream().map(TimelineEntry::feed).filter(feed -> !feed.isEmpty()).distinct().toList(),
                        outlet.getValue().size(),
                        seenAt(outlet.getValue().get(0))
                ))
                .toList();

        return new EventCoverageResponse(
                eventId,
                timeline.size(),
                outlets.size(),
                timeline.isEmpty() ? null : seenAt(timeline.get(0)),
                timeline.stream().map(NewsEventService::seenAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null),
                outlets,
                timeline
        );
    }

    // Article sources are stored as "Outlet -> Section" (NewsSource.name).
    public static String outletOf(String source) {
        if (source == null) {
            return "Unknown";
        }
        int arrow = source.indexOf(SOURCE_SEPARATOR);
        return arrow < 0 ? source.trim() : source.substring(0, arrow).trim();
    }

    static String feedOf(String source) {
        if (source == null) {
            return "";
        }
        int arrow = source.indexOf(SOURCE_SEPARATOR);
        return arrow < 0 ? "" : source.substring(arrow + SOURCE_SEPARATOR.length()).trim();
    }

    // The outlet's publish time, or when X-NEWS collected the article if the
    // feed gave none.
    private static LocalDateTime seenAt(TimelineEntry entry) {
        return entry.publishedAt() != null ? entry.publishedAt() : entry.observedAt();
    }

    @Transactional
    public void deleteEvent(Long id) {

        NewsEvent event = eventRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Event not found"
                        )
                );

        // Articles outlive the event they were grouped under - detach
        // rather than delete, so their content isn't lost.
        for (Article article : event.getArticles()) {
            article.setNewsEvent(null);
        }

        articleRepository.saveAll(event.getArticles());

        // A fact-check has no meaning without the event it verifies.
        factCheckRepository.deleteAll(event.getFactChecks());

        eventRepository.delete(event);
    }
}