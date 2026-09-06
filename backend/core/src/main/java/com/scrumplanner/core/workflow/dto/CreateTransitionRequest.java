package com.scrumplanner.core.workflow.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTransitionRequest(
        @NotNull UUID fromStateId,
        @NotNull UUID toStateId
) {
}
