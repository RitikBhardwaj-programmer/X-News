-- X-NEWS V3: give NewsEvent a real identity beyond "whichever article was
-- the best pairwise match" (see the V3 plan, "Article-level -> event-centroid
-- level matching"). centroid_embedding is left nullable - it's backfilled in
-- V3 and, going forward, only ever set by the (separately added) online
-- centroid update logic.

ALTER TABLE news_events
    ADD COLUMN centroid_embedding vector(384),
    ADD COLUMN member_count integer NOT NULL DEFAULT 0,
    ADD COLUMN first_activity_at timestamp,
    ADD COLUMN last_activity_at timestamp,
    ADD COLUMN status varchar(20) NOT NULL DEFAULT 'OPEN';

CREATE INDEX IF NOT EXISTS idx_news_events_status
    ON news_events (status);
