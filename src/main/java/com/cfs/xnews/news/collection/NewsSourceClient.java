package com.cfs.xnews.news.collection;


import com.cfs.xnews.news.collection.dto.CollectedArticle;
import com.cfs.xnews.news.source.NewsSource;

import java.util.List;

public interface NewsSourceClient {

    List<CollectedArticle> fetchArticles(
            NewsSource source
    );
}
