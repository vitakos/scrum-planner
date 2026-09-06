package com.scrumplanner.core.workflow.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * The full list of state ids for a workflow, in the desired display order.
 * Must contain exactly the same ids as the workflow's current states (no
 * more, no fewer, no duplicates) — the new sort order is the index of each
 * id in this list.
 */
public record ReorderStatesRequest(@NotEmpty List<UUID> stateIds) {
}
