package com.cfs.xnews.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class KafkaProducer {

    private final KafkaTemplate<String, ArticleEvent> kafkaTemplate;

    public KafkaProducer(
            KafkaTemplate<String, ArticleEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Inside a transaction the message is sent only after commit, so the
     * consumer can never read an article that is not in the database yet
     * (and nothing is sent if the transaction rolls back).
     */
    public void publishArticle(ArticleEvent event) {

        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            send(event);
                        }
                    }
            );
            return;
        }

        send(event);
    }

    private void send(ArticleEvent event) {

        kafkaTemplate.send(
                KafkaTopicConfig.ARTICLE_TOPIC,
                String.valueOf(event.articleId()),
                event
        );
    }
}
