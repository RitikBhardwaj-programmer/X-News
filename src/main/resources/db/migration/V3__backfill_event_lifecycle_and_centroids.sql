-- Backfill for existing V2 events, computed from their current members
-- (see the V3 plan, "Backfill job to compute initial centroids for
-- existing V2 events from their current members").

-- member_count + activity window come from ALL attached articles,
-- regardless of whether they have an embedding yet.
UPDATE news_events e
SET
    member_count = sub.cnt,
    first_activity_at = sub.min_published,
    last_activity_at = sub.max_published
FROM (
    SELECT
        news_event_id,
        count(*) AS cnt,
        min(published_at) AS min_published,
        max(published_at) AS max_published
    FROM articles
    WHERE news_event_id IS NOT NULL
    GROUP BY news_event_id
) sub
WHERE e.id = sub.news_event_id;

-- centroid_embedding only comes from articles that actually have an
-- embedding. l2_normalize keeps this consistent with the online running-mean
-- update, which also normalizes after each attach.
UPDATE news_events e
SET centroid_embedding = sub.centroid
FROM (
    SELECT
        news_event_id,
        l2_normalize(avg(embedding)) AS centroid
    FROM articles
    WHERE news_event_id IS NOT NULL
      AND embedding IS NOT NULL
    GROUP BY news_event_id
) sub
WHERE e.id = sub.news_event_id;

-- Defensive: an event with no attached articles at all (shouldn't happen
-- today, since events are always created with a founding article) still
-- needs a non-null activity window before the NOT NULL constraint below.
UPDATE news_events
SET
    first_activity_at = COALESCE(first_activity_at, created_at),
    last_activity_at = COALESCE(last_activity_at, created_at)
WHERE first_activity_at IS NULL
   OR last_activity_at IS NULL;

ALTER TABLE news_events
    ALTER COLUMN first_activity_at SET NOT NULL,
    ALTER COLUMN last_activity_at SET NOT NULL;
