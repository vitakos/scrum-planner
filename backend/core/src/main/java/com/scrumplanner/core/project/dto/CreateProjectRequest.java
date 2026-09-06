package com.scrumplanner.core.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank
        @Size(max = 10)
        @Pattern(regexp = "^[A-Z][A-Z0-9]*$", message = "must be 2-10 uppercase letters/digits, starting with a letter")
        String key,

        @NotBlank
        @Size(max = 200)
        String name,

        @Size(max = 2000)
        String description
) {
}
