-- 0007_workflow_state_description.sql
-- Adds an optional description to workflow_state so project admins can
-- explain what a state means (e.g. entry/exit criteria) to the team.

ALTER TABLE workflow_state
    ADD COLUMN description TEXT;
