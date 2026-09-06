-- 0003_workflow_state_ordering.sql
-- Adds display ordering and an "initial state" marker to workflow_state, so
-- the UI can render states in a stable order and a new work item can be
-- created in the right starting state for its workflow.

ALTER TABLE workflow_state
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0,
    ADD COLUMN is_initial BOOLEAN NOT NULL DEFAULT false;

-- At most one initial state per workflow.
CREATE UNIQUE INDEX uq_workflow_state_single_initial
    ON workflow_state (workflow_id)
    WHERE is_initial;
