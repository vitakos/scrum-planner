package com.scrumplanner.core.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTransitionRequest(
        @NotBlank @Size(max = 100) String name
) {
}
