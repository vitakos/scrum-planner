package com.scrumplanner.core.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "workflow_definition")
public class WorkflowDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "work_item_type", nullable = false)
    private String workItemType;

    @Column(nullable = false)
    private String name;

    protected WorkflowDefinition() {
        // JPA
    }

    public WorkflowDefinition(UUID projectId, String workItemType, String name) {
        this.projectId = projectId;
        this.workItemType = workItemType;
        this.name = name;
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
}
