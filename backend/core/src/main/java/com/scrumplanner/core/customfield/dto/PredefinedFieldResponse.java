package com.scrumplanner.core.customfield.dto;

import com.scrumplanner.core.customfield.PredefinedField;

public record PredefinedFieldResponse(String code, String name, String dataType, String description) {
    public static PredefinedFieldResponse from(PredefinedField field) {
        return new PredefinedFieldResponse(field.code(), field.name(), field.dataType(), field.description());
    }
}
