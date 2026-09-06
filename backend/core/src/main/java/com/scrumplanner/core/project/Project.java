package com.scrumplanner.core.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String key;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "work_item_seq", nullable = false)
    private int workItemSeq = 0;

    protected Project() {
        // JPA
    }

    public Project(String key, String name, String description) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void rename(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Simple read-modify-write counter for the human-readable work item key
     * (e.g. "SPAI-1"). Not concurrency-safe under truly simultaneous
     * requests, which is an acceptable tradeoff for a single local user;
     * revisit (e.g. a DB-side atomic increment) before multi-user support.
     */
    public int nextWorkItemSeq() {
        workItemSeq += 1;
        return workItemSeq;
    }
}
