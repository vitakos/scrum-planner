package com.scrumplanner.core.workflow;

import com.scrumplanner.core.worktype.WorkItemTypeCatalog;
import com.scrumplanner.core.worktype.WorkItemTypeCatalogRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Creates the predefined workflow (To Do -> In Progress -> Done, with the
 * obvious back-and-forth transitions) for every system work item type when a
 * new project is set up. The user can then rename/add/remove states and
 * transitions per type from the project configuration screen.
 */
@Service
public class DefaultWorkflowSeeder {

    private final WorkItemTypeCatalogRepository typeCatalogRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;

    public DefaultWorkflowSeeder(
            WorkItemTypeCatalogRepository typeCatalogRepository,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowStateRepository workflowStateRepository,
            WorkflowTransitionRepository workflowTransitionRepository
    ) {
        this.typeCatalogRepository = typeCatalogRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStateRepository = workflowStateRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
    }

    public void seedDefaultWorkflows(UUID projectId) {
        for (WorkItemTypeCatalog type : typeCatalogRepository.findAllByOrderBySortOrderAsc()) {
            if (!type.isSystem()) {
                continue;
            }
            seedWorkflowFor(projectId, type.getCode(), type.getName());
        }
    }

    private void seedWorkflowFor(UUID projectId, String workItemTypeCode, String workItemTypeName) {
        WorkflowDefinition workflow = workflowDefinitionRepository.save(
                new WorkflowDefinition(projectId, workItemTypeCode, "Default " + workItemTypeName + " workflow")
        );

        WorkflowState toDo = workflowStateRepository.save(
                new WorkflowState(workflow.getId(), "To Do", WorkflowStateCategory.to_do, 0, true)
        );
        WorkflowState inProgress = workflowStateRepository.save(
                new WorkflowState(workflow.getId(), "In Progress", WorkflowStateCategory.in_progress, 1, false)
        );
        WorkflowState done = workflowStateRepository.save(
                new WorkflowState(workflow.getId(), "Done", WorkflowStateCategory.done, 2, false)
        );

        workflowTransitionRepository.save(
                new WorkflowTransition(workflow.getId(), toDo.getId(), inProgress.getId(), "Start"));
        workflowTransitionRepository.save(
                new WorkflowTransition(workflow.getId(), inProgress.getId(), done.getId(), "Complete"));
        workflowTransitionRepository.save(
                new WorkflowTransition(workflow.getId(), inProgress.getId(), toDo.getId(), "Back to To Do"));
        workflowTransitionRepository.save(
                new WorkflowTransition(workflow.getId(), done.getId(), inProgress.getId(), "Reopen"));
    }
}
