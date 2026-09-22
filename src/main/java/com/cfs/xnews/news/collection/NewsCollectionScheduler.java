package com.cfs.xnews.news.collection;



import com.cfs.xnews.news.source.NewsSource;
import com.cfs.xnews.news.source.NewsSourceRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsCollectionScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectionScheduler.class);

    private final NewsSourceRepository sourceRepository;
    private final NewsCollectionService collectionService;

    public NewsCollectionScheduler(
            NewsSourceRepository sourceRepository,
            NewsCollectionService collectionService
    ) {
        this.sourceRepository = sourceRepository;
        this.collectionService = collectionService;
    }

    @Scheduled(fixedRate = 600000)
    public void collectNews() {

        for (NewsSource source :
                sourceRepository.findByEnabledTrue()) {

            try {

                int count =
                        collectionService
                                .collectFromSource(
                                        source.getId()
                                );

                log.info(
                        "Collected {} articles from {}",
                        count,
                        source.getName()
                );

            } catch (Exception e) {

                log.error(
                        "Failed to collect from {}: {}",
                        source.getName(),
                        e.getMessage(),
                        e
                );
            }
        }
    }
}