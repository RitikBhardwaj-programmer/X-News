package com.cfs.xnews.kafka;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KafkaProducerTest {

    @Mock
    private KafkaTemplate<String, ArticleEvent> kafkaTemplate;

    private final ArticleEvent event = new ArticleEvent(
            42L, "title", "description", "http://example.com/a", "source", LocalDateTime.now()
    );

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishArticle_sendsImmediatelyOutsideATransaction() {

        new KafkaProducer(kafkaTemplate).publishArticle(event);

        verify(kafkaTemplate).send(KafkaTopicConfig.ARTICLE_TOPIC, "42", event);
    }

    @Test
    void publishArticle_insideATransaction_sendsOnlyAfterCommit() {

        TransactionSynchronizationManager.initSynchronization();

        new KafkaProducer(kafkaTemplate).publishArticle(event);

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        verify(kafkaTemplate).send(KafkaTopicConfig.ARTICLE_TOPIC, "42", event);
    }

    @Test
    void publishArticle_insideATransactionThatRollsBack_neverSends() {

        TransactionSynchronizationManager.initSynchronization();

        new KafkaProducer(kafkaTemplate).publishArticle(event);

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }
}
