package com.scrumplanner.core.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull String category
) {
}
