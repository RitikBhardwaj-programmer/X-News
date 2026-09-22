package com.cfs.xnews.common;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Stop-gap until Flyway migrations land. Runs after context refresh (so
 * Hibernate's ddl-auto=update has already created the articles table) and
 * is idempotent, safe to run on every startup.
 * <p>
 * Also covers the {@code unique = true} on {@link com.cfs.xnews.news.articles.Article#getUrl()}:
 * verified against a real dev database that ddl-auto=update adds missing
 * columns/tables but does not reliably add a unique constraint to an
 * already-existing column, so it's enforced here via a unique index instead.
 */
@Component
public class DatabaseIndexInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseIndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {

        jdbcTemplate.execute(
                """
                CREATE INDEX IF NOT EXISTS idx_articles_embedding_hnsw
                ON articles USING hnsw (embedding vector_cosine_ops)
                """
        );

        jdbcTemplate.execute(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS idx_articles_url_unique
                ON articles (url)
                """
        );
    }
}
