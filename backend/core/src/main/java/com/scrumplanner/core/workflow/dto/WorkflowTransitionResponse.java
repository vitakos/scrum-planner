package com.scrumplanner.core.workflow.dto;

import com.scrumplanner.core.workflow.WorkflowTransition;

import java.util.UUID;

public record WorkflowTransitionResponse(UUID id, UUID fromStateId, UUID toStateId) {
    public static WorkflowTransitionResponse from(WorkflowTransition transition) {
        return new WorkflowTransitionResponse(transition.getId(), transition.getFromStateId(), transition.getToStateId());
    }
}
