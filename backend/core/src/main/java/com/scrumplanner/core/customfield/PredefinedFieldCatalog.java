package com.scrumplanner.core.customfield;

import java.util.List;

/**
 * The fixed set of built-in fields every work item carries, mirroring the
 * `work_item` columns (mapped and still-to-come, see WorkItem's class
 * comment) and the description/content fields promised by
 * docs/backlog-data-model.md. Same set for every work item type today,
 * since all types share the single polymorphic work_item table.
 */
public final class PredefinedFieldCatalog {

    private static final List<PredefinedField> FIELDS = List.of(
            new PredefinedField("key", "Key", "TEXT",
                    "Unique per-project identifier (e.g. SPAI-1), assigned automatically."),
            new PredefinedField("title", "Title", "TEXT",
                    "Short summary of the work item."),
            new PredefinedField("type", "Type", "TEXT",
                    "The work item type (Epic, Feature, User Story, Task, Bug, Test Case, Test Run)."),
            new PredefinedField("state", "State", "SINGLE_SELECT",
                    "Current workflow state — configured in the Workflow tab."),
            new PredefinedField("parent", "Parent", "REFERENCE",
                    "Parent work item in the hierarchy (e.g. a Feature under an Epic)."),
            new PredefinedField("assignee", "Assignee", "USER",
                    "Person currently responsible for the work item."),
            new PredefinedField("reporter", "Reporter", "USER",
                    "Person who created/reported the work item."),
            new PredefinedField("sprint", "Sprint", "REFERENCE",
                    "Sprint the work item is scheduled in."),
            new PredefinedField("description", "Description", "RICH_TEXT",
                    "Free-form description/body, stored in the flexible content document."),
            new PredefinedField("createdAt", "Created At", "DATE",
                    "Timestamp when the work item was created."),
            new PredefinedField("updatedAt", "Updated At", "DATE",
                    "Timestamp when the work item was last updated.")
    );

    private PredefinedFieldCatalog() {
    }

    public static List<PredefinedField> list() {
        return FIELDS;
    }
}
