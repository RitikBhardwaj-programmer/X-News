-- Built after the backfill (V3) so it isn't indexing an all-null column.
CREATE INDEX IF NOT EXISTS idx_news_events_centroid_hnsw
    ON news_events USING hnsw (centroid_embedding vector_cosine_ops);
