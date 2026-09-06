package com.scrumplanner.core.workflow.dto;

import com.scrumplanner.core.workflow.WorkflowState;

import java.util.UUID;

public record WorkflowStateResponse(
        UUID id, String name, String category, int sortOrder, boolean initial, String color, String description
) {
    public static WorkflowStateResponse from(WorkflowState state) {
        return new WorkflowStateResponse(
                state.getId(), state.getName(), state.getCategory().name(), state.getSortOrder(), state.isInitial(),
                state.getColor(), state.getDescription()
        );
    }
}
