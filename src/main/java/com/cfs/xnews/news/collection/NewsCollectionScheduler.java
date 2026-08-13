package com.cfs.xnews.news.collection;



import com.cfs.xnews.news.source.NewsSource;
import com.cfs.xnews.news.source.NewsSourceRepository;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsCollectionScheduler {

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

                System.out.println(
                        "Collected " + count +
                                " articles from " +
                                source.getName()
                );

            } catch (Exception e) {

                System.err.println(
                        "Failed to collect from " +
                                source.getName() +
                                ": " +
                                e.getMessage()
                );
            }
        }
    }
}