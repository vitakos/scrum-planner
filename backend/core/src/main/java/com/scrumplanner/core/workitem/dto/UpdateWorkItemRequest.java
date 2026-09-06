package com.scrumplanner.core.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateWorkItemRequest(
        @NotBlank @Size(max = 500) String title,
        UUID parentId
) {
}
