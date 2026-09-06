package com.scrumplanner.core.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull String category,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "color must be a hex value like #a1b2c3") String color
) {
}
