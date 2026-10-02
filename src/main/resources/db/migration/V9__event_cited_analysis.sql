-- X-NEWS V4 roadmap step 4: the event analysis as cited evidence. Agreed
-- facts and per-outlet framing, each sentence citing the event's article
-- ids ("cite or drop"); produced by the run in summary_run_id. summary and
-- bias_analysis stay as plain-text views of it for older clients.
-- Additive only.

ALTER TABLE news_events
    ADD COLUMN analysis jsonb;
