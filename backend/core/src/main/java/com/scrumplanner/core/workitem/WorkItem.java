package com.scrumplanner.core.workitem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Note: the underlying work_item table also has assignee_id, reporter_id and
 * sprint_id columns for people and sprints — still to come. They're
 * intentionally left unmapped here (all nullable in the DB); Hibernate's
 * schema validation only checks the columns an entity declares, so this is
 * safe to extend later without a migration. parent_id (hierarchy) and
 * content_ref (the MongoDB-backed content document, see
 * WorkItemContentService) are now mapped below.
 */
@Entity
@Table(name = "work_item")
public class WorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(name = "state_id", nullable = false)
    private UUID stateId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "content_ref")
    private String contentRef;

    @Column(nullable = false)
    private int seq;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected WorkItem() {
        // JPA
    }

    public WorkItem(UUID projectId, String type, String title, UUID stateId, int seq) {
        this(projectId, type, title, stateId, seq, null);
    }

    public WorkItem(UUID projectId, String type, String title, UUID stateId, int seq, UUID parentId) {
        this.projectId = projectId;
        this.type = type;
        this.title = title;
        this.stateId = stateId;
        this.seq = seq;
        this.parentId = parentId;
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public UUID getStateId() {
        return stateId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public String getContentRef() {
        return contentRef;
    }

    public void setContentRef(String contentRef) {
        this.contentRef = contentRef;
    }

    public int getSeq() {
        return seq;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void rename(String title) {
        this.title = title;
        this.updatedAt = OffsetDateTime.now();
    }

    public void moveToState(UUID stateId) {
        this.stateId = stateId;
        this.updatedAt = OffsetDateTime.now();
    }

    public void reparent(UUID parentId) {
        this.parentId = parentId;
        this.updatedAt = OffsetDateTime.now();
    }
}
