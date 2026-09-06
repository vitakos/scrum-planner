package com.scrumplanner.core.workitem.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record WorkItemResponse(
        UUID id,
        String key,
        UUID projectId,
        String type,
        String typeName,
        String title,
        UUID stateId,
        String stateName,
        String stateCategory,
        UUID parentId,
        String parentKey,
        String parentTitle,
        int childCount,
        List<AvailableTransitionResponse> availableTransitions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
