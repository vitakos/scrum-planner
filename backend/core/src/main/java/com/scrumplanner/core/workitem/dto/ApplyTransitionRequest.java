package com.scrumplanner.core.workitem.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ApplyTransitionRequest(
        @NotNull UUID transitionId
) {
}
