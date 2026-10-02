package com.cfs.xnews.claim;

import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.NewsEventService;
import com.cfs.xnews.event.dto.ArticleText;
import com.cfs.xnews.event.dto.ClaimExtractionResponse;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import com.cfs.xnews.provenance.ExtractionRunService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Numeric claims (V4 roadmap step 6): extracted after an article is
 * processed, in its own transaction; linked to agreeing and conflicting
 * claims in the same event; shown only after an admin approves them.
 */
@Service
public class ClaimService {

    private static final Logger log = LoggerFactory.getLogger(ClaimService.class);

    static final String RUN_KIND = "claim-extraction";
    static final Set<String> PREDICATES = Set.of(
            "innings_score", "all_out_for", "chased", "set_target",
            "runs_scored", "bowling_figures", "won_by", "lost_by");
    static final Set<String> FIELDS = Set.of("title", "description");
    static final Set<String> DECISIONS = Set.of("approved", "rejected");
    static final Set<String> STATUSES = Set.of("pending", "approved", "rejected");
    static final int MAX_LIST = 100;

    private final ClaimRepository claimRepository;
    private final ClaimReviewRepository reviewRepository;
    private final ArticleRepository articleRepository;
    private final EventMatchingClient eventMatchingClient;
    private final ExtractionRunService extractionRunService;

    public ClaimService(
            ClaimRepository claimRepository,
            ClaimReviewRepository reviewRepository,
            ArticleRepository articleRepository,
            EventMatchingClient eventMatchingClient,
            ExtractionRunService extractionRunService
    ) {
        this.claimRepository = claimRepository;
        this.reviewRepository = reviewRepository;
        this.articleRepository = articleRepository;
        this.eventMatchingClient = eventMatchingClient;
        this.extractionRunService = extractionRunService;
    }

    public record ClaimView(
            Long claimId,
            String subject,
            String predicate,
            String valueText,
            String unit,
            String quote,
            String field,
            LocalDateTime createdAt,
            Long articleId,
            String articleTitle,
            String articleUrl,
            String outlet,
            Long eventId,
            String status,
            long supportingArticles,
            long conflictingArticles
    ) {
    }

    /** Number of claims stored (0 when skipped, not cricket, or the AI service failed). */
    @Transactional
    public int recordClaims(Long articleId) {

        Article article = articleRepository.findById(articleId).orElse(null);

        if (article == null || claimRepository.hasClaims(articleId)) {
            return 0;
        }

        ClaimExtractionResponse response;

        try {
            response = eventMatchingClient.extractClaims(new ArticleText(article.getTitle(), article.getDescription()));
        } catch (RestClientException e) {
            log.warn("Claim extraction failed for article={}: {}", articleId, e.getMessage());
            return 0;
        }

        if (response == null || response.claims() == null || response.claims().isEmpty()) {
            return 0;
        }

        Long runId = extractionRunService.runId(RUN_KIND, response.extractorVersion(), ExtractionRunService.NO_PROMPT);
        Long eventId = article.getNewsEvent() == null ? null : article.getNewsEvent().getId();

        int stored = 0;

        for (ClaimExtractionResponse.Claim extracted : valid(response.claims())) {

            Claim claim = claimRepository.save(new Claim(
                    articleId,
                    eventId,
                    extracted.subject(),
                    extracted.subjectNormalized(),
                    extracted.predicate(),
                    BigDecimal.valueOf(extracted.value()),
                    extracted.valueText(),
                    extracted.unit(),
                    extracted.quote(),
                    extracted.start(),
                    extracted.end(),
                    extracted.field(),
                    runId
            ));
            stored++;

            if (eventId != null) {
                linkEvidence(claim);
            }
        }

        return stored;
    }

    // The same subject, predicate and unit in another article of the event:
    // the same value supports, another value refutes. Both directions.
    void linkEvidence(Claim claim) {

        for (Claim other : claimRepository.findComparable(
                claim.getEventId(), claim.getSubjectNormalized(), claim.getPredicate(),
                claim.getUnit(), claim.getArticleId())) {

            String label = evidenceLabel(claim.getValueText(), other.getValueText());
            claimRepository.insertEvidenceIfAbsent(claim.getId(), other.getArticleId(), label);
            claimRepository.insertEvidenceIfAbsent(other.getId(), claim.getArticleId(), label);
        }
    }

    static String evidenceLabel(String value, String otherValue) {
        return value.equalsIgnoreCase(otherValue) ? "SUPPORTS" : "REFUTES";
    }

    // Defensive: only well-formed claims from the AI service are stored.
    static List<ClaimExtractionResponse.Claim> valid(List<ClaimExtractionResponse.Claim> claims) {

        return claims.stream()
                .filter(c -> PREDICATES.contains(c.predicate()) && FIELDS.contains(c.field()))
                .filter(c -> notBlank(c.subject(), 200) && notBlank(c.subjectNormalized(), 200))
                .filter(c -> notBlank(c.valueText(), 30) && notBlank(c.unit(), 20) && notBlank(c.quote(), 500))
                .filter(c -> c.start() >= 0 && c.end() > c.start())
                .toList();
    }

    private static boolean notBlank(String text, int maxChars) {
        return text != null && !text.isBlank() && text.length() <= maxChars;
    }

    // =========================================================
    // REVIEW AND DISPLAY
    // =========================================================

    @Transactional(readOnly = true)
    public List<ClaimView> list(String status, Long eventId, int limit) {

        if (!STATUSES.contains(status)) {
            throw new IllegalArgumentException("status must be pending, approved or rejected");
        }

        return claimRepository.findWithStatus(status, eventId, Math.max(1, Math.min(limit, MAX_LIST)))
                .stream()
                .map(ClaimService::toView)
                .toList();
    }

    /** Empty when the claim doesn't exist (404). */
    @Transactional
    public Optional<ClaimReview> review(Long claimId, String decision, String note, String reviewer) {

        String normalized = decision == null ? "" : decision.trim().toLowerCase();

        if (!DECISIONS.contains(normalized)) {
            throw new IllegalArgumentException("decision must be approved or rejected");
        }

        if (!claimRepository.existsById(claimId)) {
            return Optional.empty();
        }

        String safeNote = note == null || note.isBlank() ? null : note.length() <= 500 ? note : note.substring(0, 500);

        return Optional.of(reviewRepository.save(new ClaimReview(claimId, normalized, reviewer, safeNote)));
    }

    static ClaimView toView(Object[] row) {

        return new ClaimView(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                (String) row[5],
                (String) row[6],
                toLocalDateTime(row[7]),
                ((Number) row[8]).longValue(),
                (String) row[9],
                (String) row[10],
                NewsEventService.outletOf((String) row[11]),
                row[12] == null ? null : ((Number) row[12]).longValue(),
                (String) row[13],
                ((Number) row[14]).longValue(),
                ((Number) row[15]).longValue()
        );
    }

    private static LocalDateTime toLocalDateTime(Object value) {

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        return (LocalDateTime) value;
    }
}
