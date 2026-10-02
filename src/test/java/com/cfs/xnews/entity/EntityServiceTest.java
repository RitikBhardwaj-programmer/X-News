package com.cfs.xnews.entity;

import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.dto.EntityExtractionResponse;
import com.cfs.xnews.event.dto.EntityExtractionResponse.Mention;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import com.cfs.xnews.provenance.ExtractionRunService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntityServiceTest {

    private NamedEntityRepository entities;
    private ArticleRepository articles;
    private EventMatchingClient client;
    private ExtractionRunService runs;
    private EntityService service;

    @BeforeEach
    void setUp() {
        entities = mock(NamedEntityRepository.class);
        articles = mock(ArticleRepository.class);
        client = mock(EventMatchingClient.class);
        runs = mock(ExtractionRunService.class);
        service = new EntityService(entities, articles, client, runs);

        when(articles.findById(1L)).thenReturn(Optional.of(
                new Article("Supreme Court quashes detention", "d", "http://example.com/1", "s", null)));
        when(runs.runId(any(), any(), any())).thenReturn(9L);
        when(entities.insertMentionIfAbsent(anyLong(), anyLong(), anyString(), anyString(), anyLong())).thenReturn(1);
    }

    private static Mention mention(String text, String type) {
        return new Mention(text, text.toLowerCase(), type, "title");
    }

    private static NamedEntity saved(long id) {
        NamedEntity entity = mock(NamedEntity.class);
        when(entity.getId()).thenReturn(id);
        return entity;
    }

    @Test
    void storesMentionsWithTheirRun() {

        when(client.extractEntities(any())).thenReturn(new EntityExtractionResponse(
                "rules-1", List.of(mention("Supreme Court", "name"))));
        when(entities.findIdByAlias("supreme court")).thenReturn(Optional.of(5L));

        assertThat(service.recordMentions(1L)).isEqualTo(1);

        verify(runs).runId("entity-extraction", "rules-1", ExtractionRunService.NO_PROMPT);
        verify(entities).insertMentionIfAbsent(1L, 5L, "Supreme Court", "title", 9L);
    }

    @Test
    void aNewAliasCreatesItsEntity() {

        when(entities.findIdByAlias("mulla afroz")).thenReturn(Optional.empty());
        NamedEntity created = saved(7L);
        when(entities.save(any())).thenReturn(created);
        when(entities.insertAliasIfAbsent(7L, "mulla afroz")).thenReturn(1);

        assertThat(service.resolve(mention("Mulla Afroz", "name"))).isEqualTo(7L);
        verify(entities, never()).delete(any());
    }

    @Test
    void anAliasClaimedConcurrentlyUsesTheOtherEntity() {

        when(entities.findIdByAlias("kerala"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(3L));
        NamedEntity ours = saved(8L);
        when(entities.save(any())).thenReturn(ours);
        when(entities.insertAliasIfAbsent(8L, "kerala")).thenReturn(0);

        assertThat(service.resolve(mention("Kerala", "name"))).isEqualTo(3L);
        verify(entities).delete(ours);
    }

    @Test
    void alreadyRecordedOrMissingArticlesAreSkipped() {

        when(entities.hasMentions(1L)).thenReturn(true);
        when(articles.findById(2L)).thenReturn(Optional.empty());

        assertThat(service.recordMentions(1L)).isZero();
        assertThat(service.recordMentions(2L)).isZero();
        verify(client, never()).extractEntities(any());
    }

    @Test
    void anAiServiceFailureStoresNothing() {

        when(client.extractEntities(any())).thenThrow(new ResourceAccessException("Read timed out"));

        assertThat(service.recordMentions(1L)).isZero();
        verify(entities, never()).insertMentionIfAbsent(anyLong(), anyLong(), anyString(), anyString(), anyLong());
    }

    @Test
    void malformedMentionsAreDropped() {

        List<Mention> kept = EntityService.valid(List.of(
                new Mention("India", "india", "team", "title"),
                new Mention("X", "x", "name", "title"),
                new Mention("Delhi", "delhi", "place", "title"),
                new Mention("Delhi", "delhi", "name", "body"),
                new Mention(" ", "blank", "name", "title")
        ));

        assertThat(kept).extracting(Mention::normalized).containsExactly("india");
        verify(entities, never()).findIdByAlias(eq("x"));
    }
}
