package com.scrumplanner.core.workitem.dto;

import java.util.UUID;

public record AvailableTransitionResponse(UUID id, String name, UUID toStateId, String toStateName) {
}
