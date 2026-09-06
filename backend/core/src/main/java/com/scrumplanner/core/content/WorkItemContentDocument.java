package com.scrumplanner.core.content;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Map;

/**
 * The MongoDB document behind a work item's flexible content (see
 * docs/backlog-data-model.md), referenced from Postgres via
 * {@code work_item.content_ref} (this document's id).
 * {@code comments}/{@code attachments} are part of the same collection's
 * schema but aren't populated by this service yet.
 */
@Document(collection = "work_item_content")
public class WorkItemContentDocument {

    @Id
    private String id;

    @Field("work_item_id")
    private String workItemId;

    /**
     * Markdown source text (never HTML) for the work item's description /
     * rich content. Generic on purpose — also used for gap-analysis notes
     * and drafted-item rationale by the future Intake Assistant epic.
     */
    private String description;

    /**
     * Custom field values, keyed by CustomFieldDefinition.name — validated
     * against that project/work-item-type's definitions in WorkItemService
     * before being written here.
     */
    @Field("custom_fields")
    private Map<String, Object> customFields;

    protected WorkItemContentDocument() {
        // Spring Data
    }

    public WorkItemContentDocument(String workItemId) {
        this.workItemId = workItemId;
    }

    public String getId() {
        return id;
    }

    public String getWorkItemId() {
        return workItemId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
}
