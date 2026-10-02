package com.cfs.xnews.processing;

import com.cfs.xnews.claim.ClaimService;
import com.cfs.xnews.entity.EntityService;
import com.cfs.xnews.kafka.ArticleEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArticleEventConsumerTest {

    @Test
    void entityAndClaimFailuresDoNotFailTheMessage() {

        ArticleProcessingService processing = mock(ArticleProcessingService.class);
        EntityService entities = mock(EntityService.class);
        ClaimService claims = mock(ClaimService.class);
        doThrow(new IllegalStateException("db hiccup")).when(entities).recordMentions(4L);
        doThrow(new IllegalStateException("db hiccup")).when(claims).recordClaims(4L);

        ArticleEvent event = new ArticleEvent(4L, "t", "d", "u", "s", null);

        assertThatCode(() -> new ArticleEventConsumer(processing, entities, claims).consume(event))
                .doesNotThrowAnyException();
        verify(processing).process(event);
        verify(entities).recordMentions(4L);
        verify(claims).recordClaims(4L);
    }
}
