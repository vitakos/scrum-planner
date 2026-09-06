package com.scrumplanner.core.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, UUID> {
    List<WorkflowTransition> findAllByWorkflowId(UUID workflowId);

    boolean existsByWorkflowIdAndFromStateIdAndToStateId(UUID workflowId, UUID fromStateId, UUID toStateId);

    boolean existsByFromStateIdOrToStateId(UUID fromStateId, UUID toStateId);
}
