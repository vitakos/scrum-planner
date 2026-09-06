package com.scrumplanner.core.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowStateRepository extends JpaRepository<WorkflowState, UUID> {
    List<WorkflowState> findAllByWorkflowIdOrderBySortOrderAsc(UUID workflowId);

    int countByWorkflowId(UUID workflowId);

    boolean existsByWorkflowIdAndNameIgnoreCase(UUID workflowId, String name);
}
