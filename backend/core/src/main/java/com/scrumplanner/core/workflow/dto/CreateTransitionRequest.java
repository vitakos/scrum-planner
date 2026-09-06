package com.scrumplanner.core.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateTransitionRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull UUID fromStateId,
        @NotNull UUID toStateId
) {
}
