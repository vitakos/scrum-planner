package com.scrumplanner.core.customfield.dto;

import java.util.List;

public record FieldCatalogResponse(
        String workItemType,
        String workItemTypeName,
        List<PredefinedFieldResponse> predefinedFields,
        List<CustomFieldDefinitionResponse> customFields
) {
}
