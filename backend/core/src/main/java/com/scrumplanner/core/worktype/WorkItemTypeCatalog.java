package com.scrumplanner.core.worktype;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The catalog of work item types available to be used in a project's
 * workflows (Epic, Feature, User Story, Task, Bug, Test Case, Test Run by
 * default). Seeded by database/sql/migrations/0002_flexible_work_item_types.sql.
 * Read-only from the API for now — no UI to add custom types yet.
 */
@Entity
@Table(name = "work_item_type_catalog")
public class WorkItemTypeCatalog {

    @Id
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected WorkItemTypeCatalog() {
        // JPA
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isSystem() {
        return system;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
