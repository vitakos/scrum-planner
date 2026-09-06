package com.scrumplanner.core.customfield;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.UUID;

/**
 * Definition of a custom field configured for a work item type within a
 * project (name / data type / options). Field *values* are not stored here
 * — per docs/backlog-data-model.md they live in each work item's MongoDB
 * work_item_content document, keyed by this field's name.
 */
@Entity
@Table(name = "custom_field_definition")
public class CustomFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "work_item_type", nullable = false)
    private String workItemType;

    @Column(nullable = false)
    private String name;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    /**
     * Selectable values for SINGLE_SELECT / MULTI_SELECT fields; null for
     * every other data type. Mapped straight to the jsonb `options` column
     * added in 0001_init_schema.sql — no extra JSON library needed, Hibernate
     * 6 handles this natively via @JdbcTypeCode(SqlTypes.JSON).
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> options;

    protected CustomFieldDefinition() {
        // JPA
    }

    public CustomFieldDefinition(UUID projectId, String workItemType, String name, String dataType, List<String> options) {
        this.projectId = projectId;
        this.workItemType = workItemType;
        this.name = name;
        this.dataType = dataType;
        this.options = options;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getWorkItemType() {
        return workItemType;
    }

    public String getName() {
        return name;
    }

    public String getDataType() {
        return dataType;
    }

    public List<String> getOptions() {
        return options;
    }

    public void update(String name, String dataType, List<String> options) {
        this.name = name;
        this.dataType = dataType;
        this.options = options;
    }
}
