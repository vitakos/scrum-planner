package com.scrumplanner.core.workitem;

import com.scrumplanner.core.common.ConflictException;
import com.scrumplanner.core.common.NotFoundException;
import com.scrumplanner.core.content.WorkItemContentService;
import com.scrumplanner.core.content.WorkItemContentService.WorkItemContent;
import com.scrumplanner.core.customfield.CustomFieldDefinition;
import com.scrumplanner.core.customfield.CustomFieldDefinitionRepository;
import com.scrumplanner.core.project.Project;
import com.scrumplanner.core.project.ProjectRepository;
import com.scrumplanner.core.workflow.WorkflowDefinition;
import com.scrumplanner.core.workflow.WorkflowDefinitionRepository;
import com.scrumplanner.core.workflow.WorkflowState;
import com.scrumplanner.core.workflow.WorkflowStateRepository;
import com.scrumplanner.core.workflow.WorkflowTransition;
import com.scrumplanner.core.workflow.WorkflowTransitionRepository;
import com.scrumplanner.core.workitem.dto.ApplyTransitionRequest;
import com.scrumplanner.core.workitem.dto.AvailableTransitionResponse;
import com.scrumplanner.core.workitem.dto.CreateWorkItemRequest;
import com.scrumplanner.core.workitem.dto.UpdateWorkItemRequest;
import com.scrumplanner.core.workitem.dto.WorkItemResponse;
import com.scrumplanner.core.worktype.WorkItemTypeCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final WorkItemStateLogRepository workItemStateLogRepository;
    private final ProjectRepository projectRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowStateRepository workflowStateRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkItemTypeCatalogRepository typeCatalogRepository;
    private final WorkItemContentService workItemContentService;
    private final CustomFieldDefinitionRepository customFieldDefinitionRepository;

    public WorkItemService(
            WorkItemRepository workItemRepository,
            WorkItemStateLogRepository workItemStateLogRepository,
            ProjectRepository projectRepository,
            WorkflowDefinitionRepository workflowDefinitionRepository,
            WorkflowStateRepository workflowStateRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            WorkItemTypeCatalogRepository typeCatalogRepository,
            WorkItemContentService workItemContentService,
            CustomFieldDefinitionRepository customFieldDefinitionRepository
    ) {
        this.workItemRepository = workItemRepository;
        this.workItemStateLogRepository = workItemStateLogRepository;
        this.projectRepository = projectRepository;
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowStateRepository = workflowStateRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.typeCatalogRepository = typeCatalogRepository;
        this.workItemContentService = workItemContentService;
        this.customFieldDefinitionRepository = customFieldDefinitionRepository;
    }

    @Transactional
    public WorkItemResponse createWorkItem(UUID projectId, CreateWorkItemRequest request) {
        Project project = requireProject(projectId);
        TypeWorkflowContext context = loadContext(projectId, request.type());

        WorkflowState initialState = context.statesById().values().stream()
                .filter(WorkflowState::isInitial)
                .findFirst()
                .orElseThrow(() -> new ConflictException(
                        "Work item type '" + request.type() + "' has no initial state configured"));

        UUID parentId = request.parentId();
        if (parentId != null) {
            requireValidParent(projectId, parentId, null);
        }

        int seq = project.nextWorkItemSeq();
        projectRepository.save(project);

        WorkItem item = workItemRepository.save(
                new WorkItem(projectId, request.type(), request.title().trim(), initialState.getId(), seq, parentId)
        );

        String content = request.content();
        Map<String, Object> customFields = validateCustomFields(projectId, request.type(), request.customFields());
        if (content != null || customFields != null) {
            // item.getId() is already populated: WorkItem uses a client-generated
            // (GenerationType.UUID) id, and item stays managed within this
            // transaction, so setting contentRef here is enough — no extra save.
            item.setContentRef(workItemContentService.upsert(null, item.getId(), content, customFields));
        }

        return toResponse(item, project.getKey(), context, content, customFields != null ? customFields : Map.of());
    }

    @Transactional(readOnly = true)
    public List<WorkItemResponse> listWorkItems(UUID projectId, String type) {
        Project project = requireProject(projectId);
        List<WorkItem> items = type != null
                ? workItemRepository.findAllByProjectIdAndTypeOrderBySeqAsc(projectId, type)
                : workItemRepository.findAllByProjectIdOrderBySeqAsc(projectId);

        List<String> contentRefs = items.stream()
                .map(WorkItem::getContentRef)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, WorkItemContent> contentByRef = workItemContentService.findByRefs(contentRefs);

        Map<String, TypeWorkflowContext> contextsByType = new HashMap<>();
        return items.stream()
                .map(item -> {
                    TypeWorkflowContext context = contextsByType.computeIfAbsent(
                            item.getType(), t -> loadContext(projectId, t));
                    WorkItemContent content = item.getContentRef() != null
                            ? contentByRef.getOrDefault(item.getContentRef(), WorkItemContent.EMPTY)
                            : WorkItemContent.EMPTY;
                    return toResponse(item, project.getKey(), context, content.description(), content.customFields());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkItemResponse getWorkItem(UUID projectId, UUID workItemId) {
        Project project = requireProject(projectId);
        WorkItem item = requireWorkItem(projectId, workItemId);
        WorkItemContent content = workItemContentService.find(item.getContentRef()).orElse(WorkItemContent.EMPTY);
        return toResponse(item, project.getKey(), loadContext(projectId, item.getType()), content.description(), content.customFields());
    }

    /**
     * Resolves a human-readable key such as "SPAI-1" (project key + '-' +
     * per-project sequence, see docs/backlog-data-model.md and
     * Project#nextWorkItemSeq) to its work item, without needing the
     * project id up front. Backs the frontend route/link scheme that
     * addresses a work item by key alone (e.g. "/SPAI-1"), including links
     * inserted from the rich text editor.
     */
    @Transactional(readOnly = true)
    public WorkItemResponse getWorkItemByKey(String key) {
        int separatorIndex = key.lastIndexOf('-');
        if (separatorIndex <= 0 || separatorIndex == key.length() - 1) {
            throw new NotFoundException("Not a valid work item key: " + key);
        }
        String projectKey = key.substring(0, separatorIndex);
        int seq;
        try {
            seq = Integer.parseInt(key.substring(separatorIndex + 1));
        } catch (NumberFormatException e) {
            throw new NotFoundException("Not a valid work item key: " + key);
        }

        Project project = projectRepository.findByKey(projectKey)
                .orElseThrow(() -> new NotFoundException("No project with key '" + projectKey + "' for work item key: " + key));
        WorkItem item = workItemRepository.findByProjectIdAndSeq(project.getId(), seq)
                .orElseThrow(() -> new NotFoundException("Work item not found: " + key));
        WorkItemContent content = workItemContentService.find(item.getContentRef()).orElse(WorkItemContent.EMPTY);
        return toResponse(item, project.getKey(), loadContext(project.getId(), item.getType()), content.description(), content.customFields());
    }

    @Transactional
    public WorkItemResponse updateWorkItem(UUID projectId, UUID workItemId, UpdateWorkItemRequest request) {
        Project project = requireProject(projectId);
        WorkItem item = requireWorkItem(projectId, workItemId);

        item.rename(request.title().trim());

        UUID parentId = request.parentId();
        if (parentId != null) {
            requireValidParent(projectId, parentId, workItemId);
        }
        item.reparent(parentId);

        String content = request.content();
        Map<String, Object> customFields = validateCustomFields(projectId, item.getType(), request.customFields());

        WorkItemContent existing = workItemContentService.find(item.getContentRef()).orElse(WorkItemContent.EMPTY);
        if (content != null || customFields != null) {
            item.setContentRef(workItemContentService.upsert(item.getContentRef(), item.getId(), content, customFields));
        }

        String responseContent = content != null ? content : existing.description();
        Map<String, Object> responseCustomFields = customFields != null ? customFields : existing.customFields();

        return toResponse(item, project.getKey(), loadContext(projectId, item.getType()), responseContent, responseCustomFields);
    }

    @Transactional
    public WorkItemResponse applyTransition(UUID projectId, UUID workItemId, ApplyTransitionRequest request) {
        Project project = requireProject(projectId);
        WorkItem item = requireWorkItem(projectId, workItemId);
        TypeWorkflowContext context = loadContext(projectId, item.getType());

        WorkflowTransition transition = context.transitions().stream()
                .filter(t -> t.getId().equals(request.transitionId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Transition not found for this work item's workflow: " + request.transitionId()));

        if (!transition.getFromStateId().equals(item.getStateId())) {
            throw new ConflictException("This transition doesn't apply to the item's current state");
        }

        workItemStateLogRepository.save(new WorkItemStateLog(item.getId(), item.getStateId(), transition.getToStateId()));
        item.moveToState(transition.getToStateId());

        WorkItemContent content = workItemContentService.find(item.getContentRef()).orElse(WorkItemContent.EMPTY);
        return toResponse(item, project.getKey(), context, content.description(), content.customFields());
    }

    @Transactional
    public void deleteWorkItem(UUID projectId, UUID workItemId) {
        WorkItem item = requireWorkItem(projectId, workItemId);
        workItemContentService.delete(item.getContentRef());
        workItemRepository.delete(item);
    }

    /**
     * A parent must exist, belong to the same project, and (for an update on
     * an existing item) not be the item itself or a descendant of it — since
     * either would create a cycle in the hierarchy.
     */
    private void requireValidParent(UUID projectId, UUID parentId, UUID workItemId) {
        if (workItemId != null && parentId.equals(workItemId)) {
            throw new ConflictException("A work item can't be its own parent");
        }

        WorkItem parent = workItemRepository.findById(parentId)
                .orElseThrow(() -> new NotFoundException("Parent work item not found: " + parentId));

        if (!parent.getProjectId().equals(projectId)) {
            throw new ConflictException("Parent work item must belong to the same project");
        }

        if (workItemId == null) {
            return;
        }

        UUID ancestorId = parent.getParentId();
        while (ancestorId != null) {
            if (ancestorId.equals(workItemId)) {
                throw new ConflictException("Setting this parent would create a cycle in the work item hierarchy");
            }
            ancestorId = workItemRepository.findById(ancestorId)
                    .map(WorkItem::getParentId)
                    .orElse(null);
        }
    }

    /**
     * Custom field values are free-form on the Mongo side, but every key
     * must name a field actually configured for this project/work item type
     * — otherwise a typo or a stale (deleted) field name would silently pile
     * up in the content document forever. Returns {@code null} unchanged
     * (meaning "don't touch stored custom fields") when the request didn't
     * send any.
     */
    private Map<String, Object> validateCustomFields(UUID projectId, String workItemType, Map<String, Object> customFields) {
        if (customFields == null) {
            return null;
        }
        Set<String> validNames = customFieldDefinitionRepository
                .findAllByProjectIdAndWorkItemTypeOrderByNameAsc(projectId, workItemType).stream()
                .map(CustomFieldDefinition::getName)
                .collect(Collectors.toSet());
        for (String name : customFields.keySet()) {
            if (!validNames.contains(name)) {
                throw new IllegalArgumentException(
                        "Unknown custom field '" + name + "' for work item type '" + workItemType + "'");
            }
        }
        return customFields;
    }

    private WorkItemResponse toResponse(
            WorkItem item, String projectKey, TypeWorkflowContext context, String content, Map<String, Object> customFields
    ) {
        WorkflowState state = context.statesById().get(item.getStateId());
        List<AvailableTransitionResponse> available = context.transitions().stream()
                .filter(t -> t.getFromStateId().equals(item.getStateId()))
                .map(t -> {
                    WorkflowState toState = context.statesById().get(t.getToStateId());
                    return new AvailableTransitionResponse(
                            t.getId(), t.getName(), t.getToStateId(), toState != null ? toState.getName() : "?");
                })
                .toList();

        String parentKey = null;
        String parentTitle = null;
        if (item.getParentId() != null) {
            WorkItem parent = workItemRepository.findById(item.getParentId()).orElse(null);
            if (parent != null) {
                parentKey = projectKey + "-" + parent.getSeq();
                parentTitle = parent.getTitle();
            }
        }
        int childCount = workItemRepository.countByParentId(item.getId());

        return new WorkItemResponse(
                item.getId(),
                projectKey + "-" + item.getSeq(),
                item.getProjectId(),
                item.getType(),
                context.typeName(),
                item.getTitle(),
                content,
                customFields,
                item.getStateId(),
                state != null ? state.getName() : "?",
                state != null ? state.getCategory().name() : "?",
                item.getParentId(),
                parentKey,
                parentTitle,
                childCount,
                available,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private TypeWorkflowContext loadContext(UUID projectId, String type) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findByProjectIdAndWorkItemType(projectId, type)
                .orElseThrow(() -> new NotFoundException(
                        "No workflow for work item type '" + type + "' in project " + projectId));
        Map<UUID, WorkflowState> statesById = workflowStateRepository
                .findAllByWorkflowIdOrderBySortOrderAsc(workflow.getId()).stream()
                .collect(Collectors.toMap(WorkflowState::getId, s -> s));
        List<WorkflowTransition> transitions = workflowTransitionRepository.findAllByWorkflowId(workflow.getId());
        String typeName = typeCatalogRepository.findById(type)
                .map(t -> t.getName())
                .orElse(type);
        return new TypeWorkflowContext(workflow, statesById, transitions, typeName);
    }

    private Project requireProject(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));
    }

    private WorkItem requireWorkItem(UUID projectId, UUID workItemId) {
        return workItemRepository.findById(workItemId)
                .filter(item -> item.getProjectId().equals(projectId))
                .orElseThrow(() -> new NotFoundException("Work item not found: " + workItemId));
    }

    private record TypeWorkflowContext(
            WorkflowDefinition workflow,
            Map<UUID, WorkflowState> statesById,
            List<WorkflowTransition> transitions,
            String typeName
    ) {
    }
}
