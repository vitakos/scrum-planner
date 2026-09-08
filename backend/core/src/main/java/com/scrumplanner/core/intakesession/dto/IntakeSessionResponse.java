package com.scrumplanner.core.intakesession.dto;

import java.time.OffsetDateTime;
import java.util.List;

// IntakeSessionResponse (AISC-100): DTO for intake session response to API callers.
public record IntakeSessionResponse(
    String id,
    String projectId,
    List<IntakeMessageResponse> messages,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public record IntakeMessageResponse(
        String id,
        String sender,
        String text,
        OffsetDateTime timestamp,
        List<IntakeAttachmentResponse> attachments
    ) {}

    public record IntakeAttachmentResponse(
        String id,
        String name,
        int size
    ) {}
}
