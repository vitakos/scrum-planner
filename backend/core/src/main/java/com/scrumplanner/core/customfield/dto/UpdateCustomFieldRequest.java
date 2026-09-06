package com.scrumplanner.core.customfield.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateCustomFieldRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull String dataType,
        List<String> options
) {
}
