package com.cfs.xnews.processing;

import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.dto.ArticleText;
import com.cfs.xnews.event.dto.EventCandidateV2;
import com.cfs.xnews.event.dto.EventMatchV2Request;
import com.cfs.xnews.event.dto.EventMatchV2Response;
import com.cfs.xnews.event.dto.EventMatchV2Result;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventMatcherV2Test {

    private static EventMatchV2Result result(long eventId, double probability) {
        return new EventMatchV2Result(eventId, probability, Map.of());
    }

    @Test
    void bestIsTheHighestProbabilityAndTheFirstWinsATie() {

        assertThat(EventMatcherV2.best(List.of(result(1, 0.2), result(2, 0.9), result(3, 0.5))).eventId())
                .isEqualTo(2);
        assertThat(EventMatcherV2.best(List.of(result(1, 0.9), result(2, 0.9))).eventId())
                .isEqualTo(1);
        assertThat(EventMatcherV2.best(List.of())).isNull();
    }

    @Test
    void attachesOnlyAtOrAboveTheThreshold() {

        assertThat(EventMatcherV2.attaches(result(1, 0.98), 0.98)).isTrue();
        assertThat(EventMatcherV2.attaches(result(1, 0.99), 0.98)).isTrue();
        assertThat(EventMatcherV2.attaches(result(1, 0.979), 0.98)).isFalse();
        assertThat(EventMatcherV2.attaches(null, 0.98)).isFalse();
    }

    @Test
    void anEmptyResponseCountsAsAFailedCall() {

        EventMatchingClient client = mock(EventMatchingClient.class);
        when(client.predictV2(any())).thenReturn(null);

        EventMatcherV2 matcher = new EventMatcherV2(
                mock(ArticleRepository.class), client, mock(VocabularyRefitScheduler.class));
        Article article = new Article("t", "d", "http://example.com/a", "s", null);

        assertThatThrownBy(() -> matcher.decide(article, new float[]{1f}, "[1]", List.of()))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void aDefaultVocabularyRequestsARefit() {

        EventMatchingClient client = mock(EventMatchingClient.class);
        when(client.predictV2(any())).thenReturn(new EventMatchV2Response("v2", "default", 0.99, List.of()));
        VocabularyRefitScheduler refits = mock(VocabularyRefitScheduler.class);

        EventMatcherV2.Outcome outcome = new EventMatcherV2(mock(ArticleRepository.class), client, refits)
                .decide(new Article("t", "d", "http://example.com/a", "s", null), new float[]{1f}, "[1]", List.of());

        verify(refits).requestRefit();
        assertThat(outcome.chosenEventId()).isNull();
        assertThat(outcome.bestProbability()).isNull();
        assertThat(outcome.modelVersion()).isEqualTo("v2/default");
    }

    @Test
    void cosineMatchesTheSqlSimilarity() {

        float[] a = {1, 0, 0};
        float[] b = {1, 1, 0};

        assertThat(EventMatcherV2.cosine(a, a)).isCloseTo(1.0, within(1e-12));
        assertThat(EventMatcherV2.cosine(a, b)).isCloseTo(Math.sqrt(0.5), within(1e-12));
        assertThat(EventMatcherV2.cosine(a, new float[]{0, 0, 0})).isEqualTo(0.0);
    }

    @Test
    void truncateKeepsShortTextAndNull() {

        assertThat(EventMatcherV2.truncate("abcdef", 3)).isEqualTo("abc");
        assertThat(EventMatcherV2.truncate("abc", 3)).isEqualTo("abc");
        assertThat(EventMatcherV2.truncate(null, 3)).isNull();
    }

    // The AI service's /predict/v2 is Python (pydantic, snake_case);
    // RestClient serialises with Jackson 3.
    @Test
    void requestJsonUsesTheAiServiceFieldNames() {

        String json = JsonMapper.builder().build().writeValueAsString(new EventMatchV2Request(
                "title",
                null,
                List.of(new EventCandidateV2(7L, 0.9, 1.0, 0.95, 0.7, 0.9, 0.8,
                        List.of(new ArticleText("member", "text"))))
        ));

        assertThat(json).contains(
                "\"title\":\"title\"",
                "\"event_id\":7",
                "\"similarity\":0.9",
                "\"temporal_score\":1.0",
                "\"member_max\":0.95",
                "\"member_min\":0.7",
                "\"member_top3\":0.9",
                "\"member_newest\":0.8",
                "\"members\":[{\"title\":\"member\",\"description\":\"text\"}]"
        );
    }

    @Test
    void responseJsonFromTheAiServiceIsRead() {

        EventMatchV2Response response = JsonMapper.builder().build().readValue("""
                {"model_version": "v2-b-2026-10-02", "vocabulary_version": "default", "threshold": 0.98,
                 "results": [{"event_id": 7, "probability": 0.99,
                              "features": {"similarity": 0.9, "text_tfidf_max": 0.4}}]}
                """, EventMatchV2Response.class);

        assertThat(response.modelVersion()).isEqualTo("v2-b-2026-10-02");
        assertThat(response.vocabularyVersion()).isEqualTo("default");
        assertThat(response.threshold()).isEqualTo(0.98);
        assertThat(response.results()).singleElement().satisfies(r -> {
            assertThat(r.eventId()).isEqualTo(7L);
            assertThat(r.features()).containsEntry("text_tfidf_max", 0.4);
        });
    }
}
