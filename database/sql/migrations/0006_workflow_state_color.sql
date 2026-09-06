-- 0006_workflow_state_color.sql
-- Adds an optional custom color to workflow_state. NULL means "use the
-- default color for this state's category" (to_do: gray, in_progress:
-- orange, done: green), resolved at the application layer.

ALTER TABLE workflow_state
    ADD COLUMN color TEXT;

ALTER TABLE workflow_state
    ADD CONSTRAINT chk_workflow_state_color_hex CHECK (color IS NULL OR color ~ '^#[0-9a-fA-F]{6}$');
