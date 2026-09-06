package com.scrumplanner.core.customfield.dto;

import com.scrumplanner.core.customfield.CustomFieldDefinition;

import java.util.List;
import java.util.UUID;

public record CustomFieldDefinitionResponse(
        UUID id, String workItemType, String name, String dataType, List<String> options
) {
    public static CustomFieldDefinitionResponse from(CustomFieldDefinition field) {
        return new CustomFieldDefinitionResponse(
                field.getId(), field.getWorkItemType(), field.getName(), field.getDataType(), field.getOptions()
        );
    }
}
