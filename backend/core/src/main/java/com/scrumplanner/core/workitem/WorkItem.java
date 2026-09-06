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
 * Note: the underlying work_item table also has parent_id, assignee_id,
 * reporter_id, sprint_id and content_ref columns for hierarchy, people,
 * sprints and the MongoDB-backed description/content document — all still
 * to come. They're intentionally left unmapped here (all nullable in the
 * DB); Hibernate's schema validation only checks the columns an entity
 * declares, so this is safe to extend later without a migration.
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
        this.projectId = projectId;
        this.type = type;
        this.title = title;
        this.stateId = stateId;
        this.seq = seq;
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
}
