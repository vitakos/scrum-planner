package com.scrumplanner.core.project.dto;

import com.scrumplanner.core.project.Project;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String key,
        String name,
        String description,
        OffsetDateTime createdAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getKey(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt()
        );
    }
}
