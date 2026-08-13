package com.cfs.xnews.news.source;



import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NewsSourceRepository
        extends JpaRepository<NewsSource, Long> {

    boolean existsByUrl(String url);

    List<NewsSource> findByEnabledTrue();
}
