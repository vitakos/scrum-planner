package com.scrumplanner.core.intakesession.dto;

// IntakeMessageRequest (AISC-100): DTO for creating an intake message.
public record IntakeMessageRequest(
    String sender,
    String text
) {}
