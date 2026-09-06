package com.scrumplanner.core.workflow;

import com.scrumplanner.core.common.ConflictException;
import com.scrumplanner.core.common.NotFoundException;
import com.scrumplanner.core.worktype.WorkItemTypeCatalog;
import com.scrumplanner.core.worktype.WorkItemTypeCatalogRepository;
import com.scrumplanner.core.workflow.dto.CreateStateRequest;
import com.scrumplanner.core.workflow.dto.CreateTransitionRequest;
import com.scrumplanner.core.workflow.dto.UpdateStateRequest;
import com.scrumplanner.core.workflow.dto.UpdateTransitionRequest;
import com.scrumplanner.core.workflow.dto.WorkflowResponse;
import com.scrumplanner.core.workflow.dto.WorkflowStateResponse;
import com.scrumplanner.core.workflow.dto.WorkflowTransitionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkflowService {

    private final WorkItemTypeCatalogRepository typeCatalogRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;

    public WorkflowService(
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

    @Transactional(readOnly = true)
    public List<WorkflowResponse> listWorkflows(UUID projectId) {
        Map<String, String> typeNames = typeCatalogRepository.findAll().stream()
                .collect(Collectors.toMap(WorkItemTypeCatalog::getCode, WorkItemTypeCatalog::getName));

        return workflowDefinitionRepository.findAllByProjectId(projectId).stream()
                .map(workflow -> toResponse(workflow, typeNames.getOrDefault(workflow.getWorkItemType(), workflow.getWorkItemType())))
                .toList();
    }

    @Transactional
    public WorkflowStateResponse addState(UUID projectId, String workItemType, CreateStateRequest request) {
        WorkflowDefinition workflow = requireWorkflow(projectId, workItemType);
        WorkflowStateCategory category = parseCategory(request.category());

        if (workflowStateRepository.existsByWorkflowIdAndNameIgnoreCase(workflow.getId(), request.name().trim())) {
            throw new ConflictException("A state named '" + request.name() + "' already exists in this workflow");
        }

        int nextSortOrder = workflowStateRepository.countByWorkflowId(workflow.getId());
        WorkflowState state = workflowStateRepository.save(
                new WorkflowState(workflow.getId(), request.name().trim(), category, nextSortOrder, false)
        );
        return WorkflowStateResponse.from(state);
    }

    @Transactional
    public WorkflowStateResponse updateState(UUID projectId, String workItemType, UUID stateId, UpdateStateRequest request) {
        WorkflowDefinition workflow = requireWorkflow(projectId, workItemType);
        WorkflowState state = requireStateInWorkflow(workflow.getId(), stateId);
        WorkflowStateCategory category = parseCategory(request.category());

        boolean nameTaken = workflowStateRepository.findAllByWorkflowIdOrderBySortOrderAsc(workflow.getId()).stream()
                .anyMatch(other -> !other.getId().equals(stateId) && other.getName().equalsIgnoreCase(request.name().trim()));
        if (nameTaken) {
            throw new ConflictException("A state named '" + request.name() + "' already exists in this workflow");
        }

        state.update(request.name().trim(), category, state.getSortOrder());
        return WorkflowStateResponse.from(state);
    }

    @Transactional
    public void deleteState(UUID projectId, String workItemType, UUID stateId) {
        WorkflowDefinition workflow = requireWorkflow(projectId, workItemType);
        WorkflowState state = requireStateInWorkflow(workflow.getId(), stateId);

        if (state.isInitial()) {
            throw new ConflictException("Cannot delete the initial state of a workflow");
        }
        if (workflowTransitionRepository.existsByFromStateIdOrToStateId(stateId, stateId)) {
            throw new ConflictException("Cannot delete a state that is used by a transition — remove the transition(s) first");
        }
        workflowStateRepository.delete(state);
    }

    @Transactional
    public WorkflowTransitionResponse addTransition(UUID projectId, String workItemType, CreateTransitionRequest request) {
        WorkflowDefinition workflow = requireWorkflow(projectId, workItemType);
        requireStateInWorkflow(workflow.getId(), request.fromStateId());
        requireStateInWorkflow(workflow.getId(), request.toStateId());

        if (request.fromStateId().equals(request.toStateId())) {
            throw new IllegalArgumentException("A transition cannot go from a state to itself");
        }
        if (workflowTransitionRepository.existsByWorkflowIdAndFromStateIdAndToStateId(
                workflow.getId(), request.fromStateId(), request.toStateId())) {
            throw new ConflictException("This transition already exists");
        }
        requireTransitionNameAvailable(workflow.getId(), request.name().trim(), null);

        WorkflowTransition transition = workflowTransitionRepository.save(
                new WorkflowTransition(workflow.getId(), request.fromStateId(), request.toStateId(), request.name().trim())
        );
        return WorkflowTransitionResponse.from(transition);
    }

    @Transactional
    public WorkflowTransitionResponse updateTransition(
            UUID projectId, String workItemType, UUID transitionId, UpdateTransitionRequest request
    ) {
        WorkflowDefinition workflow = requireWorkflow(projectId, workItemType);
        WorkflowTransition transition = requireTransitionInWorkflow(workflow.getId(), transitionId);
        requireTransitionNameAvailable(workflow.getId(), request.name().trim(), transitionId);

        transition.rename(request.name().trim());
        return WorkflowTransitionResponse.from(transition);
    }

    @Transactional
    public void deleteTransition(UUID projectId, String workItemType, UUID transitionId) {
        WorkflowDefinition workflow = requireWorkflow(projectId, workItemType);
        WorkflowTransition transition = requireTransitionInWorkflow(workflow.getId(), transitionId);
        workflowTransitionRepository.delete(transition);
    }

    private WorkflowTransition requireTransitionInWorkflow(UUID workflowId, UUID transitionId) {
        return workflowTransitionRepository.findById(transitionId)
                .filter(t -> t.getWorkflowId().equals(workflowId))
                .orElseThrow(() -> new NotFoundException("Transition not found in this workflow: " + transitionId));
    }

    private void requireTransitionNameAvailable(UUID workflowId, String name, UUID excludingTransitionId) {
        boolean nameTaken = workflowTransitionRepository.findAllByWorkflowId(workflowId).stream()
                .anyMatch(other -> !other.getId().equals(excludingTransitionId) && other.getName().equalsIgnoreCase(name));
        if (nameTaken) {
            throw new ConflictException("A transition named '" + name + "' already exists in this workflow");
        }
    }

    private WorkflowResponse toResponse(WorkflowDefinition workflow, String typeName) {
        List<WorkflowStateResponse> states = workflowStateRepository
                .findAllByWorkflowIdOrderBySortOrderAsc(workflow.getId()).stream()
                .map(WorkflowStateResponse::from)
                .toList();
        List<WorkflowTransitionResponse> transitions = workflowTransitionRepository
                .findAllByWorkflowId(workflow.getId()).stream()
                .map(WorkflowTransitionResponse::from)
                .sorted(Comparator.comparing(WorkflowTransitionResponse::fromStateId))
                .toList();
        return new WorkflowResponse(workflow.getId(), workflow.getWorkItemType(), typeName, workflow.getName(), states, transitions);
    }

    private WorkflowDefinition requireWorkflow(UUID projectId, String workItemType) {
        return workflowDefinitionRepository.findByProjectIdAndWorkItemType(projectId, workItemType)
                .orElseThrow(() -> new NotFoundException(
                        "No workflow for work item type '" + workItemType + "' in project " + projectId));
    }

    private WorkflowState requireStateInWorkflow(UUID workflowId, UUID stateId) {
        return workflowStateRepository.findById(stateId)
                .filter(state -> state.getWorkflowId().equals(workflowId))
                .orElseThrow(() -> new NotFoundException("State not found in this workflow: " + stateId));
    }

    private WorkflowStateCategory parseCategory(String category) {
        try {
            return WorkflowStateCategory.valueOf(category);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("category must be one of to_do, in_progress, done");
        }
    }
}
