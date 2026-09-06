package com.scrumplanner.core.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record CreateWorkItemRequest(
        @NotBlank String type,
        @NotBlank @Size(max = 500) String title,
        UUID parentId,
        @Size(max = 200_000) String content,
        Map<String, Object> customFields
) {
}
