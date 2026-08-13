package com.cfs.xnews.news.collection;



import com.cfs.xnews.news.collection.dto.CollectedArticle;
import com.cfs.xnews.news.source.NewsSource;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class RssNewsSourceClient
        implements NewsSourceClient {

    @Override
    public List<CollectedArticle> fetchArticles(
            NewsSource source
    ) {

        try {

            URL feedUrl = new URL(source.getUrl());

            SyndFeedInput input = new SyndFeedInput();

            SyndFeed feed =
                    input.build(new XmlReader(feedUrl));

            return feed.getEntries()
                    .stream()
                    .map(entry ->
                            mapToCollectedArticle(
                                    entry,
                                    source
                            )
                    )
                    .filter(article ->
                            article.title() != null
                                    && !article.title().isBlank()
                                    && article.url() != null
                                    && !article.url().isBlank()
                    )
                    .toList();

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to fetch RSS feed: "
                            + source.getUrl(),
                    e
            );
        }
    }

    private CollectedArticle mapToCollectedArticle(
            SyndEntry entry,
            NewsSource source
    ) {

        LocalDateTime publishedAt = null;

        if (entry.getPublishedDate() != null) {

            publishedAt =
                    entry.getPublishedDate()
                            .toInstant()
                            .atZone(
                                    ZoneId.systemDefault()
                            )
                            .toLocalDateTime();
        }

        return new CollectedArticle(
                entry.getTitle(),
                entry.getDescription() != null
                        ? entry.getDescription()
                        .getValue()
                        : null,
                entry.getLink(),
                source.getName(),
                publishedAt
        );
    }
}
