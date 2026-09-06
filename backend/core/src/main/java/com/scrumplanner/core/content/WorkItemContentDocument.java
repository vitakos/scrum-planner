package com.scrumplanner.core.content;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * The MongoDB document behind a work item's rich content body (see
 * docs/backlog-data-model.md). Only the {@code description} field is used
 * here — it holds the work item's Markdown-formatted body text, referenced
 * from Postgres via {@code work_item.content_ref} (this document's id).
 * {@code custom_fields}/{@code comments}/{@code attachments} are part of the
 * same collection's schema but aren't populated by this service yet.
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

    protected WorkItemContentDocument() {
        // Spring Data
    }

    public WorkItemContentDocument(String workItemId, String description) {
        this.workItemId = workItemId;
        this.description = description;
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
}
