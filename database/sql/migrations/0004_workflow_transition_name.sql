-- 0004_workflow_transition_name.sql
-- Adds a display name/label to workflow_transition (e.g. "Start" for
-- To Do -> In Progress, "Complete" for In Progress -> Done), so the workflow
-- can show a verb for the action rather than just the state pair.

ALTER TABLE workflow_transition
    ADD COLUMN name TEXT NOT NULL DEFAULT '';

-- Only backfills existing rows; new inserts must supply a name explicitly
-- from here on (enforced at the application layer too).
ALTER TABLE workflow_transition
    ALTER COLUMN name DROP DEFAULT;
