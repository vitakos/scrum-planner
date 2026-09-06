package com.scrumplanner.core.customfield;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CustomFieldDefinitionRepository extends JpaRepository<CustomFieldDefinition, UUID> {
    List<CustomFieldDefinition> findAllByProjectIdAndWorkItemTypeOrderByNameAsc(UUID projectId, String workItemType);
}
