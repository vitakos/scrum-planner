package com.scrumplanner.core.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "workflow_state")
public class WorkflowState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStateCategory category;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_initial", nullable = false)
    private boolean initial;

    protected WorkflowState() {
        // JPA
    }

    public WorkflowState(UUID workflowId, String name, WorkflowStateCategory category, int sortOrder, boolean initial) {
        this.workflowId = workflowId;
        this.name = name;
        this.category = category;
        this.sortOrder = sortOrder;
        this.initial = initial;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public String getName() {
        return name;
    }

    public WorkflowStateCategory getCategory() {
        return category;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isInitial() {
        return initial;
    }

    public void update(String name, WorkflowStateCategory category, int sortOrder) {
        this.name = name;
        this.category = category;
        this.sortOrder = sortOrder;
    }
}
