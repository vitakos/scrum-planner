package com.scrumplanner.core.worktype.dto;

import com.scrumplanner.core.worktype.WorkItemTypeCatalog;

public record WorkItemTypeResponse(String code, String name, boolean system, int sortOrder) {
    public static WorkItemTypeResponse from(WorkItemTypeCatalog entity) {
        return new WorkItemTypeResponse(entity.getCode(), entity.getName(), entity.isSystem(), entity.getSortOrder());
    }
}
