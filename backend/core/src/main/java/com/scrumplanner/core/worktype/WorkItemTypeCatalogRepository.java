package com.scrumplanner.core.worktype;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkItemTypeCatalogRepository extends JpaRepository<WorkItemTypeCatalog, String> {
    List<WorkItemTypeCatalog> findAllByOrderBySortOrderAsc();
}
