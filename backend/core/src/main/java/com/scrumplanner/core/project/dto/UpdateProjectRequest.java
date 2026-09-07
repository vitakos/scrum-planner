package com.scrumplanner.core.project.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 2048)
        @Pattern(
                regexp = "^$|^(?:https?|git|ssh)://[\\w.~-]+(?::\\d+)?(?:/[\\w./~%-]*)?$|^[\\w.-]+@[\\w.-]+:[\\w./~-]+(?:\\.git)?$",
                message = "must be a valid http(s), git or ssh repository URL"
        )
        String repositoryUrl
) {
}
