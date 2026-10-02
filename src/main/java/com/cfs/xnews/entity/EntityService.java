package com.cfs.xnews.entity;

import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.dto.ArticleText;
import com.cfs.xnews.event.dto.EntityExtractionResponse;
import com.cfs.xnews.event.dto.EntityExtractionResponse.Mention;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import com.cfs.xnews.provenance.ExtractionRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Set;

/**
 * Records which entities an article mentions (V4 roadmap step 3). Runs
 * after the article is processed, in its own transaction, so a failure
 * here never affects processing or event matching.
 */
@Service
public class EntityService {

    private static final Logger log = LoggerFactory.getLogger(EntityService.class);

    static final String RUN_KIND = "entity-extraction";
    static final int MAX_NAME_CHARS = 200;
    static final int MAX_TITLE_CHARS = 500;
    static final int MAX_DESCRIPTION_CHARS = 5000;
    static final Set<String> TYPES = Set.of("team", "name");
    static final Set<String> FIELDS = Set.of("title", "description");

    private final NamedEntityRepository entityRepository;
    private final ArticleRepository articleRepository;
    private final EventMatchingClient eventMatchingClient;
    private final ExtractionRunService extractionRunService;

    public EntityService(
            NamedEntityRepository entityRepository,
            ArticleRepository articleRepository,
            EventMatchingClient eventMatchingClient,
            ExtractionRunService extractionRunService
    ) {
        this.entityRepository = entityRepository;
        this.articleRepository = articleRepository;
        this.eventMatchingClient = eventMatchingClient;
        this.extractionRunService = extractionRunService;
    }

    /** Number of mentions stored (0 when skipped or the AI service failed). */
    @Transactional
    public int recordMentions(Long articleId) {

        Article article = articleRepository.findById(articleId).orElse(null);

        // Missing article, or already done (a re-delivered Kafka message).
        if (article == null || entityRepository.hasMentions(articleId)) {
            return 0;
        }

        EntityExtractionResponse response;

        try {
            response = eventMatchingClient.extractEntities(new ArticleText(
                    truncate(article.getTitle(), MAX_TITLE_CHARS),
                    truncate(article.getDescription(), MAX_DESCRIPTION_CHARS)
            ));
        } catch (RestClientException e) {
            log.warn("Entity extraction failed for article={}: {}", articleId, e.getMessage());
            return 0;
        }

        if (response == null || response.mentions() == null || response.mentions().isEmpty()) {
            return 0;
        }

        Long runId = extractionRunService.runId(
                RUN_KIND,
                response.extractorVersion(),
                ExtractionRunService.NO_PROMPT
        );

        int stored = 0;

        for (Mention mention : valid(response.mentions())) {
            Long entityId = resolve(mention);
            stored += entityRepository.insertMentionIfAbsent(
                    articleId,
                    entityId,
                    truncate(mention.text(), MAX_NAME_CHARS),
                    mention.field(),
                    runId
            );
        }

        return stored;
    }

    // The entity an alias belongs to, created on first sight. If another
    // transaction claims the same alias first, its entity wins and ours is
    // removed.
    Long resolve(Mention mention) {

        String alias = truncate(mention.normalized(), MAX_NAME_CHARS);

        Long existing = entityRepository.findIdByAlias(alias).orElse(null);
        if (existing != null) {
            return existing;
        }

        NamedEntity entity = entityRepository.save(
                new NamedEntity(truncate(mention.text(), MAX_NAME_CHARS), mention.type())
        );

        if (entityRepository.insertAliasIfAbsent(entity.getId(), alias) == 1) {
            return entity.getId();
        }

        entityRepository.delete(entity);
        return entityRepository.findIdByAlias(alias)
                .orElseThrow(() -> new IllegalStateException("Alias vanished: " + alias));
    }

    // Defensive: only well-formed mentions from the AI service are stored.
    static List<Mention> valid(List<Mention> mentions) {

        return mentions.stream()
                .filter(m -> m.normalized() != null && m.normalized().length() > 2)
                .filter(m -> m.text() != null && !m.text().isBlank())
                .filter(m -> TYPES.contains(m.type()) && FIELDS.contains(m.field()))
                .toList();
    }

    static String truncate(String text, int maxChars) {
        return text == null || text.length() <= maxChars ? text : text.substring(0, maxChars);
    }
}
