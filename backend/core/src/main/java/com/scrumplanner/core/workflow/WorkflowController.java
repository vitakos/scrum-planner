package com.scrumplanner.core.workflow;

import com.scrumplanner.core.workflow.dto.CreateStateRequest;
import com.scrumplanner.core.workflow.dto.CreateTransitionRequest;
import com.scrumplanner.core.workflow.dto.UpdateStateRequest;
import com.scrumplanner.core.workflow.dto.UpdateTransitionRequest;
import com.scrumplanner.core.workflow.dto.WorkflowResponse;
import com.scrumplanner.core.workflow.dto.WorkflowStateResponse;
import com.scrumplanner.core.workflow.dto.WorkflowTransitionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping
    public List<WorkflowResponse> list(@PathVariable UUID projectId) {
        return workflowService.listWorkflows(projectId);
    }

    @PostMapping("/{workItemType}/states")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowStateResponse addState(
            @PathVariable UUID projectId,
            @PathVariable String workItemType,
            @Valid @RequestBody CreateStateRequest request
    ) {
        return workflowService.addState(projectId, workItemType, request);
    }

    @PutMapping("/{workItemType}/states/{stateId}")
    public WorkflowStateResponse updateState(
            @PathVariable UUID projectId,
            @PathVariable String workItemType,
            @PathVariable UUID stateId,
            @Valid @RequestBody UpdateStateRequest request
    ) {
        return workflowService.updateState(projectId, workItemType, stateId, request);
    }

    @DeleteMapping("/{workItemType}/states/{stateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteState(
            @PathVariable UUID projectId,
            @PathVariable String workItemType,
            @PathVariable UUID stateId
    ) {
        workflowService.deleteState(projectId, workItemType, stateId);
    }

    @PostMapping("/{workItemType}/transitions")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowTransitionResponse addTransition(
            @PathVariable UUID projectId,
            @PathVariable String workItemType,
            @Valid @RequestBody CreateTransitionRequest request
    ) {
        return workflowService.addTransition(projectId, workItemType, request);
    }

    @PutMapping("/{workItemType}/transitions/{transitionId}")
    public WorkflowTransitionResponse updateTransition(
            @PathVariable UUID projectId,
            @PathVariable String workItemType,
            @PathVariable UUID transitionId,
            @Valid @RequestBody UpdateTransitionRequest request
    ) {
        return workflowService.updateTransition(projectId, workItemType, transitionId, request);
    }

    @DeleteMapping("/{workItemType}/transitions/{transitionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransition(
            @PathVariable UUID projectId,
            @PathVariable String workItemType,
            @PathVariable UUID transitionId
    ) {
        workflowService.deleteTransition(projectId, workItemType, transitionId);
    }
}
