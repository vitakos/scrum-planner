package com.scrumplanner.core.workitem;

import com.scrumplanner.core.content.WorkItemContentService;
import com.scrumplanner.core.customfield.CustomFieldDefinitionRepository;
import com.scrumplanner.core.project.Project;
import com.scrumplanner.core.project.ProjectRepository;
import com.scrumplanner.core.workflow.WorkflowDefinition;
import com.scrumplanner.core.workflow.WorkflowDefinitionRepository;
import com.scrumplanner.core.workflow.WorkflowState;
import com.scrumplanner.core.workflow.WorkflowStateCategory;
import com.scrumplanner.core.workflow.WorkflowStateRepository;
import com.scrumplanner.core.workflow.WorkflowTransitionRepository;
import com.scrumplanner.core.workitem.dto.WorkItemResponse;
import com.scrumplanner.core.worktype.WorkItemTypeCatalogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Covers AISC-83/AISC-76: the excludeDoneCategory filter on
 * {@link WorkItemService#listWorkItems}. Each work item type has its own
 * workflow (and therefore its own state ids and "done" state), so the
 * filter must resolve "is this item's state in the done category" per the
 * item's own type context rather than by comparing against a single
 * hardcoded state id.
 */
@ExtendWith(MockitoExtension.class)
class WorkItemServiceTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private WorkItemRepository workItemRepository;
    @Mock
    private WorkItemStateLogRepository workItemStateLogRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private WorkflowDefinitionRepository workflowDefinitionRepository;
    @Mock
    private WorkflowStateRepository workflowStateRepository;
    @Mock
    private WorkflowTransitionRepository workflowTransitionRepository;
    @Mock
    private WorkItemTypeCatalogRepository typeCatalogRepository;
    @Mock
    private WorkItemContentService workItemContentService;
    @Mock
    private CustomFieldDefinitionRepository customFieldDefinitionRepository;

    private WorkItemService service;

    // "task" workflow: To Do (to_do) / Done (done)
    private WorkItem taskToDo;
    private WorkItem taskDone;

    // "bug" workflow: In Progress (in_progress) / Resolved (done) — distinct
    // state ids from the task workflow's Done state, on purpose.
    private WorkItem bugInProgress;
    private WorkItem bugResolved;

    @BeforeEach
    void setUp() {
        service = new WorkItemService(
                workItemRepository,
                workItemStateLogRepository,
                projectRepository,
                workflowDefinitionRepository,
                workflowStateRepository,
                workflowTransitionRepository,
                typeCatalogRepository,
                workItemContentService,
                customFieldDefinitionRepository);

        Project project = new Project("AISC", "AI Scrum", null);
        ReflectionTestUtils.setField(project, "id", PROJECT_ID);
        when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));

        WorkflowDefinition taskWorkflow = new WorkflowDefinition(PROJECT_ID, "task", "Task workflow");
        ReflectionTestUtils.setField(taskWorkflow, "id", UUID.randomUUID());
        WorkflowState taskToDoState = newState(taskWorkflow.getId(), "To Do", WorkflowStateCategory.to_do, 0, true);
        WorkflowState taskDoneState = newState(taskWorkflow.getId(), "Done", WorkflowStateCategory.done, 1, false);

        WorkflowDefinition bugWorkflow = new WorkflowDefinition(PROJECT_ID, "bug", "Bug workflow");
        ReflectionTestUtils.setField(bugWorkflow, "id", UUID.randomUUID());
        WorkflowState bugInProgressState = newState(bugWorkflow.getId(), "In Progress", WorkflowStateCategory.in_progress, 0, true);
        WorkflowState bugResolvedState = newState(bugWorkflow.getId(), "Resolved", WorkflowStateCategory.done, 1, false);

        when(workflowDefinitionRepository.findByProjectIdAndWorkItemType(PROJECT_ID, "task"))
                .thenReturn(Optional.of(taskWorkflow));
        when(workflowDefinitionRepository.findByProjectIdAndWorkItemType(PROJECT_ID, "bug"))
                .thenReturn(Optional.of(bugWorkflow));
        when(workflowStateRepository.findAllByWorkflowIdOrderBySortOrderAsc(taskWorkflow.getId()))
                .thenReturn(List.of(taskToDoState, taskDoneState));
        when(workflowStateRepository.findAllByWorkflowIdOrderBySortOrderAsc(bugWorkflow.getId()))
                .thenReturn(List.of(bugInProgressState, bugResolvedState));
        when(workflowTransitionRepository.findAllByWorkflowId(any())).thenReturn(List.of());
        when(typeCatalogRepository.findById(anyString())).thenReturn(Optional.empty());
        when(workItemContentService.findByRefs(any())).thenReturn(Map.of());
        when(workItemRepository.countByParentId(any())).thenReturn(0);

        taskToDo = newItem("task", "Task A", taskToDoState.getId(), 1);
        taskDone = newItem("task", "Task B", taskDoneState.getId(), 2);
        bugInProgress = newItem("bug", "Bug A", bugInProgressState.getId(), 3);
        bugResolved = newItem("bug", "Bug B", bugResolvedState.getId(), 4);
    }

    private static WorkflowState newState(UUID workflowId, String name, WorkflowStateCategory category, int sortOrder, boolean initial) {
        WorkflowState state = new WorkflowState(workflowId, name, category, sortOrder, initial);
        ReflectionTestUtils.setField(state, "id", UUID.randomUUID());
        return state;
    }

    private static WorkItem newItem(String type, String title, UUID stateId, int seq) {
        WorkItem item = new WorkItem(PROJECT_ID, type, title, stateId, seq);
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        return item;
    }

    @Test
    void omittingExcludeDoneCategoryPreservesTodaysUnfilteredBehavior() {
        when(workItemRepository.findAllByProjectIdOrderBySeqAsc(PROJECT_ID))
                .thenReturn(List.of(taskToDo, taskDone, bugInProgress, bugResolved));

        List<WorkItemResponse> result = service.listWorkItems(PROJECT_ID, null);

        assertThat(result).extracting(WorkItemResponse::key)
                .containsExactlyInAnyOrder(keyOf(taskToDo), keyOf(taskDone), keyOf(bugInProgress), keyOf(bugResolved));
    }

    @Test
    void excludeDoneCategoryFalseReturnsEverything() {
        when(workItemRepository.findAllByProjectIdOrderBySeqAsc(PROJECT_ID))
                .thenReturn(List.of(taskToDo, taskDone, bugInProgress, bugResolved));

        List<WorkItemResponse> result = service.listWorkItems(PROJECT_ID, null, false);

        assertThat(result).hasSize(4);
    }

    @Test
    void excludeDoneCategoryTrueOmitsDoneItemsAcrossTypes() {
        when(workItemRepository.findAllByProjectIdOrderBySeqAsc(PROJECT_ID))
                .thenReturn(List.of(taskToDo, taskDone, bugInProgress, bugResolved));

        List<WorkItemResponse> result = service.listWorkItems(PROJECT_ID, null, true);

        // taskDone and bugResolved are each "done" per their own type's workflow
        // (different state ids entirely) and must both be excluded, while the
        // to_do/in_progress items from either type are kept.
        assertThat(result).extracting(WorkItemResponse::key)
                .containsExactlyInAnyOrder(keyOf(taskToDo), keyOf(bugInProgress));
    }

    @Test
    void excludeDoneCategoryCombinesWithTypeFilter() {
        when(workItemRepository.findAllByProjectIdAndTypeOrderBySeqAsc(PROJECT_ID, "task"))
                .thenReturn(List.of(taskToDo, taskDone));

        List<WorkItemResponse> result = service.listWorkItems(PROJECT_ID, "task", true);

        assertThat(result).extracting(WorkItemResponse::key).containsExactly(keyOf(taskToDo));
    }

    private static String keyOf(WorkItem item) {
        return "AISC-" + item.getSeq();
    }
}
