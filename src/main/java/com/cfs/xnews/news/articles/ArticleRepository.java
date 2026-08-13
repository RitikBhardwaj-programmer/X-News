package com.cfs.xnews.news.articles;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleRepository
        extends JpaRepository<Article, Long> {

    boolean existsByUrl(String url);

    Optional<Article> findByUrl(String url);
}
