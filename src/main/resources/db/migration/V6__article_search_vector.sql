-- X-NEWS V4 roadmap step 5: hybrid search. Postgres full-text search over
-- article titles (weight A) and descriptions (weight B), fused at query time
-- with the existing pgvector embeddings. The column is generated, so it
-- stays in step with the text without application code. Additive only.

ALTER TABLE articles
    ADD COLUMN search_vector tsvector
        GENERATED ALWAYS AS (
            setweight(to_tsvector('english', coalesce(title, '')), 'A')
                || setweight(to_tsvector('english', coalesce(description, '')), 'B')
        ) STORED;

CREATE INDEX idx_articles_search_vector
    ON articles USING gin (search_vector);
