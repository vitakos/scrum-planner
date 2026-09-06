package com.scrumplanner.core.workflow.dto;

import java.util.List;
import java.util.UUID;

public record WorkflowResponse(
        UUID workflowId,
        String workItemType,
        String workItemTypeName,
        String name,
        List<WorkflowStateResponse> states,
        List<WorkflowTransitionResponse> transitions
) {
}
