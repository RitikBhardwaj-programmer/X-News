package com.cfs.xnews.search;

import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.dto.EmbeddingResponse;
import com.cfs.xnews.news.articles.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SearchServiceTest {

    private static final LocalDateTime NOON = LocalDateTime.of(2026, 10, 2, 12, 0);

    private final ArticleRepository repository = mock(ArticleRepository.class);
    private final EventMatchingClient client = mock(EventMatchingClient.class);
    private final SearchService service = new SearchService(repository, client);

    private static Object[] row(long id, Long eventId, double score) {
        return new Object[]{id, "Title " + id, "The Hindu -> India", "http://example.com/" + id,
                Timestamp.valueOf(NOON), eventId, score};
    }

    @Test
    void hybridSearchWhenTheQueryCanBeEmbedded() {

        when(client.generateEmbeddingWithTimeout("kohli century"))
                .thenReturn(new EmbeddingResponse(Collections.nCopies(384, 0.1)));
        when(repository.searchHybrid(eq("kohli century"), startsWith("[0.1,0.1"), eq(20)))
                .thenReturn(List.<Object[]>of(row(7, 3L, 0.032), row(8, null, 0.016)));

        SearchResponse response = service.search("  kohli century ", 20);

        assertThat(response.query()).isEqualTo("kohli century");
        assertThat(response.mode()).isEqualTo("hybrid");
        assertThat(response.results()).extracting(SearchResponse.Result::articleId).containsExactly(7L, 8L);
        assertThat(response.results().get(0).eventId()).isEqualTo(3L);
        assertThat(response.results().get(0).publishedAt()).isEqualTo(NOON);
        assertThat(response.results().get(1).eventId()).isNull();
        verify(repository, never()).searchText(anyString(), anyInt());
    }

    @Test
    void fullTextOnlyWhenTheAiServiceIsDown() {

        when(client.generateEmbeddingWithTimeout("monsoon"))
                .thenThrow(new ResourceAccessException("Read timed out"));
        when(repository.searchText("monsoon", 20)).thenReturn(List.<Object[]>of(row(1, 2L, 0.4)));

        SearchResponse response = service.search("monsoon", 20);

        assertThat(response.mode()).isEqualTo("text");
        assertThat(response.results()).hasSize(1);
    }

    @Test
    void limitIsClampedToOneToFifty() {

        when(client.generateEmbeddingWithTimeout("x")).thenReturn(new EmbeddingResponse(List.of()));

        service.search("x", 500);
        verify(repository).searchText("x", SearchService.MAX_LIMIT);

        service.search("x", 0);
        verify(repository).searchText("x", 1);
    }

    @Test
    void blankOrOverlongQueriesAreRejected() {

        assertThatThrownBy(() -> service.search("   ", 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search(null, 20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search("a".repeat(201), 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("200");
        verifyNoInteractions(repository, client);
    }
}
