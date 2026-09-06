package com.scrumplanner.core.workflow;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Note: the underlying workflow_transition table also has an allowed_roles
 * text[] column (defaulting to '{}') for future role-based transition guards.
 * It's intentionally left unmapped here — there's no auth/roles yet — and
 * Hibernate's schema validation only checks the columns an entity declares,
 * so an unmapped column with a DB-side default is safe to leave alone.
 */
@Entity
@Table(name = "workflow_transition")
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "from_state_id", nullable = false)
    private UUID fromStateId;

    @Column(name = "to_state_id", nullable = false)
    private UUID toStateId;

    @Column(nullable = false)
    private String name;

    protected WorkflowTransition() {
        // JPA
    }

    public WorkflowTransition(UUID workflowId, UUID fromStateId, UUID toStateId, String name) {
        this.workflowId = workflowId;
        this.fromStateId = fromStateId;
        this.toStateId = toStateId;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkflowId() {
        return workflowId;
    }

    public UUID getFromStateId() {
        return fromStateId;
    }

    public UUID getToStateId() {
        return toStateId;
    }

    public String getName() {
        return name;
    }

    public void rename(String name) {
        this.name = name;
    }
}
