package com.scrumplanner.core.workitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkItemRequest(
        @NotBlank String type,
        @NotBlank @Size(max = 500) String title
) {
}
