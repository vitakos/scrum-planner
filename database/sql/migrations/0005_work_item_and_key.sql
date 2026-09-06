-- 0005_work_item_and_key.sql
-- Supports the backlog feature: a per-project incrementing sequence used to
-- build a human-readable work item key (e.g. "SPAI-1"), as hinted at in the
-- project wizard UI.

ALTER TABLE project
    ADD COLUMN work_item_seq INT NOT NULL DEFAULT 0;

ALTER TABLE work_item
    ADD COLUMN seq INT NOT NULL DEFAULT 0;

-- Only backfills existing rows (there are none yet, since work items didn't
-- exist as a feature before this migration); new inserts must supply seq
-- explicitly from here on.
ALTER TABLE work_item
    ALTER COLUMN seq DROP DEFAULT;

ALTER TABLE work_item
    ADD CONSTRAINT uq_work_item_project_seq UNIQUE (project_id, seq);
