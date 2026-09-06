package com.scrumplanner.core.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {
    List<WorkflowDefinition> findAllByProjectId(UUID projectId);

    Optional<WorkflowDefinition> findByProjectIdAndWorkItemType(UUID projectId, String workItemType);
}
