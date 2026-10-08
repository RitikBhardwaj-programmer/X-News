-- X-NEWS V4: Gemini auto-titles for events that reach five articles.
-- The original title (the first article's headline) is never overwritten;
-- the generated title sits next to it with its provenance. Additive only.

ALTER TABLE news_events
    -- a neutral title that cited at least two of the event's articles
    ADD COLUMN generated_title varchar(200),
    -- which model, prompt and code produced it
    ADD COLUMN title_run_id bigint REFERENCES extraction_runs (id),
    -- when titling was last attempted (UTC); set on every attempt that
    -- reached Gemini, also when the reply was discarded, so an event is
    -- tried once and the daily request budget survives restarts
    ADD COLUMN title_attempted_at timestamp(6) without time zone;
