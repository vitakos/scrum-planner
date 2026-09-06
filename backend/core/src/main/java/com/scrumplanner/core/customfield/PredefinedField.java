package com.scrumplanner.core.customfield;

/**
 * A read-only, system-defined field on the work_item model (see
 * docs/backlog-data-model.md) — shown in the Custom Fields configuration
 * screen alongside the project's custom fields so admins can see the full
 * picture before adding new ones. Not persisted; the catalog is fixed code
 * (see PredefinedFieldCatalog).
 */
public record PredefinedField(String code, String name, String dataType, String description) {
}
