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

    /**
     * Optional custom color (hex, e.g. "#a1b2c3"). Null means "use the
     * default color for this state's category" — that default is resolved
     * at the presentation layer (frontend), not stored here.
     */
    @Column
    private String color;

    @Column(columnDefinition = "TEXT")
    private String description;

    protected WorkflowState() {
        // JPA
    }

    public WorkflowState(UUID workflowId, String name, WorkflowStateCategory category, int sortOrder, boolean initial) {
        this(workflowId, name, category, sortOrder, initial, null);
    }

    public WorkflowState(
            UUID workflowId, String name, WorkflowStateCategory category, int sortOrder, boolean initial, String color
    ) {
        this(workflowId, name, category, sortOrder, initial, color, null);
    }

    public WorkflowState(
            UUID workflowId, String name, WorkflowStateCategory category, int sortOrder, boolean initial,
            String color, String description
    ) {
        this.workflowId = workflowId;
        this.name = name;
        this.category = category;
        this.sortOrder = sortOrder;
        this.initial = initial;
        this.color = color;
        this.description = description;
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

    public String getColor() {
        return color;
    }

    public String getDescription() {
        return description;
    }

    public void update(String name, WorkflowStateCategory category, int sortOrder, String color, String description) {
        this.name = name;
        this.category = category;
        this.sortOrder = sortOrder;
        this.color = color;
        this.description = description;
    }

    public void reorder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
