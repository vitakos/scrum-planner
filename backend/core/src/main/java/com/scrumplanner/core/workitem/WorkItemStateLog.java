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
 * Append-only log of state transitions applied to a work item — the source
 * for cycle time / lead time / cumulative flow metrics later (see
 * docs/backlog-data-model.md). The changed_by column exists in the DB for
 * when there's more than one user; left unmapped for now.
 */
@Entity
@Table(name = "work_item_state_log")
public class WorkItemStateLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "work_item_id", nullable = false)
    private UUID workItemId;

    @Column(name = "from_state_id")
    private UUID fromStateId;

    @Column(name = "to_state_id", nullable = false)
    private UUID toStateId;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    protected WorkItemStateLog() {
        // JPA
    }

    public WorkItemStateLog(UUID workItemId, UUID fromStateId, UUID toStateId) {
        this.workItemId = workItemId;
        this.fromStateId = fromStateId;
        this.toStateId = toStateId;
        this.changedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkItemId() {
        return workItemId;
    }

    public UUID getFromStateId() {
        return fromStateId;
    }

    public UUID getToStateId() {
        return toStateId;
    }

    public OffsetDateTime getChangedAt() {
        return changedAt;
    }
}
