package com.scrumplanner.core.workflow;

/**
 * Mirrors the chk_workflow_state_category CHECK constraint on
 * workflow_state.category. Kept as a small fixed Java enum (unlike work item
 * types) since these three buckets drive metrics/reporting logic directly.
 */
public enum WorkflowStateCategory {
    to_do,
    in_progress,
    done
}
